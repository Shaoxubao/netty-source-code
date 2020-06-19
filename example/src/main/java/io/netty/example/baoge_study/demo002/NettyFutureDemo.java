package io.netty.example.baoge_study.demo002;

import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

/**
 * @Author shaoxubao
 * @Date 2020/6/19 14:38
 */
public class NettyFutureDemo {
    public static void main(String[] args) throws InterruptedException {

        long l = System.currentTimeMillis();

        EventExecutorGroup group = new DefaultEventExecutorGroup(4); // 创建执行线程池SingleThreadEventExecutor

        Future<Integer> f = group.submit(new Callable<Integer>() {           // 创建Callable
            @Override
            public Integer call() throws Exception {
                System.out.println("执行耗时操作...");
                timeConsumingOperation();
                return 100;
            }
        });

        f.addListener(new FutureListener<Object>() { // 添加匿名监听者
            @Override
            public void operationComplete(Future<Object> objectFuture) throws Exception {
                System.out.println("计算结果:：" + objectFuture.get()); // promise.get获取集果
            }
        });
        // 也可以简写如下
        f.addListener((FutureListener) future -> {
            System.out.println("计算结果2:：" + future.get());

        });

        System.out.println("主线程运算耗时:" + (System.currentTimeMillis() - l) + "ms");

        new CountDownLatch(1).await();
    }

    static void timeConsumingOperation() {
        try {
            Thread.sleep(3000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
