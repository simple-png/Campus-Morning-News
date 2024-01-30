package com.heima.common.aliyun;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.green20220302.Client;
import com.aliyun.green20220302.models.TextModerationRequest;
import com.aliyun.green20220302.models.TextModerationResponse;
import com.aliyun.green20220302.models.TextModerationResponseBody;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "aliyun")
public class TextAutoRoute {

    private String accessKeyId;
    private String secret;

    public Map<String, Object> performTextModeration(String textContent) {
        Map<String, Object> resultMap = new HashMap<>();

        Config config = new Config();
        config.setAccessKeyId(accessKeyId);
        config.setAccessKeySecret(secret);
        config.setRegionId("cn-shanghai");
        config.setEndpoint("green-cip.cn-shanghai.aliyuncs.com");
        config.setReadTimeout(6000);
        config.setConnectTimeout(3000);
        Client client = null;
        try {
            client = new Client(config);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        RuntimeOptions runtime = new RuntimeOptions();
        runtime.readTimeout = 10000;
        runtime.connectTimeout = 10000;

        JSONObject serviceParameters = new JSONObject();
        serviceParameters.put("content", textContent);

        if (serviceParameters.get("content") == null || serviceParameters.getString("content").trim().length() == 0) {
            resultMap.put("error", "text moderation content is empty");
            return resultMap;
        }

        TextModerationRequest textModerationRequest = new TextModerationRequest();
        textModerationRequest.setService("chat_detection");
        textModerationRequest.setServiceParameters(serviceParameters.toJSONString());

        try {
            TextModerationResponse response = client.textModerationWithOptions(textModerationRequest, runtime);

            if (response != null) {
                if (500 == response.getStatusCode() || (response.getBody() != null && 500 == (response.getBody().getCode()))) {
                    config.setRegionId("cn-beijing");
                    config.setEndpoint("green-cip.cn-beijing.aliyuncs.com");
                    client = new Client(config);
                    response = client.textModerationWithOptions(textModerationRequest, runtime);
                }
            }

            if (response != null) {
                if (response.getStatusCode() == 200) {
                    TextModerationResponseBody result = response.getBody();
                    resultMap.put("result", JSON.toJSONString(result));

                    Integer code = result.getCode();
                    if (code != null && code == 200) {
                        TextModerationResponseBody.TextModerationResponseBodyData data = result.getData();
                        resultMap.put("labels", data.getLabels());
                        resultMap.put("reason", data.getReason());
                    } else {
                        resultMap.put("error", "text moderation not success. code:" + code);
                    }
                } else {
                    resultMap.put("error", "response not success. status:" + response.getStatusCode());
                }
            }
        } catch (Exception e) {
            resultMap.put("error", e.getMessage());
        }
        return resultMap;
    }
}
