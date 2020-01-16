/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.util.concurrent;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Abstract base class for {@link EventExecutorGroup} implementations that handles their tasks with multiple threads at
 * the same time.
 *
 * MultithreadEventExecutorGroup继承AbstractEventExecutorGroup的子类，而此类做了对线程的大多的实现，
 * 从名字可以看出他是多线程事件执行组，而netty是事件驱动的所以在很多定义里都有这个event事件做了标注
 * 可以看出他也是一个抽象类说明它内部有一些抽象方法需要子类实现去定制一些特制的功能。
 */
public abstract class MultithreadEventExecutorGroup extends AbstractEventExecutorGroup {

    // 执行器数组这个EventExecutor是group管理的执行器，在next方法可以看到他是返回的此执行器
    // 采用了final修饰并且采用了数组说明他的长度是固定的
    // 需要注意固定的修饰因为后面再使用的时候会有引用
    private final EventExecutor[] children;

    // 此set是对上方的执行器数组的一个副本，并且这个副本只读。
    private final Set<EventExecutor> readonlyChildren;

    // 中断执行器的数量，如果group被中断则会遍历调用children的中断方法，而每个children被中断都会进行一个计数
    // 而terminatedChildren则是对中断children的计数，为何使用后面再中断将会讲述
    private final AtomicInteger terminatedChildren = new AtomicInteger();

    // 中断执行的返回结果，因为需要关闭时一个执行组所以为了异步执行所以返回了一个应答然后根据用于调用去决定是等待获取结果
    // 还是去设置一个结果事件，之前在讲述future的时候详细介绍过，等下讲述的时候将会详细介绍他的使用
    private final Promise<?> terminationFuture = new DefaultPromise(GlobalEventExecutor.INSTANCE);

    // 执行的选择器，什么是选择器呢，因为是线程组那么在来任务的时候将会选择使用哪个执行器去执行这个任务
    // 而此选择器则用到了，之前我们看到的定义next方法其实他的实现就是使用了这个选择器去返回执行器
    // 具体使用讲到的地方会详细说明
    private final EventExecutorChooserFactory.EventExecutorChooser chooser;

    /**
     * Create a new instance.
     *
     * @param nThreads          the number of threads that will be used by this instance.
     * @param threadFactory     the ThreadFactory to use, or {@code null} if the default should be used.
     * @param args              arguments which will passed to each {@link #newChild(Executor, Object...)} call
     */
    // 线程池（线程执行组）的构造器
    // nThreads 之前说过children是限制长度的而此参数就是用来设置此线程池的线程数大小
    // threadFactory 线程的创建工厂，用于创建线程
    // args 在创建执行器的时候传入固定参数，使用时将会讲述
    protected MultithreadEventExecutorGroup(int nThreads, ThreadFactory threadFactory, Object... args) {
        // 这里有个小逻辑如果传入的线程工厂不是null则把工厂包装给一个executor。如果默认传null则会用默认的线程工厂
        this(nThreads, threadFactory == null ? null : new ThreadPerTaskExecutor(threadFactory), args);
    }

    /**
     * Create a new instance.
     *
     * @param nThreads          the number of threads that will be used by this instance.
     * @param executor          the Executor to use, or {@code null} if the default should be used.
     * @param args              arguments which will passed to each {@link #newChild(Executor, Object...)} call
     */
    // 除了传入线程工厂还有一个做法就是传入一个executor，上一个构造就是对此构造的封装
    protected MultithreadEventExecutorGroup(int nThreads, Executor executor, Object... args) {
        // 传入了默认的执行器的选择器
        this(nThreads, executor, DefaultEventExecutorChooserFactory.INSTANCE, args);
    }

