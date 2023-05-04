## Netty-Telnet Server

- [Netty] TCP server 설정

<br/>

## Index

- [Dependency (Gradle)](#dependency-gradle)
- [구현](#구현)
  - [Application](#application)
  - [Handler](#handler)
  - [DTO](#dto)

<br/>

### Dependency (Gradle)

```groovy
dependencies {
	// Webflux
	implementation 'org.springframework.boot:spring-boot-starter-webflux'

	// Netty
	implementation 'io.netty:netty-all'
}
```

<br/>

## 구현

### Application

- EventLoopGroup 생성
  - Netty는 Java NIO를 사용하므로 `NioEventLoopGroup` 생성

- ServerBootStrap 생성 및 구성
  - ServerBootStrap 인스턴스 생성 후 EventLoopGroup 설정
  - `NioServerSocketChannel` 인스턴스를 ServerBootstrap의 인스턴스로 설정

- ChannelInitializer 생성
  - serverBootstrap에 자식 핸들러로 `childHandler()` 메소드 호출
  - ChannelInitializer의 `initChannel` 메소드를 overriding
  - SocketChannel을 통해 `pipeline()`을 생성하고,  
    이 파이프 라인을 통해 들어오는 데이터를 처리하기 위한 핸들러를 붙혀줌

```java
import com.humuson.tcpserver.handler.NettyServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyServerApplication {

  public static final String HOST = "netty-server";
  public static final int PORT = 9000;

  public static void main(String[] args) throws Exception {
    EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    EventLoopGroup workerGroup = new NioEventLoopGroup();

    try {
      ServerBootstrap b = new ServerBootstrap();
      b.group(bossGroup, workerGroup)
              .channel(NioServerSocketChannel.class)
              .handler(new LoggingHandler(LogLevel.INFO))
              .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                  ch.pipeline().addLast(new NettyServerHandler());
                }
              });

      ChannelFuture cf = b.bind(HOST, PORT).sync();
      log.info("## Server started - host: {}, port: {}", HOST, PORT);
      cf.channel().closeFuture().sync();

    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }
}
```

<br/>

### Handler

- 클라이언트 연결에서 들어오는 데이터를 처리하는 클래스

- 소켓 채널을 통해 데이터가 수신될 때마다 `channelRead()` 메소드 호출
  - ChannelHandlerContext를 통해 데이터를 버퍼에 써서 반환

- 소켓 채널로부터 읽을 수 있는 데이터가 더 이상 존재하지 않을 때 `channelReadComplete()` 호출
  - 마지막으로 채널을 닫아주는 역할

- 수신 또는 데이터를 전송하는 동안 예외가 발생하는 경우 `exceptionCaught()` 호출
  - 연결을 닫거나 오류 코드로 응답하는 등을 결정

```java
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.humuson.tcpserver.dto.TestDto;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.Date;

@Slf4j
@Sharable
public class NettyServerHandler extends ChannelInboundHandlerAdapter {

  public ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    ctx.write("## Welcome to " + InetAddress.getLocalHost().getHostName() + "!\r\n");
    ctx.write("## It is " + new Date() + " now.\r\n");
    ctx.flush();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    String message = ((ByteBuf) msg).toString(Charset.defaultCharset());
    String response;
    boolean close = false;

    if (message.isEmpty()) {
      response = "## 메세지가 입력되지 않았습니다.\r\n";

    } else if ("bye".equalsIgnoreCase(message)) {
      response = "## 종료합니다.\r\n";
      close = true;

    } else {
      response = "## 서버에서 확인했습니다.\r\n";
      log.info("## 수신 Data : {}", message);

      try {
        TestDto testDto = objectMapper.readValue(message, TestDto.class);
        log.info("## dto로 변환 성공\ndto.orderId : {}", testDto.getOrderId());

      } catch (JsonProcessingException e) {
        log.info("## dto로 변환 실패");
        throw new RuntimeException(e);
      }
    }

    ChannelFuture future = ctx.write(response);

    if (close) {
      future.addListener(ChannelFutureListener.CLOSE);
    }
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    ctx.flush();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    cause.printStackTrace();
    ctx.close();
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
