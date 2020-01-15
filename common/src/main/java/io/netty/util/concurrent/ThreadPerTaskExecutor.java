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

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

// 此类是用于包装了线程工厂的一个执行器
// 这里需要对此类有印象后面再讲述执行器实现的时候回使用到他。
// 这个类很简单他的类名很清楚的讲述了，每个任务的执行线程，代表后面的执行器的实现都不会有线程的操作都是有此类进行的，这里要有印象
public final class ThreadPerTaskExecutor implements Executor {
    private final ThreadFactory threadFactory;

    public ThreadPerTaskExecutor(ThreadFactory threadFactory) {
        this.threadFactory = ObjectUtil.checkNotNull(threadFactory, "threadFactory");
    }

    // 当调用此执行器时将会使用线程工厂创建一个线程去执行传入的runnable
    @Override
    public void execute(Runnable command) {
        threadFactory.newThread(command).start();
    }
}
