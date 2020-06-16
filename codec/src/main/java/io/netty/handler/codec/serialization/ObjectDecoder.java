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
package io.netty.handler.codec.serialization;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;

/**
 * A decoder which deserializes the received {@link ByteBuf}s into Java
 * objects.
 * <p>
 * Please note that the serialized form this decoder expects is not
 * compatible with the standard {@link ObjectOutputStream}.  Please use
 * {@link ObjectEncoder} or {@link ObjectEncoderOutputStream} to ensure the
 * interoperability with this decoder.
 */
public class ObjectDecoder extends LengthFieldBasedFrameDecoder {

    private final ClassResolver classResolver;

    /**
     * Creates a new decoder whose maximum object size is {@code 1048576}
     * bytes.  If the size of the received object is greater than
     * {@code 1048576} bytes, a {@link StreamCorruptedException} will be
     * raised.
     *
     * @param classResolver  the {@link ClassResolver} to use for this decoder
     */
    public ObjectDecoder(ClassResolver classResolver) {
        this(1048576, classResolver);
    }

    /**
     * Creates a new decoder with the specified maximum object size.
     *
     * @param maxObjectSize  the maximum byte length of the serialized object.
     *                       if the length of the received object is greater
     *                       than this value, {@link StreamCorruptedException}
     *                       will be raised.
     * @param classResolver    the {@link ClassResolver} which will load the class
     *                       of the serialized object
     *
     * 参数maxObjectSize：表示可接受的对象反序列化的最大字节数，默认为1048576 bytes，约等于1M
     * 参数classResolver：由于需要将二进制字节反序列化为Java对象，需要指定一个ClassResolver来加载这个类的字节码对象
     */
    public ObjectDecoder(int maxObjectSize, ClassResolver classResolver) {
        // 调用父类LengthFieldBasedFrameDecoder构造方法
        super(maxObjectSize, 0, 4, 0, 4);
        this.classResolver = classResolver;
    }

    // 当需要解码时，decode方法会被回调
    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        // 首先调用父类的decode方法进行解码，会解析出包含可解析为java对象的完整二进制字节封装到ByteBuf中，同时Length字段的4个字节会被删除
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        if (frame == null) {
            return null;
        }

        // 构造JDK ObjectInputStream实例用于解码
        ObjectInputStream ois = new CompactObjectInputStream(new ByteBufInputStream(frame, true), classResolver);
        try {
            // 调用readObject方法进行解码，其返回的就是反序列化之后的Java对象
            return ois.readObject();
        } finally {
            ois.close();
        }
    }
}
