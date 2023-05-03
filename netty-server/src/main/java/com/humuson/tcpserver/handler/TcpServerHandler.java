package com.humuson.tcpserver.handler;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.util.Date;

@Slf4j
@Sharable
public class TcpServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.write("## Welcome to " + InetAddress.getLocalHost().getHostName() + "!\r\n");
        ctx.write("## It is " + new Date() + " now.\r\n");
        ctx.flush();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        String message = msg.toString();
        String response;
        boolean close = false;

        if (message.isEmpty()) {
            response = "## 메세지가 입력되지 않았습니다.\r\n";

        } else if ("bye".equalsIgnoreCase(message)) {
            response = "## 종료합니다.\r\n";
            close = true;

        } else {
            response = "## 서버에서 확인했습니다.\r\n";
            log.info(message);
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
