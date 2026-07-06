package com.zhenq.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.zhenq.common.ErrorCode;
import com.zhenq.exception.BusinessException;
import com.zhenq.exception.ThrowUtils;
import com.zhenq.core.visual.VisualEditPromptUtils;
import com.zhenq.mapper.ChatHistoryMapper;
import com.zhenq.model.dto.visual.VisualEditContext;
import com.zhenq.model.entity.App;
import com.zhenq.model.entity.ChatHistory;
import com.zhenq.model.entity.User;
import com.zhenq.model.enums.ChatMessageTypeEnum;
import com.zhenq.model.vo.ChatHistoryCursorPageVO;
import com.zhenq.model.vo.ChatHistoryVO;
import com.zhenq.service.ChatHistoryService;
import com.zhenq.service.UserService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 对话历史服务实现
 */
@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory>
        implements ChatHistoryService {

    /**
     * 单次加载最大条数
     */
    private static final int MAX_PAGE_SIZE = 20;

    /**
     * 注入 AI 上下文的最大历史条数
     */
    private static final int AI_MEMORY_LIMIT = 20;

    @Resource
    private UserService userService;

    @Override
    public Long saveUserMessage(Long appId, Long userId, String content) {
        return saveMessage(appId, ChatMessageTypeEnum.USER.getValue(), content, userId);
    }

    @Override
    public Long saveAiMessage(Long appId, String content) {
        return saveMessage(appId, ChatMessageTypeEnum.AI.getValue(), content, null);
    }

    @Override
    public Long saveErrorMessage(Long appId, String content) {
        return saveMessage(appId, ChatMessageTypeEnum.ERROR.getValue(), content, null);
    }

    private Long saveMessage(Long appId, String messageType, String content, Long userId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(StrUtil.isBlank(content), ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        ChatHistory chatHistory = new ChatHistory();
        chatHistory.setAppId(appId);
        chatHistory.setMessageType(messageType);
        chatHistory.setContent(content);
        chatHistory.setUserId(userId);
        boolean result = this.save(chatHistory);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "保存对话历史失败");
        return chatHistory.getId();
    }

    @Override
    public ChatHistoryCursorPageVO listHistoryByCursor(Long appId, Long lastId, int pageSize) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR);
        if (pageSize <= 0) {
            pageSize = 10;
        }
        pageSize = Math.min(pageSize, MAX_PAGE_SIZE);

        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("app_id", appId)
                .lt("id", lastId, lastId != null && lastId > 0)
                .orderBy("id desc")
                .limit(pageSize);
        List<ChatHistory> historyList = this.list(queryWrapper);
        if (ObjectUtils.isEmpty(historyList)) {
            ChatHistoryCursorPageVO empty = new ChatHistoryCursorPageVO();
            empty.setRecords(List.of());
            empty.setHasMore(false);
            empty.setNextCursor(null);
            return empty;
        }

        // 转为时间正序（旧 → 新），便于前端直接渲染
        List<ChatHistory> ordered = new ArrayList<>(historyList);
        Collections.reverse(ordered);

        Long nextCursor = ordered.get(0).getId();
        boolean hasMore = historyList.size() >= pageSize;
        if (hasMore && nextCursor != null) {
            long olderCount = this.count(QueryWrapper.create()
                    .eq("app_id", appId)
                    .lt("id", nextCursor));
            hasMore = olderCount > 0;
        }

        ChatHistoryCursorPageVO pageVO = new ChatHistoryCursorPageVO();
        pageVO.setRecords(getChatHistoryVOList(ordered));
        pageVO.setHasMore(hasMore);
        pageVO.setNextCursor(nextCursor);
        return pageVO;
    }

    @Override
    public boolean deleteByAppId(Long appId) {
        if (appId == null || appId <= 0) {
            return true;
        }
        QueryWrapper queryWrapper = QueryWrapper.create().eq("app_id", appId);
        return this.remove(queryWrapper);
    }

    @Override
    public void checkChatHistoryViewAuth(App app, User loginUser) {
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        if (!app.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }

    @Override
    public String buildPromptWithMemory(Long appId, String currentMessage) {
        return buildPromptWithMemory(appId, currentMessage, null);
    }

    @Override
    public String buildPromptWithMemory(Long appId, String currentMessage, VisualEditContext visualContext) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("app_id", appId)
                // 排除系统错误消息，避免历史错误污染记忆
                .ne("message_type", ChatMessageTypeEnum.ERROR.getValue())
                .orderBy("id desc")
                .limit(AI_MEMORY_LIMIT);
        List<ChatHistory> recentList = this.list(queryWrapper);

        StringBuilder sb = new StringBuilder();
        if (!ObjectUtils.isEmpty(recentList)) {
            Collections.reverse(recentList);
            sb.append("【对话历史】\n");
            sb.append("以下是用户与本应用的过往对话，请基于已有成果进行增量改进，不要完全重写。\n");
            for (ChatHistory history : recentList) {
                if (ChatMessageTypeEnum.USER.getValue().equals(history.getMessageType())) {
                    sb.append("用户：").append(history.getContent()).append("\n");
                } else if (ChatMessageTypeEnum.AI.getValue().equals(history.getMessageType())) {
                    String aiContent = StrUtil.maxLength(history.getContent(), 2000);
                    sb.append("AI：").append(aiContent).append("\n");
                }
                // 跳过系统错误消息：多为鉴权/连接等基础设施错误，注入会污染上下文并误导模型
            }
            sb.append("\n");
        }

        sb.append("【当前需求】\n");
        sb.append(currentMessage);
        sb.append("\n\n请基于对话历史，在已有网页基础上完成当前需求。");

        return VisualEditPromptUtils.appendVisualEditSection(sb.toString(), visualContext);
    }

    @Override
    public ChatHistoryVO getChatHistoryVO(ChatHistory chatHistory) {
        if (chatHistory == null) {
            return null;
        }
        ChatHistoryVO vo = new ChatHistoryVO();
        BeanUtils.copyProperties(chatHistory, vo);
        return vo;
    }

    @Override
    public List<ChatHistoryVO> getChatHistoryVOList(List<ChatHistory> list) {
        if (ObjectUtils.isEmpty(list)) {
            return List.of();
        }
        return list.stream().map(this::getChatHistoryVO).toList();
    }
}
