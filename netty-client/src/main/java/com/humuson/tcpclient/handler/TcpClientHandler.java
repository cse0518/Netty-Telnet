package com.humuson.tcpclient.handler;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Sharable
public class TcpClientHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) {
        log.info("### 채널이 등록되었습니다 ###");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        String message = msg.toString();
        log.info("### 서버에서 메세지를 수신했습니다 : {}", message);
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
