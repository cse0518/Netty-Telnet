package com.humuson.tcpclient.consumer;

import io.netty.channel.ChannelFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Consumer {

    private final ChannelFuture netty;

    public Consumer(ChannelFuture netty) {
        this.netty = netty;
    }

    @KafkaListener(topics = "${spring.kafka.topic.name}", containerFactory = "myKafkaListenerContainerFactory")
    public void consume(String message) {
        log.info("### consume data : {}", message);
        netty.channel().writeAndFlush(message);
    }
}
