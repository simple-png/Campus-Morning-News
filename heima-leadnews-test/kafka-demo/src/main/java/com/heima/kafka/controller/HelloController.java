package com.heima.kafka.controller;

import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

@RestController
public class HelloController {
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @GetMapping("/hello")
    public String hello() {
        HashMap<String, String> map = new HashMap<>();
        map.put("hello","world");
        map.put("name","xiaoming");
        kafkaTemplate.send("itcast-topic", JSON.toJSONString(map));
        return "ok";
    }

}
