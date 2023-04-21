package com.humuson.tcpclient.subscriber;

import com.humuson.tcpclient.TcpClientApplication;
import com.humuson.tcpclient.dto.TestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TestSubscriber {

    @KafkaListener(topics = "${topic.name}", containerFactory = "testKafkaListenerContainerFactory")
    public void testListener(TestDto testDTO) {
        log.info("consume data : " + testDTO.toString());
        TcpClientApplication.stage.add(testDTO);
    }
}
