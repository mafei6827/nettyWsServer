package com.betmatrix.theonex.netty.handler;

import com.betmatrix.theonex.netty.util.Constants;
import com.betmatrix.theonex.netty.util.NettyUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Junior
 */
public class WebSocketServerHandler extends SimpleChannelInboundHandler<Object> {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketServerHandler.class);

    public static Map<String,AtomicInteger> ipMap = new ConcurrentHashMap<>();

    public static DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private WebSocketServerHandshaker handshaker;

    private static Object lock = new Object();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        ClientSubscribeManager.addPingChannel(ctx.channel());
        //统计ip上的客户端数量
        String ip = NettyUtil.parseChannelRemoteIp(ctx.channel());
        synchronized (lock){
            AtomicInteger count = ipMap.getOrDefault(ip,new AtomicInteger(0));
            count.incrementAndGet();
            ipMap.putIfAbsent(ip,count);
            logger.info("on channelActive, remoteAddress: {}, current ip {} clients: {}",
                    ctx.channel().remoteAddress().toString(), ip, count.get());
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        ClientSubscribeManager.removeChannel(ctx);
        //统计ip上的客户端数量
        String ip = NettyUtil.parseChannelRemoteIp(ctx.channel());
        synchronized(lock){
            AtomicInteger count = ipMap.getOrDefault(ip,new AtomicInteger(0));
            count.decrementAndGet();
            ipMap.putIfAbsent(ip,count);
            logger.info("on channelInactive, remoteAddress: {}, current ip {} clients: {}",
                    ctx.channel().remoteAddress().toString(), ip, count.get());
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            if(Constants.CLIENT_TYPE_CART.equals(ctx.channel().attr(AttributeKey.valueOf("type")).get())){
                handleCartWebSocket(ctx, (WebSocketFrame) msg);
            }else if(Constants.CLIENT_TYPE_LIST.equals(ctx.channel().attr(AttributeKey.valueOf("type")).get())){
                handleListWebSocket(ctx, (WebSocketFrame) msg);
            }else {
                return;
            }
        }
    }



    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        /*if (evt instanceof IdleStateEvent) {
            IdleStateEvent evnet = (IdleStateEvent) evt;
            // 判断Channel是否读空闲, 读空闲时移除Channel
            if (evnet.state().equals(IdleState.READER_IDLE)) {
                final String remoteAddress = NettyUtil.parseChannelRemoteAddr(ctx.channel());
                logger.warn("NETTY SERVER PIPELINE: IDLE exception [{}]", remoteAddress);
                UserInfoManager.removeChannel(ctx.channel());
                UserInfoManager.broadCastInfo(ChatCode.SYS_USER_COUNT,UserInfoManager.getAuthUserCount());
            }
        }
        ctx.fireUserEventTriggered(evt);*/
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        if (!request.decoderResult().isSuccess() || !"websocket".equals(request.headers().get("Upgrade"))) {
            logger.warn("protobuf don't support websocket");
            ctx.channel().close();
            return;
        }
        String uri = request.uri();
        WebSocketServerHandshakerFactory handshakerFactory = new WebSocketServerHandshakerFactory(
                "ws://"+request.headers().get(HttpHeaderNames.HOST) + uri, null, true);
        handshaker = handshakerFactory.newHandshaker(request);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            // 动态加入websocket的编解码处理
            handshaker.handshake(ctx.channel(), request);
        }

        if(Constants.WEBSOCKET_CART_URL.equals(uri)){
            ctx.channel().attr(AttributeKey.valueOf("type")).set(Constants.CLIENT_TYPE_CART);
        }else if(Constants.WEBSOCKET_LIST_URL.equals(uri)){
            ctx.channel().attr(AttributeKey.valueOf("type")).set(Constants.CLIENT_TYPE_LIST);
        }else {
            logger.error(df.format(new Date()) + "client 路径错误。");
            ctx.channel().close();
        }
    }

    private void handleCartWebSocket(ChannelHandlerContext ctx, WebSocketFrame frame) {
        // 判断是否关闭链路命令
        if (handleGeneralWebSocketFrame(ctx, frame)) return;
        String message = ((TextWebSocketFrame) frame).text();
        Channel channel = ctx.channel();
        ClientSubscribeManager.addCartChannel(channel,message);
    }

    private void handleListWebSocket(ChannelHandlerContext ctx, WebSocketFrame frame) {
        // 判断是否关闭链路命令
        if (handleGeneralWebSocketFrame(ctx, frame)) return;
        String message = ((TextWebSocketFrame) frame).text();
        Channel channel = ctx.channel();
        ClientSubscribeManager.addListChannel(channel,message);
    }

    private boolean handleGeneralWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        // 判断是否关闭链路命令
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            //ClientSubscribeManager.removeChannel(ctx);
            return true;
        }
        // 判断是否Ping消息
        if (frame instanceof PingWebSocketFrame) {
            logger.info("ping message:{}");
            ctx.writeAndFlush(new PongWebSocketFrame());
            return true;
        }
        // 判断是否Pong消息
        if (frame instanceof PongWebSocketFrame) {
            logger.info("pong message:{}");
            ctx.writeAndFlush(new PongWebSocketFrame());
            return true;
        }
        // 本程序目前只支持文本消息
        if (!(frame instanceof TextWebSocketFrame)) {
            throw new UnsupportedOperationException(frame.getClass().getName() + " frame type not supported");
        }
        String message = ((TextWebSocketFrame) frame).text();
        if("@heart".equals(message))
            return true;
        return false;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        logger.error("from exceptionCaught:",cause);
    }
}
