package com.betmatrix.theonex.netty.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;

/**
 * Created by junior on 15:48 2018/5/2.
 */
public class WebSocketHeaderHandler extends SimpleChannelInboundHandler<Object> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;
            HttpHeaders headers = request.headers();
            headers.set("sec-websocket-extensions","permessage-deflate; client_no_context_takeover; server_no_context_takeover");
        }
        ctx.fireChannelRead(msg);
    }
}
