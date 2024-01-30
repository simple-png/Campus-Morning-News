package com.heima.common.aliyun;

import com.alibaba.fastjson.JSON;
import com.aliyun.green20220302.Client;
import com.aliyun.green20220302.models.ImageModerationRequest;
import com.aliyun.green20220302.models.ImageModerationResponse;
import com.aliyun.green20220302.models.ImageModerationResponseBody;
import com.aliyun.green20220302.models.ImageModerationResponseBody.ImageModerationResponseBodyData;
import com.aliyun.green20220302.models.ImageModerationResponseBody.ImageModerationResponseBodyDataResult;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ImageUrlDemo {
    /**
     * 创建请求客户端
     *
     * @param accessKeyId
     * @param accessKeySecret
     * @param endpoint
     * @return
     * @throws Exception
     */
    public static Client createClient(String accessKeyId, String accessKeySecret, String endpoint) throws Exception {
        Config config = new Config();
        config.setAccessKeyId(accessKeyId);
        config.setAccessKeySecret(accessKeySecret);
        // 设置http代理。
        //config.setHttpProxy("http://10.10.xx.xx:xxxx");
        // 设置https代理。
        //config.setHttpsProxy("https://10.10.xx.xx:xxxx");
        // 接入区域和地址请根据实际情况修改
      	// 接入地址列表：https://help.aliyun.com/document_detail/467828.html?#section-uib-qkw-0c8
        config.setEndpoint(endpoint);
        return new Client(config);
    }

    public static ImageModerationResponse invokeFunction(String accessKeyId, String accessKeySecret, String endpoint) throws Exception {
        //注意，此处实例化的client请尽可能重复使用，避免重复建立连接，提升检测性能。
        Client client = createClient(accessKeyId, accessKeySecret, endpoint);

        // 创建RuntimeObject实例并设置运行参数
        RuntimeOptions runtime = new RuntimeOptions();

        // 检测参数构造。
        Map<String, String> serviceParameters = new HashMap<>();
        //公网可访问的URL。
        serviceParameters.put("imageUrl", "https://img.alicdn.com/tfs/xxxxxxxxxx001.png");
        //待检测数据唯一标识
        serviceParameters.put("dataId", UUID.randomUUID().toString());

        ImageModerationRequest request = new ImageModerationRequest();
        // 图片检测service：内容安全控制台图片增强版规则配置的serviceCode，示例：baselineCheck
      	// 支持service请参考：https://help.aliyun.com/document_detail/467826.html?0#p-23b-o19-gff
        request.setService("baselineCheck");
        request.setServiceParameters(JSON.toJSONString(serviceParameters));

        ImageModerationResponse response = null;
        try {
            response = client.imageModerationWithOptions(request, runtime);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    public static void main(String[] args) throws Exception {
        /**
         * 阿里云账号AccessKey拥有所有API的访问权限，建议您使用RAM用户进行API访问或日常运维。
         * 常见获取环境变量方式：
         * 方式一：
         *     获取RAM用户AccessKey ID：System.getenv("ALIBABA_CLOUD_ACCESS_KEY_ID");
         *     获取RAM用户AccessKey Secret：System.getenv("ALIBABA_CLOUD_ACCESS_KEY_SECRET");
         * 方式二：
         *     获取RAM用户AccessKey ID：System.getProperty("ALIBABA_CLOUD_ACCESS_KEY_ID");
         *     获取RAM用户AccessKey Secret：System.getProperty("ALIBABA_CLOUD_ACCESS_KEY_SECRET");
         */
        String accessKeyId = "建议从环境变量中获取RAM用户AccessKey ID";
        String accessKeySecret = "建议从环境变量中获取RAM用户AccessKey Secret";
        // 接入区域和地址请根据实际情况修改。
        ImageModerationResponse response = invokeFunction(accessKeyId, accessKeySecret, "green-cip.cn-shanghai.aliyuncs.com");
        try {
            // 自动路由。
            if (response != null) {
                //区域切换到cn-beijing。
                if (500 == response.getStatusCode() || (response.getBody() != null && 500 == (response.getBody().getCode()))) {
                    // 接入区域和地址请根据实际情况修改。
                    response = invokeFunction(accessKeyId, accessKeySecret, "green-cip.cn-beijing.aliyuncs.com");
                }
            }
            // 打印检测结果。
            if (response != null) {
                if (response.getStatusCode() == 200) {
                    ImageModerationResponseBody body = response.getBody();
                    System.out.println("requestId=" + body.getRequestId());
                    System.out.println("code=" + body.getCode());
                    System.out.println("msg=" + body.getMsg());
                    if (body.getCode() == 200) {
                        ImageModerationResponseBodyData data = body.getData();
                        System.out.println("dataId=" + data.getDataId());
                        List<ImageModerationResponseBodyDataResult> results = data.getResult();
                        for (ImageModerationResponseBodyDataResult result : results) {
                            System.out.println("label=" + result.getLabel());
                            System.out.println("confidence=" + result.getConfidence());
                        }
                    } else {
                        System.out.println("image moderation not success. code:" + body.getCode());
                    }
                } else {
                    System.out.println("response not success. status:" + response.getStatusCode());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}