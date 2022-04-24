package ru.gb.storage.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import ru.gb.storage.commons.handler.JsonDecoder;
import ru.gb.storage.commons.handler.JsonEncoder;
import ru.gb.storage.commons.handler.StringDecoder;
import ru.gb.storage.commons.handler.StringEncoder;
import ru.gb.storage.commons.message.AuthMessage;

public class ClientNetty {

    public static void main(String[] args) throws InterruptedException {
        new ClientNetty().start();
    }

    public void start() throws InterruptedException {
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) throws Exception {
                            ch.pipeline().addLast(
                                    new LengthFieldBasedFrameDecoder(1024*1024,0,3,0,3),
                                    new LengthFieldPrepender(3),
                                    new StringDecoder(),
                                    new StringEncoder(),
                                    new JsonDecoder(),
                                    new JsonEncoder(),
                                    new ChannelInboundHandlerAdapter(){

                                        @Override
                                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                            super.channelRead(ctx, msg);
                                        }
                                    }

                            );
                        }
                    });

            ChannelFuture future = bootstrap.connect("localhost", 9000).sync();
            System.out.println("Client started.");
            Channel channel = future.channel();
            AuthMessage authMessage = new AuthMessage();
            authMessage.setLogin("user");
            authMessage.setPassword("1234");
            System.out.println(authMessage);
            channel.writeAndFlush(authMessage);
            channel.flush();
            future.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}
