package com.humuson.tcpclient;

import com.humuson.tcpclient.util.ServerUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@SpringBootApplication
public class TcpClientApplication {

    public static final boolean SSL = System.getProperty("ssl") != null;
    public static final String HOST = System.getProperty("host", "127.0.0.1");
    public static final int PORT = Integer.parseInt(System.getProperty("port", SSL ? "8992" : "8023"));

    public static final Queue<Object> stage = new ConcurrentLinkedQueue<>();

    public static void main(String[] args) throws CertificateException, SSLException {
        SslContext sslContext = ServerUtil.buildSslContext();

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new TcpClientInitializer(sslContext));

            Channel ch = b.connect(HOST, PORT).sync().channel();
            while (true) {
                if (stage.isEmpty()) {
                    Thread.sleep(5000);
                    continue;
                }

                String data = stage.poll().toString();
                ch.writeAndFlush(data + "\r\n");
            }

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            group.shutdownGracefully();
        }
    }
}
