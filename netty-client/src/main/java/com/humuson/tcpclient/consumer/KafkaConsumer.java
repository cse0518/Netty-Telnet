package com.humuson.tcpclient.consumer;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Slf4j
@Component
public class KafkaConsumer {

    private final String HOST;
    private final int PORT;
    private final Bootstrap bootstrap;
    private final EventLoopGroup group;
    private Channel channel;

    public KafkaConsumer(@Value(value = "${netty.host}") String HOST,
                         @Value(value = "${netty.port}") int PORT,
                         Bootstrap bootstrap,
                         EventLoopGroup group) {
        this.HOST = HOST;
        this.PORT = PORT;
        this.bootstrap = bootstrap;
        this.group = group;
    }

    @PostConstruct
    private void connect() throws InterruptedException {
        ChannelFuture channelFuture = bootstrap.connect(HOST, PORT).sync();
        if (channelFuture.isSuccess()) {
            System.out.println("## Successfully connected to Netty server");
            channel = channelFuture.channel();
        } else {
            System.out.println("## Failed to connect to Netty server");
            throw new InterruptedException();
        }
    }

    @KafkaListener(topics = "${spring.kafka.topic.name}", containerFactory = "myKafkaListenerContainerFactory")
    public void consume(String message) throws InterruptedException {
        if (channel == null) {
            connect();
        }

        log.info("### consume data : {}", message);
        ByteBuf byteBuf = Unpooled.copiedBuffer(message, CharsetUtil.UTF_8);
        channel.writeAndFlush(byteBuf);
    }

    @PreDestroy
    public void shutdown() throws InterruptedException {
        channel.close().sync();
        group.shutdownGracefully();
    }
}
