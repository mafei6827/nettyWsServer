package com.betmatrix.theonex.netty.util;

import io.netty.channel.Channel;

import java.net.SocketAddress;

/**
 * @author junior
 */
public class NettyUtil {

    /**
     * 获取Channel的远程IP地址 带端口
     * @param channel
     * @return
     */
    public static String parseChannelRemoteAddr(final Channel channel) {
        if (null == channel) {
            return "";
        }
        SocketAddress remote = channel.remoteAddress();
        final String addr = remote != null ? remote.toString() : "";

        if (addr.length() > 0) {
            int index = addr.lastIndexOf("/");
            if (index >= 0) {
                return addr.substring(index + 1);
            }

            return addr;
        }

        return "";
    }


    /**
     * 获取Channel的远程IP地址不p
     * @param channel
     * @return
     */
    public static String parseChannelRemoteIp(final Channel channel) {
        String addr = parseChannelRemoteAddr(channel);
        if(addr.length() == 0){
            return "";
        }
        if(addr.contains("")){
            return addr.split(":")[0];
        }
        return "";
    }
}
