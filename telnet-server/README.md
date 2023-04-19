## Netty-Telnet Server

- [Netty-Telnet] TCP server 설정

<br/>

### build 파일 dependencies 추가 (gradle)

```groovy
dependencies {
	// Webflux
	implementation 'org.springframework.boot:spring-boot-starter-webflux'

	// Netty
	implementation 'io.netty:netty-all:5.0.0.Alpha2'
}
```

### 구현

- `Application`
  - EventLoopGroup 생성
    - Netty는 Java NIO를 사용하므로 `NioEventLoopGroup` 생성
  - ServerBootStrap 생성 및 구성
    - ServerBootStrap 인스턴스 생성 후 EventLoopGroup 설정
    - `NioServerSocketChannel` 인스턴스를 ServerBootstrap의 인스턴스로 설정
  - ChannelInitializer 생성
    - serverBootstrap에 자식 핸들러로 `childHandler()` 메소드 호출
  ```java
  import com.humuson.tcpserver.util.ServerUtil;
  import io.netty.bootstrap.ServerBootstrap;
  import io.netty.channel.EventLoopGroup;
  import io.netty.channel.nio.NioEventLoopGroup;
  import io.netty.channel.socket.nio.NioServerSocketChannel;
  import io.netty.handler.logging.LogLevel;
  import io.netty.handler.logging.LoggingHandler;
  import io.netty.handler.ssl.SslContext;
  
  public class TcpServerApplication {
  
      static final boolean SSL = System.getProperty("ssl") != null;
      static final int PORT = Integer.parseInt(System.getProperty("port", SSL? "8992" : "8023"));
  
      public static void main(String[] args) throws Exception {
          final SslContext sslCtx = ServerUtil.buildSslContext();
  
          EventLoopGroup bossGroup = new NioEventLoopGroup(1);
          EventLoopGroup workerGroup = new NioEventLoopGroup();
          
          try {
              ServerBootstrap b = new ServerBootstrap();
              b.group(bossGroup, workerGroup)
                      .channel(NioServerSocketChannel.class)
                      .handler(new LoggingHandler(LogLevel.INFO))
                      .childHandler(new TcpServerInitializer(sslCtx));
  
              b.bind(PORT).sync().channel().closeFuture().sync();
              
          } finally {
              bossGroup.shutdownGracefully();
              workerGroup.shutdownGracefully();
          }
      }
  }
  ```

- `Initializer`
  - SocketChannel을 통해 `pipeline()`을 생성하고,  
    이 파이프 라인을 통해 들어오는 데이터를 처리하기 위한 핸들러를 붙혀줌
  ```java
  import io.netty.channel.ChannelInitializer;
  import io.netty.channel.ChannelPipeline;
  import io.netty.channel.socket.SocketChannel;
  import io.netty.handler.codec.DelimiterBasedFrameDecoder;
  import io.netty.handler.codec.Delimiters;
  import io.netty.handler.codec.string.StringDecoder;
  import io.netty.handler.codec.string.StringEncoder;
  import io.netty.handler.ssl.SslContext;
  
  public class TcpServerInitializer extends ChannelInitializer<SocketChannel> {
  
      private static final StringDecoder DECODER = new StringDecoder();
      private static final StringEncoder ENCODER = new StringEncoder();
  
      private static final TcpServerHandler SERVER_HANDLER = new TcpServerHandler();
  
      private final SslContext sslCtx;
  
      public TcpServerInitializer(SslContext sslCtx) {
          this.sslCtx = sslCtx;
      }
  
      @Override
      public void initChannel(SocketChannel ch) throws Exception {
          ChannelPipeline pipeline = ch.pipeline();
  
          if (sslCtx != null) {
              pipeline.addLast(sslCtx.newHandler(ch.alloc()));
          }
  
          pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
          pipeline.addLast(DECODER);
          pipeline.addLast(ENCODER);
          pipeline.addLast(SERVER_HANDLER);
      }
  }
  ```

- `Handler`
  - 클라이언트 연결에서 들어오는 데이터를 처리하는 클래스
  - 소켓 채널을 통해 데이터가 수신될 때마다 `messageReceived()` 메소드 호출
    - ChannelHandlerContext를 통해 데이터를 버퍼에 써서 반환
  - 소켓 채널로부터 읽을 수 있는 데이터가 더 이상 존재하지 않을 때 `channelReadComplete()` 호출
    - 마지막으로 채널을 닫아주는 역할
  - 수신 또는 데이터를 전송하는 동안 예외가 발생하는 경우 `exceptionCaught()` 호출
    - 연결을 닫거나 오류 코드로 응답하는 등을 결정
  ```java
  import io.netty.channel.ChannelFuture;
  import io.netty.channel.ChannelFutureListener;
  import io.netty.channel.ChannelHandler.Sharable;
  import io.netty.channel.ChannelHandlerContext;
  import io.netty.channel.SimpleChannelInboundHandler;
  
  import java.net.InetAddress;
  import java.util.Date;
  
  @Sharable
  public class TcpServerHandler extends SimpleChannelInboundHandler<String> {
  
      @Override
      public void channelActive(ChannelHandlerContext ctx) throws Exception {
          ctx.write("Welcome to " + InetAddress.getLocalHost().getHostName() + "!\r\n");
          ctx.write("It is " + new Date() + " now.\r\n");
          ctx.flush();
      }
  
      @Override
      protected void messageReceived(ChannelHandlerContext ctx, String msg) {
          String response;
          boolean close = false;
          if (msg.isEmpty()) {
              response = "Please type something.\r\n";
          } else if ("bye".equalsIgnoreCase(msg)) {
              response = "Have a good day!\r\n";
              close = true;
          } else {
              response = "OK" + "\r\n";
              System.out.println(msg);
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

- `Util`
  - SSL property를 설정해주는 util class
  ```java
  import io.netty.handler.ssl.SslContext;
  import io.netty.handler.ssl.SslContextBuilder;
  import io.netty.handler.ssl.util.SelfSignedCertificate;
  
  import javax.net.ssl.SSLException;
  import java.security.cert.CertificateException;
  
  public class ServerUtil {
  
      private static final boolean SSL = System.getProperty("ssl") != null;
  
      private ServerUtil() {
      }
  
      public static SslContext buildSslContext() throws CertificateException, SSLException {
          if (!SSL) {
              return null;
          }
          SelfSignedCertificate ssc = new SelfSignedCertificate();
          return SslContextBuilder
                  .forServer(ssc.certificate(), ssc.privateKey())
                  .build();
      }
  }
  ```
