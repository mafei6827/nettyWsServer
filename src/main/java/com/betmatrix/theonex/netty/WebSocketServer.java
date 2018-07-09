package com.betmatrix.theonex.netty;

import com.betmatrix.theonex.netty.core.BaseServer;
import com.betmatrix.theonex.netty.handler.BmxWebSocketServerCompressionHandler;
import com.betmatrix.theonex.netty.handler.ClientSubscribeManager;
import com.betmatrix.theonex.netty.handler.WebSocketServerHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author junior
 */
public class WebSocketServer extends BaseServer {

    private ScheduledExecutorService executorService;

    public WebSocketServer(int port) {
        this.port = port;
        executorService = Executors.newScheduledThreadPool(2);
    }

    @Override
    public void start() {
        b.group(bossGroup, workGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .localAddress(new InetSocketAddress(port))
                .childHandler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(defLoopGroup,
                                new HttpServerCodec(),   //请求解码器
                                new HttpObjectAggregator(65536),//将多个消息转换成单一的消息对象
                                //new WebSocketHeaderHandler(),
                                new ChunkedWriteHandler(),  //支持异步发送大的码流，一般用于发送文件流
                                //new IdleStateHandler(60, 0, 0), //检测链路是否读空闲
                                new BmxWebSocketServerCompressionHandler(),
                                new WebSocketServerHandler() //处理握手和认证
                        );
                    }
                });
        try {
            cf = b.bind().sync();
            InetSocketAddress addr = (InetSocketAddress) cf.channel().localAddress();
            logger.info("WebSocketServer start success, port is:{}", addr.getPort());


            // 定时扫描keyGroup,清除失效的channelGroup和channel
            executorService.scheduleAtFixedRate(() -> {
                logger.info("scanNotActiveChannel --------");
                ClientSubscribeManager.scanNotActiveChannelGroup();
            }, 3, 60, TimeUnit.SECONDS);


            // 定时向所有客户端发送Ping消息
            executorService.scheduleAtFixedRate(() -> ClientSubscribeManager.broadCastPing(), 3, 10, TimeUnit.SECONDS);

        } catch (InterruptedException e) {
            logger.error("WebSocketServer start fail,", e);
        }
    }

    @Override
    public void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
        }
        super.shutdown();
    }
}
