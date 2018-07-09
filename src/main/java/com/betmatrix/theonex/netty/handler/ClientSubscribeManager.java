package com.betmatrix.theonex.netty.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.betmatrix.theonex.netty.util.Constants;
import com.betmatrix.theonex.netty.util.NettyUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Channel的管理器，用来管理channel的新增、删除、以及发送信息
 *
 * @author Junior
 */
public class ClientSubscribeManager {
    private static final Logger logger = LoggerFactory.getLogger(ClientSubscribeManager.class);

    private static ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);

    private static ConcurrentMap<Channel, Set<String>> clientSubs = new ConcurrentHashMap<>();

    private static ConcurrentMap<String, ChannelGroup> keyGroups = new ConcurrentHashMap<>();

    private static AtomicInteger cartClientCount = new AtomicInteger(0);

    private static AtomicInteger listClientCount = new AtomicInteger(0);

    private static AtomicInteger pingClientCount = new AtomicInteger(0);

    private static ExecutorService executorService = Executors.newScheduledThreadPool(17);


    public static void addPingChannel(Channel channel) {
        try {
            ChannelGroup channelGroup = keyGroups.getOrDefault("ping", new DefaultChannelGroup(GlobalEventExecutor.INSTANCE));
            channelGroup.add(channel);
            keyGroups.putIfAbsent("ping", channelGroup);
            pingClientCount.incrementAndGet();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addCartChannel(Channel channel, String message) {
        try {
            Set<String> topics = ((JSONArray) JSON.parseObject(message).get("matches")).stream().map(x -> x.toString()).collect(Collectors.toSet());
            handleChannelTopics(channel, topics, cartClientCount);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addListChannel(Channel channel, String message) {
        try {
            Set<String> topics = ((JSONArray) JSON.parseObject(message).get("keys")).stream().map(x -> x.toString()).collect(Collectors.toSet());
            handleChannelTopics(channel,topics,listClientCount);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleChannelTopics(Channel channel, Set<String> topics, AtomicInteger clientCount) {
        try {
            rwLock.writeLock().lock();
            //去除掉之前的订阅
            Boolean flag = false;
            Set<String> topics_prev = clientSubs.get(channel);
            if(topics_prev != null){
                for (String tp : topics_prev) {
                    ChannelGroup channelGroup = keyGroups.getOrDefault(tp, new DefaultChannelGroup(GlobalEventExecutor.INSTANCE));
                    if(channelGroup.find(channel.id()) != null){
                        channelGroup.remove(channel);
                        flag = true;
                    }
                    keyGroups.putIfAbsent(tp, channelGroup);
                }
                if(flag) clientCount.decrementAndGet();
            }
            //添加新的订阅
            for (String topic : topics) {
                ChannelGroup channelGroup = keyGroups.getOrDefault(topic, new DefaultChannelGroup(GlobalEventExecutor.INSTANCE));
                channelGroup.add(channel);
                keyGroups.putIfAbsent(topic, channelGroup);
            }
            clientSubs.put(channel,topics);
            clientCount.incrementAndGet();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * 从缓存中移除Channel，并且关闭Channel
     */
    public static void removeChannel(ChannelHandlerContext ctx) {
        try {
            Channel channel = ctx.channel();
            logger.warn("channel will be remove, address is :{}", NettyUtil.parseChannelRemoteAddr(channel));
            rwLock.writeLock().lock();
            channel.close();
            if (Constants.CLIENT_TYPE_CART.equals(ctx.channel().attr(AttributeKey.valueOf("type")).get())) {
                cartClientCount.decrementAndGet();
            } else if (Constants.CLIENT_TYPE_LIST.equals(ctx.channel().attr(AttributeKey.valueOf("type")).get())) {
                listClientCount.decrementAndGet();
            }
            pingClientCount.decrementAndGet();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * 广播普通消息
     *
     * @param records
     */
    public static void broadcastMess(Map<String, List<String>> records) {
        try {
            rwLock.readLock().lock();
            for (String topic : records.keySet()) {
                if(keyGroups.containsKey(topic)){
                    ChannelGroup channelGroup = keyGroups.get(topic);
                    List<String> result = records.get(topic);
                    executorService.execute(() -> {
                        if (result.size() == 0) return;
                        long start = System.currentTimeMillis();
                        try {
                            String json = JSON.toJSONString(result);
                            channelGroup.writeAndFlush(new TextWebSocketFrame(json));
                            logger.info("sentTochannelGroup, ,result size:{},cost time:{}, topic: {},channelGroup size: {},current listClient number:{}, current cartClient number:{}",
                                    result.size(),(System.currentTimeMillis()-start)+"ms", topic,channelGroup.size(),listClientCount.get(), cartClientCount.get());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * 向所有连接上来的client发送心跳
     */
    public static void broadCastPing() {
        try {
            rwLock.readLock().lock();
            logger.info("broadCastPing pingClientCount: {}", pingClientCount.intValue());
            ChannelGroup channelGroup = keyGroups.get("ping");
            if (channelGroup != null) {
                channelGroup.writeAndFlush(new TextWebSocketFrame("ping"));
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * 扫描并删除空的channelGroup
     */
    public static void scanNotActiveChannelGroup() {
        for (String topic : keyGroups.keySet()) {
            Iterator<Channel> iterator = keyGroups.get(topic).iterator();
            while (iterator.hasNext()){
                Channel ch = iterator.next();
                if (!ch.isOpen() || !ch.isActive()) {
                    ch.close();
                }
            }
            if (keyGroups.get(topic).size() == 0) {
                keyGroups.remove(topic);
            }
        }
    }


    public static int getAuthUserCount() {
        return cartClientCount.get() + listClientCount.get();
    }

}
