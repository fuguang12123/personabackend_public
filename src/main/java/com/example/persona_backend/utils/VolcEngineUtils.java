package com.example.persona_backend.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.ByteString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Slf4j
@Component
public class VolcEngineUtils {

    @Value("${volc.appId}")
    private String appId;

    @Value("${volc.accessToken}")
    private String accessToken;

    @Value("${volc.asr.cluster:volc_sms_status}")
    private String asrCluster;

    @Value("${volc.tts.defaultVoice:saturn_zh_female_cancan_tob}")
    private String defaultVoiceType;

    // ASR V2 接口
    private static final String ASR_URL = "wss://openspeech.bytedance.com/api/v2/asr";
    // TTS V3 接口
    private static final String TTS_WS_URL = "wss://openspeech.bytedance.com/api/v3/tts/unidirectional/stream";
    private static final String TTS_RESOURCE_ID = "seed-tts-2.0";

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    // ==========================================================================
    //  Part 1: 语音识别 (ASR) - 一句话识别 (基于 WebSocket V2)
    // ==========================================================================

    /**
     * 语音转文字 (同步阻塞)
     * @param audioData 音频文件二进制数据
     * @param format 音频格式 (mp3, wav, m4a, pcm)
     * @return 识别出的文本，失败返回错误信息
     */
    public String recognizeAudio(byte[] audioData, String format) {
        final String reqId = UUID.randomUUID().toString();
        final CountDownLatch latch = new CountDownLatch(1);
        final StringBuilder resultText = new StringBuilder();
        final StringBuffer errorMsg = new StringBuffer();

        Request request = new Request.Builder()
                .url(ASR_URL)
                .header("Authorization", "Bearer; " + accessToken)
                .build();

        WebSocketListener listener = new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                log.info("✅ [ASR] 连接成功, ReqID: {}", reqId);
                try {
                    // 1. 发送初始化参数
                    byte[] paramPayload = constructAsrParam(reqId, format);
                    webSocket.send(ByteString.of(paramPayload));

                    // 2. 发送音频数据 (分片发送，模拟流式，防止服务端缓冲溢出)
                    int chunkSize = 16 * 1024; // 16KB per chunk
                    int offset = 0;
                    while (offset < audioData.length) {
                        int end = Math.min(offset + chunkSize, audioData.length);
                        byte[] chunk = Arrays.copyOfRange(audioData, offset, end);
                        boolean isLast = (end == audioData.length);

                        byte[] audioPayload = constructAsrAudioPayload(chunk, isLast);
                        webSocket.send(ByteString.of(audioPayload));

                        offset = end;
                    }
                    log.info("📤 [ASR] 音频发送完毕 ({} bytes)，等待识别结果...", audioData.length);

                } catch (Exception e) {
                    log.error("❌ [ASR] 发送数据异常", e);
                    webSocket.close(1000, "Send Error");
                    latch.countDown();
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                try {
                    AsrResponse response = parseAsrResponse(bytes.toByteArray());
                    if (response != null) {
                        if (response.getCode() != 1000) {
                            log.error("❌ [ASR] 服务端错误: Code={}, Msg={}", response.getCode(), response.getMessage());
                            errorMsg.append("API错误: ").append(response.getMessage());
                            webSocket.close(1000, "Error");
                            latch.countDown();
                            return;
                        }
                        // sequence < 0 表示最终结果
                        if (response.getSequence() < 0) {
                            if (response.getResult() != null && response.getResult().length > 0) {
                                resultText.append(response.getResult()[0].getText());
                            }
                            log.info("✅ [ASR] 最终结果: {}", resultText);
                            webSocket.close(1000, "Finished");
                            latch.countDown();
                        }
                    }
                } catch (Exception e) {
                    log.error("❌ [ASR] 解析响应失败", e);
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                log.error("❌ [ASR] 连接失败", t);
                errorMsg.append(t.getMessage());
                latch.countDown();
            }
        };

        client.newWebSocket(request, listener);

        try {
            boolean finished = latch.await(20, TimeUnit.SECONDS);
            if (!finished) return "识别超时";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (errorMsg.length() > 0) return "识别失败: " + errorMsg;
        return resultText.length() > 0 ? resultText.toString() : "未识别出内容";
    }

    // --- ASR 辅助方法 ---

    private byte[] constructAsrParam(String reqId, String format) throws IOException {
        AsrParams params = new AsrParams();
        params.setApp(new AsrParams.App(appId, asrCluster, accessToken));
        params.setUser(new AsrParams.User("user_001"));
        params.setRequest(new AsrParams.Request(reqId, "audio_in,resample,partition,vad,fe,decode,itn,nlu_punct", 1, true, "full", 1));
        params.setAudio(new AsrParams.Audio(format, "raw", 24000, 16, 1)); // 默认24k采样率

        byte[] jsonBytes = JSON.toJSONBytes(params);
        byte[] compressed = gzipCompress(jsonBytes);
        // Header: 0x11 0x10 0x11 0x00
        byte[] header = new byte[] { 0x11, 0x10, 0x11, 0x00 };
        return concatBytes(header, intToBytes(compressed.length), compressed);
    }

    private byte[] constructAsrAudioPayload(byte[] audio, boolean isLast) throws IOException {
        byte[] compressed = gzipCompress(audio);
        // Byte 1: 0x22 (Last) or 0x20 (Not Last)
        byte byte1 = isLast ? (byte) 0x22 : (byte) 0x20;
        byte[] header = new byte[] { 0x11, byte1, 0x11, 0x00 };
        return concatBytes(header, intToBytes(compressed.length), compressed);
    }

    private AsrResponse parseAsrResponse(byte[] data) throws IOException {
        if (data.length < 4) return null;
        int headerLen = (data[0] & 0x0f) << 2;
        int msgType = (data[1] & 0xf0) >> 4;
        int compressType = data[2] & 0x0f;
        int offset = headerLen;

        if (msgType == 0b1001) { // Full Response
            if (data.length < offset + 4) return null;
            int payloadSize = ByteBuffer.wrap(data, offset, 4).getInt();
            offset += 4;
            byte[] payload = Arrays.copyOfRange(data, offset, offset + payloadSize);
            if (compressType == 1) payload = gzipDecompress(payload);
            return JSON.parseObject(new String(payload, StandardCharsets.UTF_8), AsrResponse.class);
        }
        return null;
    }

    // ==========================================================================
    //  Part 2: 语音合成 (TTS) - 豆包大模型 2.0 (基于 WebSocket V3)
    // ==========================================================================

    public byte[] synthesizeSpeech(String text, String instruction) {
        log.info("🔊 [TTS 2.0] 开始合成: \"{}\", 指令: {}, 音色: {}", text, instruction, defaultVoiceType);

        Request request = new Request.Builder()
                .url(TTS_WS_URL)
                .header("X-Api-App-Id", appId)
                .header("X-Api-Access-Key", accessToken)
                .header("X-Api-Resource-Id", TTS_RESOURCE_ID)
                .header("X-Api-Request-Id", UUID.randomUUID().toString())
                .build();

        final ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] isSuccess = {false};

        WebSocket ws = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                log.info("✅ [TTS 2.0] 连接成功");
                try {
                    sendTtsStartParams(webSocket, text, instruction);
                } catch (Exception e) {
                    log.error("发送TTS参数失败", e);
                    webSocket.close(1000, "Send Error");
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                try {
                    VolcProtocol.Message msg = VolcProtocol.Message.unmarshal(bytes.toByteArray());
                    if (msg.getType() == VolcProtocol.MsgType.AUDIO_ONLY_SERVER) {
                        if (msg.getPayload() != null) audioBuffer.write(msg.getPayload());
                    } else if (msg.getType() == VolcProtocol.MsgType.ERROR) {
                        String err = msg.getPayload() != null ? new String(msg.getPayload()) : "Unknown";
                        log.error("❌ [TTS 2.0] 错误: Code={}, Msg={}", msg.getErrorCode(), err);
                        webSocket.close(1000, "Error");
                        latch.countDown();
                    } else if (msg.getEvent() == VolcProtocol.EventType.SESSION_FINISHED) {
                        log.info("✅ [TTS 2.0] 合成结束");
                        isSuccess[0] = true;
                        sendTtsFinish(webSocket);
                        webSocket.close(1000, "Finished");
                        latch.countDown();
                    }
                } catch (Exception e) {
                    webSocket.close(1000, "Parse Error");
                    latch.countDown();
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                log.error("❌ [TTS 2.0] 连接异常", t);
                latch.countDown();
            }
        });

