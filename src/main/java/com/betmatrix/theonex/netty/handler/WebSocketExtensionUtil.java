package com.betmatrix.theonex.netty.handler;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.websocketx.extensions.WebSocketExtensionData;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by junior on 15:21 2018/5/2.
 */
public class WebSocketExtensionUtil {
    private static final String EXTENSION_SEPARATOR = ",";
    private static final String PARAMETER_SEPARATOR = ";";
    private static final char PARAMETER_EQUAL = '=';

    private static final Pattern PARAMETER = Pattern.compile("^([^=]+)(=[\\\"]?([^\\\"]+)[\\\"]?)?$");

    public static boolean isWebsocketUpgrade(HttpHeaders headers) {
        return headers.containsValue(HttpHeaderNames.CONNECTION, HttpHeaderValues.UPGRADE, true) &&
                headers.contains(HttpHeaderNames.UPGRADE, HttpHeaderValues.WEBSOCKET, true);
    }

    public static List<WebSocketExtensionData> extractExtensions(String extensionHeader) {
        String[] rawExtensions = extensionHeader.split(EXTENSION_SEPARATOR);
        if (rawExtensions.length > 0) {
            List<WebSocketExtensionData> extensions = new ArrayList<WebSocketExtensionData>(rawExtensions.length);
            for (String rawExtension : rawExtensions) {
                String[] extensionParameters = rawExtension.split(PARAMETER_SEPARATOR);
                String name = extensionParameters[0].trim();
                Map<String, String> parameters;
                if (extensionParameters.length > 1) {
                    parameters = new HashMap<String, String>(extensionParameters.length - 1);
                    for (int i = 1; i < extensionParameters.length; i++) {
                        String parameter = extensionParameters[i].trim();
                        Matcher parameterMatcher = PARAMETER.matcher(parameter);
                        if (parameterMatcher.matches() && parameterMatcher.group(1) != null) {
                            parameters.put(parameterMatcher.group(1), parameterMatcher.group(3));
                        }
                    }
                } else {
                    parameters = Collections.emptyMap();
                }
                extensions.add(new WebSocketExtensionData(name, parameters));
            }
            return extensions;
        } else {
            return Collections.emptyList();
        }
    }

    public static String appendExtension(String currentHeaderValue, String extensionName,
                                  Map<String, String> extensionParameters) {

        StringBuilder newHeaderValue = new StringBuilder(
                currentHeaderValue != null ? currentHeaderValue.length() : extensionName.length() + 1);
        if (currentHeaderValue != null && !currentHeaderValue.trim().isEmpty()) {
            newHeaderValue.append(currentHeaderValue);
            newHeaderValue.append(EXTENSION_SEPARATOR);
        }
        newHeaderValue.append(extensionName);
        for (Map.Entry<String, String> extensionParameter : extensionParameters.entrySet()) {
            newHeaderValue.append(PARAMETER_SEPARATOR);
            newHeaderValue.append(extensionParameter.getKey());
            if (extensionParameter.getValue() != null) {
                newHeaderValue.append(PARAMETER_EQUAL);
                newHeaderValue.append(extensionParameter.getValue());
            }
        }
        return newHeaderValue.toString();
    }

    private WebSocketExtensionUtil() {
        // Unused
    }
}
