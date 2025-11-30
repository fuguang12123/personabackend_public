package com.example.persona_backend;

import com.example.persona_backend.utils.VolcEngineUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;

@SpringBootTest
public class VolcEngineTest {

    @Autowired
    private VolcEngineUtils volcEngineUtils;



    // ⚠️ 测试 1：测试 TTS 2.0 (带情感/指令控制)
    @Test
    public void testTTS2_0() throws Exception {
        String text = "你好，我是你的数字分身，很高兴见到你！这真是太棒了！";
        String instruction = "激动！！！";

        System.out.println("--- 开始测试 TTS 2.0 ---");
        byte[] audioBytes = volcEngineUtils.synthesizeSpeech(text, instruction);

        if (audioBytes != null) {
            File outputFile = new File("test_tts_2.0.mp3");
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                fos.write(audioBytes);
            }
            System.out.println("✅ TTS 2.0 测试成功！音频已保存: " + outputFile.getAbsolutePath());
        } else {
            System.err.println("❌ TTS 2.0 测试失败");
        }
    }

    // ⚠️ 测试 3：使用 TTS 生成的音频进行 ASR 识别
    @Test
    public void testASR_OneSentence() throws Exception {
        String filePath = "test_tts_2.0.mp3";
        File audioFile = new File(filePath);

        if (!audioFile.exists()) {
            System.err.println("❌ 文件不存在: " + filePath + "，请先运行 testTTS2_0 生成音频。");
            return;
        }

        System.out.println("--- 开始测试 ASR (一句话识别) ---");
        System.out.println("读取文件: " + audioFile.getAbsolutePath());

        byte[] audioData = Files.readAllBytes(audioFile.toPath());

        // 传入 "mp3" 格式，因为 TTS 生成的是 mp3
        String resultText = volcEngineUtils.recognizeAudio(audioData, "mp3");

        System.out.println("📝 识别结果: " + resultText);

        if (resultText != null && resultText.contains("数字分身")) {
            System.out.println("✅ ASR 测试通过！识别内容匹配。");
        } else {
            System.out.println("⚠️ ASR 识别可能不准确或为空。");
        }
    }
}