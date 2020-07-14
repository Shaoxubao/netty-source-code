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
package io.netty.channel;

/**
 * {@link ChannelHandler} which adds callbacks for state changes. This allows the user
 * to hook in to state changes easily.
 *
 * Netty处理器重要概念
 * 1.Netty的处理器可以分为两类：入站处理器和出战处理器
 * 2.入站处理器顶层是ChannelInboundHandler,出战处理器顶层是ChannelOutboundHandler
 * 3.数据处理时常用的编解码器都是处理器
 * 4.编解码器：无论我们向网络中写入的什么类型(int、char、String、二进制等)，在网络传输中，都是以字节流的方式传输，
 *   将数据由原本的形式转换为字节流的操作称为编码（encode），将数据由字节流形式转换为原本的形式或其他格式的操作成为解码（decode）
 * 5.编码：本质上是一种出战处理器；所以编码是一种ChannelOutboundHandler
 * 6.解码：本质上是一种入站处理器；所以解码是一种ChannelInboundHandler
 * 7.在netty中，编码器通常以xxxEncoder命名；解码器通常以xxxDecoder命名
 *
 * 关于netty编解码器的重要结论：
 * 1. 无论编码器还是解码器，其所接收的的消息类型必须与待处理的参数类型一致，否则该编码器或解码器并不会被执行
 * 2.在解码器进行解码时，一定要判断缓冲（ByteBuf）中数据是否足够，否则会产生一些问题
 */
public interface ChannelInboundHandler extends ChannelHandler {

    /**
     * The {@link Channel} of the {@link ChannelHandlerContext} was registered with its {@link EventLoop}
     */
    void channelRegistered(ChannelHandlerContext ctx) throws Exception;

    /**
     * The {@link Channel} of the {@link ChannelHandlerContext} was unregistered from its {@link EventLoop}
     */
    void channelUnregistered(ChannelHandlerContext ctx) throws Exception;

    /**
     * The {@link Channel} of the {@link ChannelHandlerContext} is now active
     */
    void channelActive(ChannelHandlerContext ctx) throws Exception;

    /**
     * The {@link Channel} of the {@link ChannelHandlerContext} was registered is now inactive and reached its
     * end of lifetime.
     */
    void channelInactive(ChannelHandlerContext ctx) throws Exception;

    /**
     * Invoked when the current {@link Channel} has read a message from the peer.
     */
    void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception;

    /**
     * Invoked when the last message read by the current read operation has been consumed by
     * {@link #channelRead(ChannelHandlerContext, Object)}.  If {@link ChannelOption#AUTO_READ} is off, no further
     * attempt to read an inbound data from the current {@link Channel} will be made until
     * {@link ChannelHandlerContext#read()} is called.
     */
    void channelReadComplete(ChannelHandlerContext ctx) throws Exception;

    /**
     * Gets called if an user event was triggered.
     */
    void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception;

    /**
     * Gets called once the writable state of a {@link Channel} changed. You can check the state with
     * {@link Channel#isWritable()}.
     */
    void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception;

    /**
     * Gets called if a {@link Throwable} was thrown.
     */
    @Override
    @SuppressWarnings("deprecation")
    void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception;
}