        try {
            if (!latch.await(15, TimeUnit.SECONDS)) ws.cancel();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return (isSuccess[0] && audioBuffer.size() > 0) ? audioBuffer.toByteArray() : null;
    }

    private void sendTtsStartParams(WebSocket webSocket, String text, String instruction) throws IOException {
        JSONObject payload = new JSONObject();
        payload.put("user", new JSONObject().fluentPut("uid", "user_001"));

        JSONObject reqParams = new JSONObject();
        reqParams.put("text", text);
        reqParams.put("speaker", defaultVoiceType);
        reqParams.put("audio_params", new JSONObject().fluentPut("format", "mp3").fluentPut("sample_rate", 24000));

        // 情感/指令映射
        if (instruction != null && !instruction.isEmpty() && !"neutral".equalsIgnoreCase(instruction)) {
            JSONObject additions = new JSONObject();
            JSONArray contextTexts = new JSONArray();
            contextTexts.add(mapInstructionToContext(instruction));
            additions.put("context_texts", contextTexts);
            reqParams.put("additions", additions.toString());
        }
        payload.put("req_params", reqParams);

        VolcProtocol.Message message = new VolcProtocol.Message(VolcProtocol.MsgType.FULL_CLIENT_REQUEST, VolcProtocol.MsgTypeFlagBits.NO_SEQ);
        message.setPayload(payload.toString().getBytes(StandardCharsets.UTF_8));
        webSocket.send(ByteString.of(message.marshal()));
    }

