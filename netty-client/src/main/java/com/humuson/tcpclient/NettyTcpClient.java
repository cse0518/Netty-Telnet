package com.humuson.tcpclient;

import com.humuson.tcpclient.handler.TcpClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@SpringBootApplication
public class NettyTcpClient {

    public static String HOST = "localhost"; // netty-server
    public static int PORT = 9000;
    public static Queue<String> sendQueue = new ConcurrentLinkedQueue<>();

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(
                                    new StringEncoder(StandardCharsets.UTF_8),
                                    new TcpClientHandler()
                            );
                        }
                    });

            ChannelFuture cf = b.connect(HOST, PORT).sync();
            log.info("### TCP 연결 서버 - {}", cf.channel().remoteAddress());

            while (true) {
                if (sendQueue.isEmpty()) {
                    Thread.sleep(5000);
                    continue;
                }

                String message = sendQueue.poll();
                if (message.equals("bye")) {
                    cf.channel().writeAndFlush("### 접속을 종료합니다.");
                    break;
                }
                cf.channel().writeAndFlush(message);
            }

            cf.channel().closeFuture().sync();

        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);

        } finally {
            group.shutdownGracefully();
        }
    }
}
