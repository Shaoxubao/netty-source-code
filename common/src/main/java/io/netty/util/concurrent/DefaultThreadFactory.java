/*
 * Copyright 2013 The Netty Project
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

import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;

import java.util.Locale;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link ThreadFactory} implementation with a simple naming rule.
 */

// 默认的线程工厂
public class DefaultThreadFactory implements ThreadFactory {

    // 线程组的id，这里组代表是工厂，因为工厂是可以new的如果不分配到时候很难看出是哪里个工厂创建的线程
    // 而此处采用了static代表此属性是跟类走的而不是对象所以每次创建一个工厂pool都会增加一
    private static final AtomicInteger poolId = new AtomicInteger();

    // 创建线程的自增id
    private final AtomicInteger nextId = new AtomicInteger();
    private final String prefix;

    // 是否为守护线程
    private final boolean daemon;

    // 线程优先级
    private final int priority;

    // 创建线程所属的线程组，可以为null系统会使用默认的线程组
    protected final ThreadGroup threadGroup;

    // 下面是线程工厂的构造这里统一说明一下
    // poolType 是Class 类型他最终会被转换成类名用于poolName的使用
    // poolName 线程名但是不是完整的他会拼接一些其他数据比如poolId
    // daemon 是否为守护线程除非手动设置否则默认都是false
    // priority 线程的优先级 默认是NORM_PRIORITY 也是系统默认的
    public DefaultThreadFactory(Class<?> poolType) {
        this(poolType, false, Thread.NORM_PRIORITY);
    }

    public DefaultThreadFactory(String poolName) {
        this(poolName, false, Thread.NORM_PRIORITY);
    }

    public DefaultThreadFactory(Class<?> poolType, boolean daemon) {
        this(poolType, daemon, Thread.NORM_PRIORITY);
    }

    public DefaultThreadFactory(String poolName, boolean daemon) {
        this(poolName, daemon, Thread.NORM_PRIORITY);
    }

    public DefaultThreadFactory(Class<?> poolType, int priority) {
        this(poolType, false, priority);
    }

    public DefaultThreadFactory(String poolName, int priority) {
        this(poolName, false, priority);
    }

    public DefaultThreadFactory(Class<?> poolType, boolean daemon, int priority) {
        this(toPoolName(poolType), daemon, priority);
    }

    // 将Class类型的poolType获取类名作用于线程名
    public static String toPoolName(Class<?> poolType) {
        ObjectUtil.checkNotNull(poolType, "poolType");

        // 根据Class获取类名
        String poolName = StringUtil.simpleClassName(poolType);
        // 这里判断他的长度如果是0个长度则返回unknown如果一个长度则将它最小化然后返回
        // 如果大于一个长度则判断第一个字符是不是大写第二个字符是不是小写，如过是则把第一个字符小写拼接上后面的字符返回
        // 否则直接返回获取到的类名
        switch (poolName.length()) {
            case 0:
                return "unknown";
            case 1:
                return poolName.toLowerCase(Locale.US);
            default:
                if (Character.isUpperCase(poolName.charAt(0)) && Character.isLowerCase(poolName.charAt(1))) {
                    return Character.toLowerCase(poolName.charAt(0)) + poolName.substring(1);
                } else {
                    return poolName;
                }
        }
    }

    // 上面遗漏了一个参数threadGroup 这个参数是线程组可以为null在这里也并没有任何使用的意义，都在创建线程后的线程设置
    public DefaultThreadFactory(String poolName, boolean daemon, int priority, ThreadGroup threadGroup) {
        ObjectUtil.checkNotNull(poolName, "poolName");

        if (priority < Thread.MIN_PRIORITY || priority > Thread.MAX_PRIORITY) {
            throw new IllegalArgumentException(
                    "priority: " + priority + " (expected: Thread.MIN_PRIORITY <= priority <= Thread.MAX_PRIORITY)");
        }

        // 这里就是拼接线程前缀的地方，刚才处理的名字加上工厂组的id
        prefix = poolName + '-' + poolId.incrementAndGet() + '-';
        this.daemon = daemon;
        this.priority = priority;
        this.threadGroup = threadGroup;
    }

    // 将线程创建的group逻辑在这里操作了一遍，为了防止组为null之前说过可以传入null因为系统会自动设置而系统设置方式和此处一样
    public DefaultThreadFactory(String poolName, boolean daemon, int priority) {
        this(poolName, daemon, priority, System.getSecurityManager() == null ?
                Thread.currentThread().getThreadGroup() : System.getSecurityManager().getThreadGroup());
    }

    // 创建线程
    @Override
    public Thread newThread(Runnable r) {
        // 这里可以看出它使用了一个静态方法做了包装runnable 然后使用前面工厂的前缀名拼接了线程的id号
        Thread t = newThread(FastThreadLocalRunnable.wrap(r), prefix + nextId.incrementAndGet());
        try {
            // 如果创建的线程与工厂不一致比如工厂设置的守护线程工厂daemon是true那么创建的线程是false将会进行设置
            if (t.isDaemon() != daemon) {
                t.setDaemon(daemon);
            }

            if (t.getPriority() != priority) {
                t.setPriority(priority);
            }
        } catch (Exception ignored) {
            // Doesn't matter even if failed to set.
        }
        return t;
    }

    // 实际上就是创建了一个FastThreadLocalThread线程的子类
    protected Thread newThread(Runnable r, String name) {
        return new FastThreadLocalThread(threadGroup, r, name);
    }
}