    private void sendTtsFinish(WebSocket webSocket) throws IOException {
        VolcProtocol.Message message = new VolcProtocol.Message(VolcProtocol.MsgType.FULL_CLIENT_REQUEST, VolcProtocol.MsgTypeFlagBits.WITH_EVENT);
        message.setEvent(VolcProtocol.EventType.FINISH_CONNECTION);
        webSocket.send(ByteString.of(message.marshal()));
    }

    private String mapInstructionToContext(String input) {
        switch (input.toLowerCase()) {
            case "happy": return "请用开心的语气";
            case "sad": return "请用悲伤的语气";
            case "angry": return "请用生气的语气";
            case "excited": return "请用激动的语气";
            default: return "请用" + input + "的语气";
        }
    }

    // ================= 通用工具方法 =================

    private byte[] gzipCompress(byte[] data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) { gzip.write(data); }
        return out.toByteArray();
    }

    private byte[] gzipDecompress(byte[] data) throws IOException {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(data));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzip.read(buffer)) > 0) out.write(buffer, 0, len);
            return out.toByteArray();
        }
    }

    private byte[] intToBytes(int i) {
        return ByteBuffer.allocate(4).putInt(i).array();
    }

    private byte[] concatBytes(byte[]... arrays) {
        int totalLen = 0;
        for (byte[] arr : arrays) totalLen += arr.length;
        byte[] res = new byte[totalLen];
        int pos = 0;
        for (byte[] arr : arrays) {
            System.arraycopy(arr, 0, res, pos, arr.length);
            pos += arr.length;
        }
        return res;
    }

    // ================= DTO 类 (ASR) =================
    @Data
    public static class AsrParams {
        private App app; private User user; private Request request; private Audio audio;
        @Data @lombok.AllArgsConstructor @lombok.NoArgsConstructor public static class App { String appid; String cluster; String token; }
        @Data @lombok.AllArgsConstructor @lombok.NoArgsConstructor public static class User { String uid; }
        @Data @lombok.AllArgsConstructor @lombok.NoArgsConstructor public static class Request { String reqid; String workflow; int nbest; @JSONField(name = "show_utterances") boolean showUtterances; @JSONField(name = "result_type") String resultType; int sequence; }
        @Data @lombok.AllArgsConstructor @lombok.NoArgsConstructor public static class Audio { String format; String codec; int rate; int bits; int channels; }
    }
    @Data
    public static class AsrResponse {
        private int code; private String message; private int sequence; private ResultRes[] result;
        @Data public static class ResultRes { private String text; }
    }
}