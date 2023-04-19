package com.humuson.tcpclient.subscriber;

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
