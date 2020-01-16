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
package io.netty.example.discard;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

/**
 * Discards any incoming data.
 */
public final class DiscardServer {

    static final boolean SSL = System.getProperty("ssl") != null;
    static final int PORT = Integer.parseInt(System.getProperty("port", "8009"));

    public static void main(String[] args) throws Exception {
        // Configure SSL.
        final SslContext sslCtx;
        if (SSL) {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        } else {
            sslCtx = null;
        }

        // EventLoopGroup 表示线程池，bossGroup 对应原生中接收新连接的线程，workerGroup 对应原生中处理读写的线程
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            // ServerBootstrap 是服务端的一个启动辅助类，通过给它设置一系列参数来绑定端口启动服务
            ServerBootstrap b = new ServerBootstrap();
            // group() 分别设置处理连接事件和读写事件的线程组
            b.group(bossGroup, workerGroup)
              // 设置通道类型为 NioServerSocketChannel，这是 netty 自己的 Channel，指的是服务器通道，相应的还有客户端通道 NioSocketChannel
             .channel(NioServerSocketChannel.class)
              // handler() 为 bossGroup 中的 Channel 绑定处理器，处理器会监听 Channel 上的事件，具体过程后续会讲到
             .handler(new LoggingHandler(LogLevel.INFO))
              // childHandler() 为 workGroup 中的 Channel 绑定处理器，同上
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) {
                     ChannelPipeline p = ch.pipeline();
                     if (sslCtx != null) {
                         p.addLast(sslCtx.newHandler(ch.alloc()));
                     }
                     p.addLast(new DiscardServerHandler());
                 }
             });

            // Bind and start to accept incoming connections.
            // 真正的启动过程，绑定端口，sync() 表示阻塞等待服务器启动完成
            ChannelFuture f = b.bind(PORT).sync();

            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            // 等待服务器关闭，closeFuture() 表示获取服务器关闭事件，sync() 表示阻塞等待，这行代码会使 main 线程一直阻塞直到服务器关闭
            f.channel().closeFuture().sync();
        } finally {
            // 优雅关闭线程组
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
