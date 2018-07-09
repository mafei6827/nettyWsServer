package com.betmatrix.theonex.kafka.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author junior
 */
@Data
public class DgtKafkaDto {

    @Getter
    @Setter
    private Map<String, Object> head = new HashMap<>();

    @Getter
    @Setter
    private ContentDto content = new ContentDto();

    public void putHead(String key, Object value) {
        head.put(key, value);
    }

    public void putBaseDatas(String key, Object value) {
        content.getBaseData().put(key, value);
    }

    public List<Map<String, Object>> fingdInPartner() {
        return content.getPartnerData();
    }

    public Object getBaseDatas(String key) {
        return content.getBaseData().get(key);
    }

    public void removeBaseDatas(String key) {
        content.getBaseData().remove(key);
    }
    @Data
    private class ContentDto {

        private Map<String, Object> baseData = new HashMap<>();

        private List<Map<String, Object>> partnerData = new ArrayList<>();
    }

}
