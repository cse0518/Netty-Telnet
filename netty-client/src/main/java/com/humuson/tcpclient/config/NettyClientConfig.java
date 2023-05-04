package com.humuson.tcpclient.config;

import com.humuson.tcpclient.handler.NettyClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

@Configuration
public class NettyClientConfig {

    @Bean
    public EventLoopGroup group() {
        return new NioEventLoopGroup();
    }

    @Bean
    public Bootstrap bootstrap(EventLoopGroup group) {
        Bootstrap b = new Bootstrap();
        return b.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(
                                new StringEncoder(StandardCharsets.UTF_8),
                                new NettyClientHandler()
                        );
                    }
                });
    }
}
