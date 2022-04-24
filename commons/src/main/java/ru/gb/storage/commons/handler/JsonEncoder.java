package ru.gb.storage.commons.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import ru.gb.storage.commons.message.Message;

import java.util.List;

public class JsonEncoder extends MessageToMessageEncoder<Message> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, List<Object> out) throws Exception {
        //byte[] value = OBJECT_MAPPER.writeValueAsBytes(msg);
        //out.add(ctx.alloc().buffer().writeBytes(value));
        String value = OBJECT_MAPPER.writeValueAsString(msg);
        //System.out.println("JsonEncoder into String:"+ value);
        ctx.writeAndFlush(value);
    }
}
