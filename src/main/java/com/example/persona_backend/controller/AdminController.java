package com.example.persona_backend.controller;

import com.example.persona_backend.common.Result;
import com.example.persona_backend.entity.Persona;
import com.example.persona_backend.entity.PersonaVector;
import com.example.persona_backend.mapper.PersonaMapper;
import com.example.persona_backend.mapper.PersonaVectorMapper;
import com.example.persona_backend.utils.ZhipuAiUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private PersonaMapper personaMapper;
    @Autowired
    private PersonaVectorMapper personaVectorMapper;
    @Autowired
    private ZhipuAiUtils zhipuAiUtils;

    /**
     * 【一次性工具】同步所有旧 Persona 的向量数据
     * 作用：让旧的智能体能被推荐算法检索到
     * 调用方式：POST http://localhost:8080/admin/sync-persona-vectors
     */
    @PostMapping("/sync-persona-vectors")
    public Result<String> syncPersonaVectors() {
        List<Persona> allPersonas = personaMapper.selectList(null);
        int successCount = 0;
        int skipCount = 0;

        for (Persona persona : allPersonas) {
            // 检查是否已有向量（避免重复消耗 token）
            if (personaVectorMapper.selectById(persona.getId()) != null) {
                skipCount++;
                continue;
            }

            // 拼接用于 Embedding 的文本 (名称 + 标签 + 描述)
            String textToEmbed = "Name: " + persona.getName() +
                    "; Tags: " + persona.getPersonalityTags() +
                    "; Desc: " + persona.getDescription();

            try {
                // 调用智谱 API
                List<Double> vector = zhipuAiUtils.generateEmbedding(textToEmbed);

                if (vector != null && !vector.isEmpty()) {
                    PersonaVector pv = new PersonaVector();
                    pv.setPersonaId(persona.getId());
                    pv.setEmbedding(vector);
                    pv.setVersion(1);
                    personaVectorMapper.insert(pv);
                    successCount++;
                }

                // 避免触发 API 速率限制 (QPS)
                Thread.sleep(200);

            } catch (Exception e) {
                log.error("Sync vector failed for persona: " + persona.getName(), e);
            }
        }

        return Result.success("同步完成: 成功生成 " + successCount + " 个, 跳过 " + skipCount + " 个");
    }
}