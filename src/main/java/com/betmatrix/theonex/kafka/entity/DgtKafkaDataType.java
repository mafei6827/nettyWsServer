package com.betmatrix.theonex.kafka.entity;

import lombok.Getter;

/**
 * @author junior
 */
public enum DgtKafkaDataType {
    PING(0),
    MATCH(1),
    ODDS(2),
    PRIODDS(3);


    @Getter
    private int type;

    DgtKafkaDataType(int type) {
        this.type = type;
    }
}
