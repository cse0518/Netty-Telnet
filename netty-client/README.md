## Netty Client

- [Netty] TCP client 설정 후 Kafka Consume 연결
- Kafka 해당 토픽에서 consume 한 후, 소켓 통신을 통해 netty server로 데이터 전송

<br/>

## Index

- [Dependency (Gradle)](#dependency-gradle)
- [Property](#property)
- [구현](#구현)
  - [Netty Configuration](#netty-configuration)
  - [Kafka Configuration](#kafka-configuration)
  - [Consumer](#consumer)
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
  testAnnotationProcessor 'org.projectlombok:lombok'
}
```

<br/>

### Property

- application.yml 설정
- 변수로 불러오기 위해 설정

```yaml
spring:
  kafka:
    bootstrapAddress: localhost:9092
    consumer:
      group-id: testGroup
    topic:
      name: testTopic

netty:
  host: netty-server
  port: 9000
```

<br/>

## 구현

### Netty Configuration

- NioEventLoopGroup과 Bootstrap을 Singleton Bean으로 관리

- `Initializer`
  - ChannelInitializer의 `initChannel` 메소드를 overriding
  - SocketChannel을 통해 `pipeline()`을 생성하고,  
    이 파이프 라인을 통해 들어오는 데이터를 처리하기 위한 핸들러를 붙혀줌

```java
import com.humuson.tcpclient.handler.TcpClientHandler;
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
                        new TcpClientHandler()
                );
              }
            });
  }
}
```

<br/>

### Kafka Configuration

```java
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

  @Value(value = "${spring.kafka.bootstrapAddress}")
  private String bootstrapAddress;

  @Value(value = "${spring.kafka.consumer.group-id}")
  private String groupId;

  public ConsumerFactory<String, String> consumerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

    return new DefaultKafkaConsumerFactory<>(props);
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, String> myKafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory());
    return factory;
  }
}
```

<br/>

### Consumer

- 생성자로 `Bootstrap`, `EventLoopGroup` Bean을 주입받음.  
  인스턴스 생성 이후에 netty-server와 포트 연결(`@PostConstruct`)

- 해당 토픽의 데이터를 consume 한 후, netty-server로 데이터 전송(소켓 통신)

```java
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
