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
import ru.gb.storage.commons.message.FileContentMessage;
import ru.gb.storage.commons.message.FileRequestMessage;
import ru.gb.storage.commons.message.Message;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class ClientNetty {
    private Channel channel = null;

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
                                    new SimpleChannelInboundHandler<Message>(){

                                        @Override
                                        public void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
                                           // super.channelRead(ctx, msg);
                                            if (msg instanceof FileContentMessage) {
                                                taskFileReceive(msg);
                                            }
                                        }
                                    }

                            );
                        }
                    });

            ChannelFuture future = bootstrap.connect("localhost", 9000).sync();
            System.out.println("Client started.");
            channel = future.channel();
            AuthMessage authMessage = new AuthMessage();
            authMessage.setLogin("user");
            authMessage.setPassword("1234");
            System.out.println(authMessage);
            channel.writeAndFlush(authMessage);
            Scanner sc = new Scanner(System.in);
            while (true) {
                System.out.println("Введите(1 - передать файл, 2 - получить файл, 0 - выйти):");
                String s = sc.next();
                if (s.equals("0")) {
                    break;
                }else {
                    System.out.println("Введите имя файла");
                    String fileName = sc.next();
                    if (s.equals("1")) {
                        taskFileSend(fileName);

                    }
                    if (s.equals("2")) {
                        FileRequestMessage msg = new FileRequestMessage();
                        Path path = Paths.get(fileName);
                        msg.setPath(String.valueOf(path));
                        channel.writeAndFlush(msg);
                    }
                }
            }
            future.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }

    private void taskFileReceive(Message msg){
        FileContentMessage fcm =(FileContentMessage) msg;
        try {
            Path path = Paths.get(".","client_repository", fcm.getFileName());
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
    private void taskFileSend(String fileName) {
        Path path = Paths.get(".", "client_repository", fileName);
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
                message.setFileName(fileName);
                message.setStartPosition(accessFile.getFilePointer());
                accessFile.read(fileContent);
                message.setContent(fileContent);
                message.setLast(accessFile.getFilePointer() == accessFile.length());
                channel.writeAndFlush(message);
            }
            accessFile.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
