package com.example.persona_backend;

import com.example.persona_backend.service.AiService;
import org.codehaus.jettison.json.JSONException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AiServiceTest {
    @Autowired private AiService aiService;

    @Test
    void testGenerate() throws JSONException {
        String result = aiService.generatePersonaDescription("绝地武士");
        System.out.println(">>> AI Output: " + result);
        assert result != null && result.length() > 5;
    }
}
