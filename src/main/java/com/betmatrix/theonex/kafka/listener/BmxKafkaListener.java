package com.betmatrix.theonex.kafka.listener;

import com.alibaba.fastjson.JSON;
import com.betmatrix.theonex.kafka.entity.DgtKafkaDto;
import com.betmatrix.theonex.netty.handler.ClientSubscribeManager;
import com.betmatrix.theonex.netty.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.listener.config.ContainerProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author Junior
 */
@Component
@Slf4j
public class BmxKafkaListener {

    private ScheduledExecutorService service;

    private final static String topic = Constants.KAFKA_TOPIC;

    private final static DecimalFormat df = new DecimalFormat("0.00");

    public void listen() {
        ContainerProperties containerProps = new ContainerProperties(topic);
        containerProps.setMessageListener(new MessageListener<String, String>() {
            @Override
            public void onMessage(ConsumerRecord<String, String> record) {
                handleRecord(record);
            }
        });
        KafkaMessageListenerContainer<String, String> container = KafkaConfig.createContainer(containerProps);
        container.setBeanName("AutoListener");
        container.start();
    }

    public void handleRecord(ConsumerRecord<String, String> record) {
        String[] keys = record.key().split("_");
        DgtKafkaDto value = JSON.parseObject(record.value(),DgtKafkaDto.class);
        //Odds的key：matchPeriodId + "_" + DgtKafkaDataType.ODDS.getType()+ "_" + marketId + "_" + oddItem.id + "_"+marketType
        //比赛信息处理
        if ("1".equals(keys[1])) {

        }else{
            //赔率信息处理
            //Odds的key：matchPeriodId + "_" + DgtKafkaDataType.ODDS.getType()+ "_" + marketId + "_" + oddItem.id + "_"+marketType
            String matchPeriodId = String.valueOf(value.getBaseDatas("matchPeriodId"));
            String argument = String.valueOf(value.getBaseDatas("argument"));
            String optionNum = String.valueOf(value.getBaseDatas("optionNum"));
            String marketId = String.valueOf(value.getBaseDatas("marketId"));
            //根据2和992计算argument
/*            if(StormConfig.MARKETIDS_NEED_TRANSFER.contains(Integer.valueOf(marketId))){
                if("2".equals(optionNum)){
                    argument = new BigDecimal(argument).negate().toString();
                }else {
                    argument = new BigDecimal(argument).toString();
                }
            }else {
                argument = new BigDecimal(argument).toString();
            }*/
            DecimalFormat df = new DecimalFormat("0.00");
            value.putBaseDatas("homeTeamArgument",df.format(Double.parseDouble(argument)));
            String oddsKey = matchPeriodId + ":" + marketId + ":" + argument;
            keys = (record.key()+"_"+oddsKey).split("_");
        }

        Map<String, List<String>> msgToSend = new HashMap<>();

        DgtKafkaDto dgtKafkaDto = null;
        dgtKafkaDto = value;
        if (dgtKafkaDto == null)
            return;
        List<String> topicList = (List<String>) dgtKafkaDto.getBaseDatas("topicList");
        if ("1".equals(keys[1])) {
            String dtoJson = JSON.toJSONString(dgtKafkaDto);
            topicList.stream().forEach((topic)->{
                List<String> list = msgToSend.getOrDefault(topic, new LinkedList<>());
                list.add(dtoJson);
                msgToSend.putIfAbsent(topic,list);
            });
        } else {
            String marketId = keys[2];
            Map<String,String> pairValue = (Map<String, String>) value.getBaseDatas("oddsValuesMated");;
            //计算调整后的oddsValue
            List<Map<String, Object>> partners = dgtKafkaDto.fingdInPartner();
            if(partners!=null && partners.size()!=0){
                Integer isInLive = (Integer) dgtKafkaDto.getBaseDatas("isInLive");
                Map<String,String> newPairValue = new HashMap<>();
                for (Map.Entry<String,String> e: pairValue.entrySet()) {
                    double oddPre = Double.parseDouble(e.getValue());
                    double oddPost = getOddsCalculated(marketId, pairValue, partners, oddPre,isInLive);
                    newPairValue.put(e.getKey(),df.format(oddPost));
                    if(dgtKafkaDto.getBaseDatas("optionNum").toString().equals(e.getKey()))
                        dgtKafkaDto.putBaseDatas("oddsValue",df.format(oddPost));
                }
                dgtKafkaDto.putBaseDatas("oddsValuesMated",newPairValue);
            }
            //放入对应topics
            String dtoJson = JSON.toJSONString(dgtKafkaDto);
            topicList.stream().forEach((topic)->{
                List<String> list = msgToSend.getOrDefault(topic, new LinkedList<>());
                list.add(dtoJson);
                msgToSend.putIfAbsent(topic,list);
            });
        }
        ClientSubscribeManager.broadcastMess(msgToSend);
    }

    /**
     * 根据公式计算调整后的oddsValue
     * @param marketId
     * @param pairValue
     * @param partners
     * @param oddPre
     * @param isInLive
     * @return
     */
    private double getOddsCalculated(String marketId, Map<String, String> pairValue, List<Map<String, Object>> partners, double oddPre, Integer isInLive) {
        String profitAdjustment = (String) partners.get(0).get("adjustedMarginRatio");
        String factorPercent = (String) partners.get(0).get("adjustedCoefficientRatio");
        String factor = (String) partners.get(0).get("factor");
        Double coefficient;
        Double rounder;
        if(!StringUtils.isEmpty(profitAdjustment)){
            Double profit = new Double(0);
            for (String oddsValue :pairValue.values()) {
                profit += 1.0/Double.parseDouble(oddsValue);
            }
            if ("37".equals(marketId)|| "687".equals(marketId) || "699".equals(marketId) || "459".equals(marketId) || "600".equals(marketId) || "601".equals(marketId) || "602".equals(marketId))  {
                profit /= 2;
            }
            if (profit <= 1.005 || profit > 1.1236) {
                profit = 1.1236;
            }
            coefficient = profit / (100.0 / (100.0 - Double.parseDouble(profitAdjustment)));
            rounder = (oddPre * coefficient * 100);
            return rounder / 100.0 < 1? 1.01 : rounder.intValue() / 100.0;
        }else if (!StringUtils.isEmpty(factorPercent)) {
            // console.log('oddsAdjustment 调整系数');
            coefficient = Double.parseDouble(factorPercent);
            rounder = (oddPre + (oddPre - 1) * coefficient / 100) * 100;
            return rounder / 100.0 < 1 ? 1.01 : rounder.intValue() / 100.0;
        } else if(isInLive!=null && isInLive==0 && !StringUtils.isEmpty(factor)){
            if (Math.abs(oddPre - Double.parseDouble(factor)) - ((oddPre - 1) * 0.1 / 100)> 0.001)
                return oddPre;
            else
                return Double.parseDouble(factor);
        }else {
            return oddPre;
        }
    }

}
