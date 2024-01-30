package com.heima.kafka.sample;

import org.apache.kafka.clients.producer.*;

import java.util.Properties;

public class ProducerQuickStart {
    public static void main(String[] args) {

        //配置kafka
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.2.9:9092");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

        //创建生产对象
        KafkaProducer<String, String> producer = new KafkaProducer<String, String>(properties);
        //发送消息
        /**
         * 第一个是topic
         * 第二个是key
         * 第三个是value
         */
        ProducerRecord<String, String> kvProducerRecord = new ProducerRecord<String, String>("topic-first", "key-001", "hello,kafka");
        producer.send(kvProducerRecord, (recordMetadata, e) -> {
            if (e != null) {
                System.out.println("记录异常");
            }
            System.out.println(recordMetadata.offset());
        });
        //关闭消息通道
        producer.close();
    }
}
