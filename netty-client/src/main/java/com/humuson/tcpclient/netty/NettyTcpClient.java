package com.humuson.tcpclient.netty;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class NettyTcpClient {

    @Value(value = "${netty.host}")
    public String HOST;

    @Value(value = "${netty.port}")
    public int PORT;

    private final EventLoopGroup group;
    private ChannelFuture cf;

    public NettyTcpClient() {
        log.info("NioEventLoopGroup 생성");
        this.group = new NioEventLoopGroup();
    }

    @PreDestroy
    public void shutdown() {
        try {
            log.info("closeFuture...");
            cf.channel().closeFuture().sync();

        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);

        } finally {
            group.shutdownGracefully();
        }
    }

    @Bean
    public ChannelFuture netty() {
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

            cf = b.connect(HOST, PORT).sync();
            log.info("### TCP 연결 서버 - {}", cf.channel().remoteAddress());
            return cf;

        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
