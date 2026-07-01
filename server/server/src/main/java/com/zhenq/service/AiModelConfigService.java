package com.zhenq.service;

import com.zhenq.model.dto.aimodel.AiModelConfigUpdateRequest;
import com.zhenq.model.vo.AiModelConfigVO;
import com.zhenq.model.vo.AiModelProviderVO;

import java.util.List;

/**
 * AI 模型接入配置服务
 */
public interface AiModelConfigService {

    List<AiModelProviderVO> listProviders();

    AiModelConfigVO getAdminConfig();

    void saveAdminConfig(AiModelConfigUpdateRequest request);

    void testConnection(String providerId, String baseUrl, String apiKey, String modelName);
}
