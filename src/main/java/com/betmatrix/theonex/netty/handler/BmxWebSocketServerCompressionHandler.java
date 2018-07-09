package com.betmatrix.theonex.netty.handler;

import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.http.websocketx.extensions.WebSocketServerExtensionHandler;
import io.netty.handler.codec.http.websocketx.extensions.WebSocketServerExtensionHandshaker;
import io.netty.handler.codec.http.websocketx.extensions.compression.DeflateFrameServerExtensionHandshaker;
import io.netty.handler.codec.http.websocketx.extensions.compression.PerMessageDeflateServerExtensionHandshaker;

/**
 * Created by junior on 16:26 2018/4/28.
 */
public class BmxWebSocketServerCompressionHandler extends WebSocketServerExtensionHandler {

    public BmxWebSocketServerCompressionHandler(WebSocketServerExtensionHandshaker... extensionHandshakers) {

        super(new WebSocketServerExtensionHandshaker[]{new PerMessageDeflateServerExtensionHandshaker
                (6, ZlibCodecFactory.isSupportingWindowSizeAndMemLevel(), 15, true, true),
                new DeflateFrameServerExtensionHandshaker()});
    }

}
