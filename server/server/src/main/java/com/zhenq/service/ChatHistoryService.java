package com.zhenq.service;

import com.mybatisflex.core.service.IService;
import com.zhenq.model.dto.visual.VisualEditContext;
import com.zhenq.model.entity.App;
import com.zhenq.model.entity.ChatHistory;
import com.zhenq.model.entity.User;
import com.zhenq.model.vo.ChatHistoryCursorPageVO;
import com.zhenq.model.vo.ChatHistoryVO;

import java.util.List;

/**
 * 对话历史服务
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 保存用户消息
     */
    Long saveUserMessage(Long appId, Long userId, String content);

    /**
     * 保存 AI 回复
     */
    Long saveAiMessage(Long appId, String content);

    /**
     * 保存错误信息
     */
    Long saveErrorMessage(Long appId, String content);

    /**
     * 游标分页查询（最新优先加载，返回正序列表）
     */
    ChatHistoryCursorPageVO listHistoryByCursor(Long appId, Long lastId, int pageSize);

    /**
     * 删除应用下所有对话历史（逻辑删除）
     */
    boolean deleteByAppId(Long appId);

    /**
     * 校验是否有权查看该应用对话历史（创建者或管理员）
     */
    void checkChatHistoryViewAuth(App app, User loginUser);

    /**
     * 构建带对话记忆的完整 prompt（供 AI 增量改进）
     */
    String buildPromptWithMemory(Long appId, String currentMessage);

    /**
     * 构建带对话记忆与可视化编辑上下文的 prompt
     */
    String buildPromptWithMemory(Long appId, String currentMessage, VisualEditContext visualContext);

    ChatHistoryVO getChatHistoryVO(ChatHistory chatHistory);

    List<ChatHistoryVO> getChatHistoryVOList(List<ChatHistory> list);
}
