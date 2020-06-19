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

/**
 * The {@link EventExecutor} is a special {@link EventExecutorGroup} which comes
 * with some handy methods to see if a {@link Thread} is executed in a event loop.
 * Besides this, it also extends the {@link EventExecutorGroup} to allow for a generic
 * way to access methods.
 *
 */
public interface EventExecutor extends EventExecutorGroup {

    /**
     * Returns a reference to itself.
     */
    @Override
    EventExecutor next();

    /**
     * Return the {@link EventExecutorGroup} which is the parent of this {@link EventExecutor},
     */
    EventExecutorGroup parent();

    /**
     * Calls {@link #inEventLoop(Thread)} with {@link Thread#currentThread()} as argument
     *
     * Netty线程模型的卓越性能取决于它对当前执行的Thread的身份确定，也就是说，
     * 确定他是否是分配给当前Channel以及它的EventLoop的那个线程（通过调用inEventLoop(Thread)）。
     *
     * 为了确保一个Channel的整个生命周期中的I/O事件会被一个EventLoop负责，Netty通过inEventLoop()方法来判断当前执行的线程的身份，
     * 确定它是否是分配给当前Channel以及它的EventLoop的那一个线程。如果当前（调用）线程正是EventLoop中的线程，那么所提交的任务将会被直接执行，
     * 否则，EventLoop将调度该任务以便稍后执行，并将它放入内部的任务队列（每个EventLoop都有它自己的任务队列，从SingleThreadEventLoop的源码就能发现很多用于调度内部任务队列的方法），
     * 在下次处理它的事件时，将会执行队列中的那些任务。这种设计可以让任何线程与Channel直接交互，而无需在ChannelHandler中进行额外的同步。
     *
     * 从性能上来考虑，千万不要将一个需要长时间来运行的任务放入到任务队列中，它会影响到该队列中的其他任务的执行。
     * 解决方案是使用一个专门的EventExecutor来执行它（ChannelPipeline提供了带有EventExecutorGroup参数的addXXX()方法，
     * 该方法可以将传入的ChannelHandler绑定到你传入的EventExecutor之中），这样它就会在另一条线程中执行，与其他任务隔离。
     *
     */
    boolean inEventLoop();

    /**
     * Return {@code true} if the given {@link Thread} is executed in the event loop,
     * {@code false} otherwise.
     */
    boolean inEventLoop(Thread thread);

    /**
     * Return a new {@link Promise}.
     */
    <V> Promise<V> newPromise();

    /**
     * Create a new {@link ProgressivePromise}.
     */
    <V> ProgressivePromise<V> newProgressivePromise();

    /**
     * Create a new {@link Future} which is marked as succeeded already. So {@link Future#isSuccess()}
     * will return {@code true}. All {@link FutureListener} added to it will be notified directly. Also
     * every call of blocking methods will just return without blocking.
     */
    <V> Future<V> newSucceededFuture(V result);

    /**
     * Create a new {@link Future} which is marked as failed already. So {@link Future#isSuccess()}
     * will return {@code false}. All {@link FutureListener} added to it will be notified directly. Also
     * every call of blocking methods will just return without blocking.
     */
    <V> Future<V> newFailedFuture(Throwable cause);
}
