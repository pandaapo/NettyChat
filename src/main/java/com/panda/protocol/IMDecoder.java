package com.panda.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.msgpack.MessagePack;
import org.msgpack.MessageTypeException;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自定义IM协议的解码器
 */
public class IMDecoder extends ByteToMessageDecoder {
    //请求内容的正则
    private Pattern pattern = Pattern.compile("^\\[(.*)\\](\\s\\-\\s(.*))?");

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        try {
            //先获取可读字节数
            final int length = in.readableBytes();
            final byte[] array = new byte[length];
            String content = new String(array, in.readerIndex(), length);

            if (!(null == content || "".equals(content.trim()))) {
                if (!IMP.isIMP(content)) {
                    ctx.channel().pipeline().remove(this);
                    return;
                }
            }

            in.getBytes(in.readerIndex(), array, 0, length);
            out.add(new MessagePack().read(array, IMMessage.class));
            in.clear();
        } catch (MessageTypeException e) {
            ctx.channel().pipeline().remove(this);
        }
    }

    /**
     * 字符串解析成自定义即时通信协议
     * @param msg
     * @return
     */
    public IMMessage decode(String msg) {
        if (null == msg || "".equals(msg.trim())) {
            return null;
        }
        try {
            Matcher m = pattern.matcher(msg);
            String header = "";
            String content = "";
            if (m.matches()) {
                header = m.group(1);
                content = m.group(3);
            }

            String[] headers = header.split("\\]\\[");
            long time = 0;
            time = Long.parseLong(headers[1]);
            String nickName = headers[2];
            //昵称最多十个字
            nickName = nickName.length() < 10 ? nickName : nickName.substring(0, 9);
            if (msg.startsWith("[" + IMP.LOGIN.getName() + "]")) {
                return new IMMessage(headers[0], headers[3], time, nickName);
            } else if (msg.startsWith("[" + IMP.CHAT.getName() + "]")) {
                return new IMMessage(headers[0], time, nickName, content);
            } else if (msg.startsWith("[" + IMP.FLOWER.getName() + "]")) {
                return new IMMessage(headers[0], headers[3], time, nickName);
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
