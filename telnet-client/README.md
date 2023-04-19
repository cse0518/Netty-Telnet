## Netty-Telnet Client

- [Netty-Telnet] TCP client 설정 후 Kafka Consume 연결

<br/>

## Index

- [Dependency (Gradle)](#dependency-gradle)
- [Property](#property)
- [구현](#구현)
  - [Initializer](#initializer)
  - [Kafka Configuration](#kafka-configuration)
  - [Subscriber](#subscriber)
  - [DTO](#dto)

<br/>

### Dependency (Gradle)

```groovy
// Lombok 설정
configurations {
  compileOnly {
    extendsFrom annotationProcessor
  }
}

dependencies {
  // Webflux
  implementation 'org.springframework.boot:spring-boot-starter-webflux'

  // Netty
  implementation 'io.netty:netty-all'

  // Kafka
  implementation 'org.springframework.kafka:spring-kafka'

  // Lombok
  implementation 'org.projectlombok:lombok:1.18.26'
  compileOnly 'org.projectlombok:lombok'
  annotationProcessor 'org.projectlombok:lombok'
}
```

<br/>

### Property

- application.yml 설정
- 변수로 불러오기 위해 설정

```yaml
kafka:
  bootstrapAddress: localhost:9092
topic:
  name: test-topic
```

<br/>

## 구현

### Initializer

- SocketChannel을 통해 pipeline()을 생성하고,  
  이 파이프 라인을 통해 들어오는 데이터를 처리하기 위한 핸들러를 붙혀줌

<br/>

### Kafka Configuration

```java
import com.humuson.tcpclient.dto.TestDto;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Value(value = "${kafka.bootstrapAddress}")
    private String bootstrapAddress;

    public ConsumerFactory<String, TestDto> testConsumerFactory() {
        JsonDeserializer<TestDto> deserializer = new JsonDeserializer<>(TestDto.class);
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeMapperForKey(true);

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, deserializer);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TestDto> testKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, TestDto> factory = new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(testConsumerFactory());
        return factory;
    }
}
```

<br/>

### Subscriber

- 해당 토픽의 데이터를 consume 한 후에, 소켓 통신으로 Telnet Server로 전송

```java
import com.humuson.tcpclient.TcpClientInitializer;
import com.humuson.tcpclient.dto.TestDto;
import com.humuson.tcpclient.util.ServerUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TestSubscriber {

    public static final boolean SSL = System.getProperty("ssl") != null;
    public static final String HOST = System.getProperty("host", "127.0.0.1");
    public static final int PORT = Integer.parseInt(System.getProperty("port", SSL ? "8992" : "8023"));

    @KafkaListener(topics = "${topic.name}", containerFactory = "testKafkaListenerContainerFactory")
    public void testListener(TestDto testDTO) {
        NioEventLoopGroup group = new NioEventLoopGroup();

        try {
            String line = testDTO.toString();
            log.info(line);

            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new TcpClientInitializer(ServerUtil.buildSslContext()));

            Channel ch = b.connect(HOST, PORT).sync().channel();

            ChannelFuture lastWriteFuture = ch.writeAndFlush(line + "\r\n");

            if (lastWriteFuture != null) {
                lastWriteFuture.sync();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }
}
```

<br/>

### DTO

```java
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TestDto {

    private String productName;
    private int cost;
    private int orderId;
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TestDto { ");
        sb.append("productName='").append(productName).append('\'');
        sb.append(", cost= ").append(cost);
        sb.append(", orderId= ").append(orderId);
        sb.append(" }");
        return sb.toString();
    }
}
```
