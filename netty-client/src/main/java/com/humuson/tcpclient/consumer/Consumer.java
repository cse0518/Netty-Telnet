package com.humuson.tcpclient.consumer;

import com.humuson.tcpclient.NettyTcpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Consumer {

    @KafkaListener(topics = "${spring.kafka.topic.name}", containerFactory = "kafkaListenerContainerFactory")
    public void consume(String message) {
        log.info("### consume data : {}", message);
        NettyTcpClient.sendQueue.add(message);
    }
}
