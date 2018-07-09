package com.betmatrix.theonex.netty.handler;

import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.http.websocketx.extensions.WebSocketClientExtensionHandler;
import io.netty.handler.codec.http.websocketx.extensions.WebSocketClientExtensionHandshaker;
import io.netty.handler.codec.http.websocketx.extensions.compression.DeflateFrameClientExtensionHandshaker;
import io.netty.handler.codec.http.websocketx.extensions.compression.PerMessageDeflateClientExtensionHandshaker;

/**
 * Created by junior on 14:29 2018/4/9.
 */
@ChannelHandler.Sharable
public class BmxWebSocketClientCompressionHandler extends WebSocketClientExtensionHandler {

    public static final BmxWebSocketClientCompressionHandler INSTANCE = new BmxWebSocketClientCompressionHandler();

    public BmxWebSocketClientCompressionHandler(){
        super(new WebSocketClientExtensionHandshaker[]{new PerMessageDeflateClientExtensionHandshaker(6, ZlibCodecFactory.isSupportingWindowSizeAndMemLevel(), 15, true, true),
                new DeflateFrameClientExtensionHandshaker(false),
                new DeflateFrameClientExtensionHandshaker(true)});
    }
}