    /**
     * Create a new instance.
     *
     * @param nThreads          the number of threads that will be used by this instance.
     * @param executor          the Executor to use, or {@code null} if the default should be used.
     * @param chooserFactory    the {@link EventExecutorChooserFactory} to use.
     * @param args              arguments which will passed to each {@link #newChild(Executor, Object...)} call
     *
     */
    // 最终操作的构造器，扩展了执行器的选择器
    protected MultithreadEventExecutorGroup(int nThreads, Executor executor,
                                            EventExecutorChooserFactory chooserFactory, Object... args) {
        if (nThreads <= 0) {
            throw new IllegalArgumentException(String.format("nThreads: %d (expected: > 0)", nThreads));
        }

        // 如果传入的执行器是空的则采用默认的线程工厂和默认的执行器
        if (executor == null) {
            executor = new ThreadPerTaskExecutor(newDefaultThreadFactory());
        }

        // 创建指定线程数的执行器数组
        children = new EventExecutor[nThreads];

        for (int i = 0; i < nThreads; i ++) {
            boolean success = false;
            try {
                // 使用了newChild方法创建执行器并且传入了executor和设置参数args
                children[i] = newChild(executor, args);
                success = true;
            } catch (Exception e) {
                // TODO: Think about if this is a good exception type
                throw new IllegalStateException("failed to create a child event loop", e);
            } finally {
                if (!success) {
                    for (int j = 0; j < i; j ++) {
                        children[j].shutdownGracefully();
                    }

                    // 虽然上面调用了中断方法但是他并不会立马终止，因为内部还有内容需要执行。
                    for (int j = 0; j < i; j ++) {
                        EventExecutor e = children[j];
                        try {
                            // 判断当前的执行器是否终止了如果没有则等待获取结果
                            while (!e.isTerminated()) {
                                e.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
                            }
                        } catch (InterruptedException interrupted) {
                            // Let the caller handle the interruption.
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
        }

        // 获取执行器的选择器
        chooser = chooserFactory.newChooser(children);

        // 创建一个future的监听器用于监听终止结果,给池中每一个线程都设置这个 listener，当监听到所有线程都 terminate 以后，这个线程池就算真正的 terminate 了。
        final FutureListener<Object> terminationListener = new FutureListener<Object>() {
            @Override
            public void operationComplete(Future<Object> future) throws Exception {
                // 当此执行组中的执行器被关闭的时候回调用此方法进入这里，这里进行终止数加一然后比较是否已经达到了执行器的总数
                // 如果没有则跳过，如果有则设置当前执行器的终止future为success为null
                if (terminatedChildren.incrementAndGet() == children.length) {
                    terminationFuture.setSuccess(null);
                }
            }
        };

        // 遍历创建好的执行器动态添加终止future的结果监听器，当监听器触发则会进入上方的内部类实现
        for (EventExecutor e: children) {
            e.terminationFuture().addListener(terminationListener);
        }

        // 创建一个children的镜像set
        Set<EventExecutor> childrenSet = new LinkedHashSet<EventExecutor>(children.length);

        // 拷贝这个set
        Collections.addAll(childrenSet, children);

        // 并且设置此set内的所有数据不允许修改然后返回设置给readonlyChildren
        readonlyChildren = Collections.unmodifiableSet(childrenSet);
    }

    // 获取默认的线程工厂并且传入当前类名
    protected ThreadFactory newDefaultThreadFactory() {
        return new DefaultThreadFactory(getClass());
    }

    @Override
    public EventExecutor next() {
        return chooser.next();
    }

    @Override
    public Iterator<EventExecutor> iterator() {
        return readonlyChildren.iterator();
    }

    /**
     * Return the number of {@link EventExecutor} this implementation uses. This number is the maps
     * 1:1 to the threads it use.
     */
    public final int executorCount() {
        return children.length;
    }

    /**
     * Create a new EventExecutor which will later then accessible via the {@link #next()}  method. This method will be
     * called for each thread that will serve this {@link MultithreadEventExecutorGroup}.
     *
     */
    // 声明了一个创建执行器的方法并且抽象的，因为每个执行器的实现都有特殊的操作所以此处抽象
    protected abstract EventExecutor newChild(Executor executor, Object... args) throws Exception;

    // 之前说过调用线程组的关闭其实就是遍历执行器集合的关闭方法因为之前加了监听器去处理返回结果所以此处返回的future用于监听是否执行结束了
    @Override
    public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
        for (EventExecutor l: children) {
            l.shutdownGracefully(quietPeriod, timeout, unit);
        }
        return terminationFuture();
    }

    @Override
    public Future<?> terminationFuture() {
        return terminationFuture;
    }

    @Override
    @Deprecated
    public void shutdown() {
        for (EventExecutor l: children) {
            l.shutdown();
        }
    }

    @Override
    public boolean isShuttingDown() {
        for (EventExecutor l: children) {
            if (!l.isShuttingDown()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isShutdown() {
        for (EventExecutor l: children) {
            if (!l.isShutdown()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isTerminated() {
        for (EventExecutor l: children) {
            if (!l.isTerminated()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        loop: for (EventExecutor l: children) {
            for (;;) {
                long timeLeft = deadline - System.nanoTime();
                if (timeLeft <= 0) {
                    break loop;
                }
                if (l.awaitTermination(timeLeft, TimeUnit.NANOSECONDS)) {
                    break;
                }
            }
        }
        return isTerminated();
    }
}
