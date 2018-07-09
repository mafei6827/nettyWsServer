package com.betmatrix.theonex.kafka.listener;
import com.betmatrix.theonex.netty.util.Constants;
import com.betmatrix.theonex.netty.util.PropertiesUtil;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.config.ContainerProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by junior on 14:50 2018/1/16.
 */
public class KafkaConfig {

    private static Properties sysProps;

    static {
        sysProps = PropertiesUtil.loadPropertiesFromFile(Constants.SYSTEM_PROPS_PATH);
    }

    public static  KafkaMessageListenerContainer<String, String> createContainer(
            ContainerProperties containerProps) {
        Map<String, Object> props = consumerProps();
        DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(props);
        KafkaMessageListenerContainer<String, String> container = new KafkaMessageListenerContainer<>(cf, containerProps);
        return container;
    }

    public static  KafkaTemplate<Integer, String> createTemplate() {
        Map<String, Object> senderProps = senderProps();
        ProducerFactory<Integer, String> pf =
                new DefaultKafkaProducerFactory<Integer, String>(senderProps);
        KafkaTemplate<Integer, String> template = new KafkaTemplate<>(pf);
        return template;
    }

    public static  Map<String, Object> consumerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, sysProps.get("app.kafka.bootstrap.servers"));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, sysProps.get("app.kafka.consumer.groupId"));
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, sysProps.get("app.kafka.consumer.autoCommit"));
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, sysProps.get("app.kafka.consumer.commitInterval"));
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, sysProps.get("app.kafka.consumer.sessionTimeout"));
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, sysProps.get("app.kafka.consumer.offsetReset"));
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return props;
    }

    public static  Map<String, Object> senderProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, sysProps.get("app.kafka.bootstrap.servers"));
        props.put(ProducerConfig.RETRIES_CONFIG, 0);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return props;
    }

}
