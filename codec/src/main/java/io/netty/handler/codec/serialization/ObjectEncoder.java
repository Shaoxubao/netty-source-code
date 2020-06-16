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
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * An encoder which serializes a Java object into a {@link ByteBuf}.
 * <p>
 * Please note that the serialized form this encoder produces is not
 * compatible with the standard {@link ObjectInputStream}.  Please use
 * {@link ObjectDecoder} or {@link ObjectDecoderInputStream} to ensure the
 * interoperability with this encoder.
 */
@Sharable
public class ObjectEncoder extends MessageToByteEncoder<Serializable> {
    private static final byte[] LENGTH_PLACEHOLDER = new byte[4];

    // 当需要编码时，encode方法会被回调
    // 参数msg：就是我们需要序列化的java对象
    // 参数out：我们需要将序列化后的二进制字节写到ByteBuf中
    @Override
    protected void encode(ChannelHandlerContext ctx, Serializable msg, ByteBuf out) throws Exception {
        int startIdx = out.writerIndex();

        // ByteBufOutputStream是Netty提供的输出流，数据写入其中之后，可以通过其buffer()方法获得对应的ByteBuf实例
        ByteBufOutputStream bout = new ByteBufOutputStream(out);
        // JDK序列化机制的ObjectOutputStream
        ObjectOutputStream oout = null;
        try {
            // 首先占用4个字节，这就是Length字段的字节数，这只是占位符，后面为填充对象序列化后的字节数
            bout.write(LENGTH_PLACEHOLDER);
            // CompactObjectOutputStream是netty提供的类，其实现了JDK的ObjectOutputStream，顾名思义用于压缩
            // 同时把bout作为其底层输出流，意味着对象序列化后的字节直接写到了bout中
            oout = new CompactObjectOutputStream(bout);
            // 调用writeObject方法，即表示开始序列化
            oout.writeObject(msg);
            oout.flush();
        } finally {
            if (oout != null) {
                oout.close();
            } else {
                bout.close();
            }
        }

        int endIdx = out.writerIndex();

        // 序列化完成，设置占位符的值，也就是对象序列化后的字节数量
        out.setInt(startIdx, endIdx - startIdx - 4);
    }
}
