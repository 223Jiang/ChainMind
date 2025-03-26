package com.tencent.supersonic.chat.server.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.supersonic.chat.server.client.response.SynthesisResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author JiangWeiWei
 */
@Slf4j
@Component
public class BaiduVoiceRecognitionClient {

    @Value("${baidu.voice.apiKey}")
    private String apiKey;

    @Value("${baidu.voice.secretKey}")
    private String secretKey;

    @Value("${baidu.voice.cuid}")
    private String cuid;

    /**
     * 创建一个OkHttpClient实例，用于执行HTTP请求 设置连接超时时间为30秒，以避免长时间运行的网络连接导致的应用卡顿
     * 设置读取超时时间为30秒，确保服务器在指定时间内响应，防止应用因等待响应而卡顿 设置写入超时时间为30秒，确保请求数据在指定时间内发送完成，避免因网络问题导致的请求失败
     */
    private final OkHttpClient httpClient =
            new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS).build();

    /**
     * 语音识别
     *
     * @param audioFile 语音文件（支持PCM格式）
     * @param format 语音文件的格式（pcm/wav/amr/m4a）
     * @param rate 采样率（16000、8000）
     * @return 识别结果
     * @throws IOException IO异常
     * @throws JSONException JSON解析异常
     */
    public String recognize(File audioFile, String format, Integer rate)
            throws IOException, JSONException {
        if (audioFile == null || !audioFile.exists() || audioFile.isDirectory()) {
            throw new IllegalArgumentException("音频文件无效");
        }
        if (!isValidFormat(format)) {
            throw new IllegalArgumentException("不支持的音频格式：" + format);
        }
        if (rate != 16000 && rate != 8000) {
            throw new IllegalArgumentException("不支持的采样率：" + rate);
        }

        try {
            byte[] audioData = Files.readAllBytes(audioFile.toPath());
            return recognize(audioData, format, rate);
        } catch (IOException e) {
            log.error("Error reading audio file", e);
            throw e;
        }
    }

    /**
     * 语音识别
     *
     * @param audioData 语音二进制数据
     * @param format 语音文件的格式（pcm/wav/amr/m4a）
     * @param rate 采样率（16000、8000）
     * @return 识别结果
     * @throws IOException IO异常
     * @throws JSONException JSON解析异常
     */
    public String recognize(byte[] audioData, String format, Integer rate)
            throws IOException, JSONException {
        if (audioData == null || audioData.length == 0) {
            throw new IllegalArgumentException("音频数据为空");
        }
        if (!isRecognizeFormat(format)) {
            throw new IllegalArgumentException("不支持的音频格式：" + format);
        }
        if (rate != 16000 && rate != 8000) {
            throw new IllegalArgumentException("不支持的采样率：" + rate);
        }

        String accessToken = getAccessToken();
        String base64Audio = Base64.getEncoder().encodeToString(audioData);

        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, recognizeBuildRequestBody(format, rate,
                base64Audio, audioData.length, accessToken));

        Request request = new Request.Builder().url("https://vop.baidu.com/server_api").post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json").build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("意外代码：{}", response.code());
                throw new IOException("意外代码：" + response.code());
            }

            JSONObject jsonResponse = new JSONObject(response.body().string());
            JSONArray jsonArray = jsonResponse.getJSONArray("result");

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < jsonArray.length(); i++) {
                sb.append(jsonArray.getString(i));
                if (i < jsonArray.length() - 1) {
                    sb.append("。");
                }
            }

            return sb.toString();
        } catch (Exception e) {
            log.error("识别过程中的错误：", e);
            throw e;
        }
    }

    /**
     * 语音合成
     *
     * @param text 待合成的文本，需要为UTF-8编码
     * @param format 音频格式，"mp3-16k"，"mp3-48k"，"wav"，"pcm-8k"，"pcm-16k"
     * @return 合成结果
     */
    public String synthesis(String text, String format) throws JSONException, IOException {
        if (StringUtils.isEmpty(text)) {
            throw new IllegalArgumentException("待合成的文本数据为空");
        }
        if (StringUtils.isEmpty(format)) {
            throw new IllegalArgumentException("不支持合成的音频格式：" + format);
        }

        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, synthesisBuildRequestBody(text, format));

        Request request = new Request.Builder()
                .url("https://aip.baidubce.com/rpc/2.0/tts/v1/create?access_token="
                        + getAccessToken())
                .method("POST", body).addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json").build();

        String taskId;

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("意外代码：{}", response.code());
                throw new IOException("意外代码：" + response.code());
            }

            JSONObject jsonResponse = new JSONObject(response.body().string());
            // 获取到task_id，用于后续请求结果
            taskId = jsonResponse.getString("task_id");
        } catch (Exception e) {
            log.error("合成过程中的错误：", e);
            throw e;
        }

        return taskId;
    }

    /**
     * 语音合成查询
     *
     * @return 合成结果
     */
    public SynthesisResponse synthesisSearch(List<String> taskIds)
            throws JSONException, IOException {
        if (taskIds == null || taskIds.isEmpty()) {
            throw new IllegalArgumentException("语音合成查询数据为空");
        }

        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body =
                RequestBody.create(mediaType, new JSONObject().put("task_ids", taskIds).toString());

        Request request = new Request.Builder()
                .url("https://aip.baidubce.com/rpc/2.0/tts/v1/query?access_token="
                        + getAccessToken())
                .method("POST", body).addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json").build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("意外代码：{}", response.code());
                throw new IOException("意外代码：" + response.code());
            }

            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(response.body().string(), SynthesisResponse.class);
        } catch (Exception e) {
            log.error("合成过程中的错误：", e);
            throw e;
        }
    }


    private boolean isValidFormat(String format) {
        return StringUtils.isNotBlank(format) && ("pcm".equals(format) || "wav".equals(format)
                || "amr".equals(format) || "m4a".equals(format));
    }

    private boolean isRecognizeFormat(String format) {
        return StringUtils.isNotBlank(format) && ("mp3-16k".equals(format)
                || "mp3-48k".equals(format) || "wav".equals(format) || "pcm-8k".equals(format))
                || "pcm-16k".equals(format);
    }

    private String recognizeBuildRequestBody(String format, Integer rate, String base64Audio,
            int audioLength, String accessToken) throws JSONException {
        return new JSONObject().put("format", format).put("rate", rate).put("channel", 1)
                .put("cuid", cuid).put("speech", base64Audio).put("dev_pid", 1537)
                .put("len", audioLength).put("token", accessToken).toString();
    }

    private String synthesisBuildRequestBody(String text, String format) throws JSONException {
        return new JSONObject().put("format", format).put("text", text).put("lang", "zh")
                .put("voice", 1)
                // 语速，范围：0~15
                .put("speed", 5)
                // 音调，范围：0~15
                .put("pitch", 5)
                // 音量大小，范围：0~15
                .put("volume", 5).toString();
    }

    private String getAccessToken() throws IOException, JSONException {
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType, "grant_type=client_credentials&client_id="
                + apiKey + "&client_secret=" + secretKey);

        Request request = new Request.Builder().url("https://aip.baidubce.com/oauth/2.0/token")
                .post(body).addHeader("Content-Type", "application/x-www-form-urlencoded").build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("Failed to get access token: {}", response.code());
                throw new IOException("Failed to get access token: " + response.code());
            }

            return new JSONObject(response.body().string()).getString("access_token");
        } catch (Exception e) {
            log.error("Error getting access token", e);
            throw e;
        }
    }
}
