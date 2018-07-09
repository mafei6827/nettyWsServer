package com.betmatrix.theonex.netty.client;

import com.betmatrix.theonex.netty.handler.BmxWebSocketClientCompressionHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.IdleStateHandler;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

/**
 * Created by junior on 18:27 2018/3/20.
 */
public class NettyClient {
    static final String URL = System.getProperty("url", "ws://52.77.189.132/matches/48/2");

    private NioEventLoopGroup workGroup = new NioEventLoopGroup(4);
    private Channel channel;
    private Bootstrap bootstrap;
    private WebSocketClientHandler handler;

    public static void main(String[] args) throws Exception {
        NettyClient client = new NettyClient();
        client.start();
        while (true){

        }

    }

    public void start() throws URISyntaxException, SSLException {
        URI uri = new URI(URL);
        String scheme = uri.getScheme() == null ? "ws" : uri.getScheme();
        final String host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();
        final int port;
        if (uri.getPort() == -1) {
            if ("ws".equalsIgnoreCase(scheme)) {
                port = 80;
            } else if ("wss".equalsIgnoreCase(scheme)) {
                port = 443;
            } else {
                port = -1;
            }
        } else {
            port = uri.getPort();
        }

        if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
            System.err.println("Only WS(S) is supported.");
            return;
        }

        final boolean ssl = "wss".equalsIgnoreCase(scheme);
        final SslContext sslCtx;
        if (ssl) {
            sslCtx = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } else {
            sslCtx = null;
        }

        EventLoopGroup group = new NioEventLoopGroup();
        try {

            bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            /*ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast("http-codec", new HttpClientCodec());
                            pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
                            pipeline.addLast("ws-handler", handler);
                            pipeline.addLast("compressionExtensions", WebSocketClientCompressionHandler.INSTANCE);*/
                            ChannelPipeline p = ch.pipeline();
                            handler = new WebSocketClientHandler(
                                    WebSocketClientHandshakerFactory.newHandshaker(
                                            uri, WebSocketVersion.V13, null, true, new DefaultHttpHeaders(), 1000000));
                            p.addLast(
                                    new HttpClientCodec(),
                                    new HttpObjectAggregator(1),
                                    new IdleStateHandler(15,15,15),
                                    BmxWebSocketClientCompressionHandler.INSTANCE,
                                    handler
                                    );
                        }
                    });
            doConnect();

            while (true){

            }
        }finally {
            group.shutdownGracefully();
        }
    }

    protected void doConnect() {
        if (channel != null && channel.isActive()) {
            return;
        }

        ChannelFuture future = bootstrap.connect("47.52.239.160", 80);

        future.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture futureListener) throws Exception {
                if (futureListener.isSuccess()) {
                    channel = futureListener.channel();
                    System.out.println("Connect to server successfully!");
                    channel.closeFuture().addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture channelFuture) throws Exception {
                            futureListener.channel().eventLoop().schedule(new Runnable() {
                                @Override
                                public void run() {
                                    doConnect();
                                }
                            }, 1, TimeUnit.SECONDS);
                        }
                    });
                } else {
                    System.out.println("Failed to connect to server, try connect after 10s");
                    futureListener.channel().eventLoop().schedule(new Runnable() {
                        @Override
                        public void run() {
                            doConnect();
                        }
                    }, 1, TimeUnit.SECONDS);
                    futureListener.channel().close();
                }
            }
        });
        /*while(true){
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            future.channel().writeAndFlush("{2,7,\"data\":[0, 253345], m:[");
        }*/

    }
}
