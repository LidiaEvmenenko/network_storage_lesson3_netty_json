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
import ru.gb.storage.commons.message.FileContentMessage;
import ru.gb.storage.commons.message.FileRequestMessage;
import ru.gb.storage.commons.message.Message;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Server implements Runnable{
    private int port;
    private String username;

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
                                    new SimpleChannelInboundHandler<Object>(){
                                        @Override
                                        public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
                                            if (msg instanceof AuthMessage){
                                                System.out.println("login "+((AuthMessage) msg).getLogin()+
                                                        " password "+((AuthMessage) msg).getPassword());
                                                AuthMessage message = (AuthMessage) msg;
                                                username = message.getLogin();

                                                Path path = Paths.get( ".", "server_repository", message.getLogin());
                                                if (!Files.exists(path)){
                                                    Files.createDirectory(path);
                                                }
                                            }
                                            if (msg instanceof FileContentMessage) {
                                                taskFileContentMessage((Message) msg);
                                            }
                                            if (msg instanceof FileRequestMessage) {
                                                System.out.println("Пришло сообщение");
                                                taskFileRequestMessage(ctx, msg);

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

    private void taskFileContentMessage(Message msg){
        FileContentMessage fcm =(FileContentMessage) msg;
        try {
            Path path = Paths.get(".","server_repository", username, fcm.getFileName());
            System.out.print("Получен файл: ");
            System.out.println(path);
            RandomAccessFile accessFile = new RandomAccessFile(String.valueOf(path),"rw");
            accessFile.seek(fcm.getStartPosition());
            accessFile.write(fcm.getContent());
            if (fcm.isLast()){
                accessFile.close();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private void taskFileRequestMessage(ChannelHandlerContext ctx, Object msg){
        FileRequestMessage frm = (FileRequestMessage) msg;
        Path path = Paths.get(".", "server_repository", username, frm.getPath());
        System.out.print("Передается файл: ");
        System.out.println(path);
        File file = new File(String.valueOf(path));
        try {
            RandomAccessFile accessFile = new RandomAccessFile(file, "r");
            while (accessFile.getFilePointer() != accessFile.length()) {
                byte[] fileContent;
                long avaible = accessFile.length() - accessFile.getFilePointer();
                if (avaible > 64 * 1024) {
                    fileContent = new byte[64 * 1024];
                } else {
                    fileContent = new byte[(int) avaible];
                }
                FileContentMessage message = new FileContentMessage();
                message.setStartPosition(accessFile.getFilePointer());
                accessFile.read(fileContent);
                message.setFileName(frm.getPath());
                message.setContent(fileContent);
                message.setLast(accessFile.getFilePointer() == accessFile.length());
                ctx.writeAndFlush(message);
            }
            accessFile.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
