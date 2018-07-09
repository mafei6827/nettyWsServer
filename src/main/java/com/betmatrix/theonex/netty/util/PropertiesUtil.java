package com.betmatrix.theonex.netty.util;

import java.io.IOException;
import java.util.Properties;

/**
 * Created by junior on 10:39 2018/6/22.
 */
public class PropertiesUtil {

    public static Properties loadPropertiesFromFile(String fileName){
        Properties props = new Properties();
        try {
            props.load(Class.class.getResourceAsStream(fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return props;
    }
}
