package ru.gb.storage.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import ru.gb.storage.commons.handler.JsonDecoder;
import ru.gb.storage.commons.handler.JsonEncoder;
import ru.gb.storage.commons.handler.StringDecoder;
import ru.gb.storage.commons.handler.StringEncoder;
import ru.gb.storage.commons.message.AuthMessage;

public class Server implements Runnable{
    private int port;

    public static void main(String[] args) {
        new Server(9000);
    }

    public Server(int port) {
        this.port = port;
        run();
    }

    @Override
    public void run() {
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workGroup = new NioEventLoopGroup();
        try{
            ServerBootstrap server = new ServerBootstrap();
            server.group(bossGroup, workGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<NioSocketChannel>() {
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
                                            if (msg instanceof AuthMessage){
                                                System.out.println("login "+((AuthMessage) msg).getLogin()+
                                                        " password "+((AuthMessage) msg).getPassword());
                                            }
                                        }
                                    });//ServerInputHandler());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    // ограничение очереди заявок на обработку(применяется только для сервера)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            //применяется для всех, говорит,что будет поддерживать соединение с клиентом
            ChannelFuture future = server.bind(port).sync();// запускаем сервер
            System.out.println("Server started.");
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }

    }
}
