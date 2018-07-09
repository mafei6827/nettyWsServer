package com.betmatrix.theonex.netty;

import com.betmatrix.theonex.kafka.listener.BmxKafkaListener;
import com.betmatrix.theonex.netty.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WebSocket Netty服务器
 * @author junior
 */
public class WebSocketMain {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketMain.class);

    public static void main(String[] args) {
        final WebSocketServer server = new WebSocketServer(Constants.DEFAULT_PORT);
        server.init();
        server.start();
        final BmxKafkaListener bmxKafkaListener = new BmxKafkaListener();
        bmxKafkaListener.listen();
        // 注册进程钩子，在JVM进程关闭前释放资源
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run(){
                server.shutdown();
                logger.warn(">>>>>>>>>> jvm shutdown");
                System.exit(0);
            }
        });
    }
}
