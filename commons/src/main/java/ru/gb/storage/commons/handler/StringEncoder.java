package ru.gb.storage.commons.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class StringEncoder extends MessageToMessageEncoder<String> {

    @Override
    protected void encode(ChannelHandlerContext ctx, String s, List<Object> list) throws Exception {
        byte[] value = s.getBytes(StandardCharsets.UTF_8);
        System.out.println("StringEncoder " + Arrays.toString(value));
        list.add(ctx.alloc().buffer().writeBytes(value));
    }
}
