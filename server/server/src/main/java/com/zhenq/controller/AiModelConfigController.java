package com.zhenq.controller;

import com.zhenq.annotation.AuthCheck;
import com.zhenq.common.BaseResponse;
import com.zhenq.common.ErrorCode;
import com.zhenq.common.ResultUtils;
import com.zhenq.constant.UserConstant;
import com.zhenq.exception.ThrowUtils;
import com.zhenq.model.dto.aimodel.AiModelConfigUpdateRequest;
import com.zhenq.model.vo.AiModelConfigVO;
import com.zhenq.model.vo.AiModelProviderVO;
import com.zhenq.service.AiModelConfigService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * AI 模型接入配置（管理员）
 */
@RestController
@RequestMapping("/admin/ai-model")
public class AiModelConfigController {

    @Resource
    private AiModelConfigService aiModelConfigService;

    /**
     * 获取支持的模型提供商列表
     */
    @GetMapping("/providers")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<AiModelProviderVO>> listProviders() {
        return ResultUtils.success(aiModelConfigService.listProviders());
    }

    /**
     * 获取当前模型接入配置
     */
    @GetMapping("/config")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AiModelConfigVO> getConfig() {
        return ResultUtils.success(aiModelConfigService.getAdminConfig());
    }

    /**
     * 保存模型接入配置
     */
    @PostMapping("/config")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> saveConfig(@RequestBody AiModelConfigUpdateRequest request) {
        aiModelConfigService.saveAdminConfig(request);
        return ResultUtils.success(true);
    }

    /**
     * 测试模型连接
     */
    @PostMapping("/test")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> testConnection(@RequestBody Map<String, String> body) {
        ThrowUtils.throwIf(body == null, ErrorCode.PARAMS_ERROR);
        aiModelConfigService.testConnection(
                body.get("providerId"),
                body.get("baseUrl"),
                body.get("apiKey"),
                body.get("modelName"));
        return ResultUtils.success(true);
    }
}
