package com.zhenq.service.impl;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.zhenq.common.ErrorCode;
import com.zhenq.exception.ThrowUtils;
import com.zhenq.mapper.AppMapper;
import com.zhenq.model.dto.app.AppQueryRequest;
import com.zhenq.model.entity.App;
import com.zhenq.model.entity.User;
import com.zhenq.model.vo.AppVO;
import com.zhenq.model.vo.UserVO;
import com.zhenq.service.AppService;
import com.zhenq.service.ChatHistoryService;
import com.zhenq.service.UserService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用服务实现
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Resource
    private UserService userService;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Override
    public boolean removeById(Serializable id) {
        boolean removed = super.removeById(id);
        if (removed && id != null) {
            chatHistoryService.deleteByAppId(Long.parseLong(id.toString()));
        }
        return removed;
    }

    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtils.copyProperties(app, appVO);
        // 填充创建者信息
        Long userId = app.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            appVO.setUser(userService.getUserVO(user));
        }
        return appVO;
    }

    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (ObjectUtils.isEmpty(appList)) {
            return List.of();
        }
        // 批量查询创建者信息，避免 N+1
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userIds.isEmpty()
                ? Map.of()
                : userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO, (a, b) -> a));
        return appList.stream().map(app -> {
            AppVO appVO = new AppVO();
            BeanUtils.copyProperties(app, appVO);
            appVO.setUser(userVOMap.get(app.getUserId()));
            return appVO;
        }).toList();
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();

        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.eq("id", id, id != null);
        queryWrapper.like("app_name", appName, StringUtils.isNotBlank(appName));
        queryWrapper.like("cover", cover, StringUtils.isNotBlank(cover));
        queryWrapper.like("init_prompt", initPrompt, StringUtils.isNotBlank(initPrompt));
        queryWrapper.eq("code_gen_type", codeGenType, StringUtils.isNotBlank(codeGenType));
        queryWrapper.eq("deploy_key", deployKey, StringUtils.isNotBlank(deployKey));
        queryWrapper.eq("priority", priority, priority != null);
        queryWrapper.eq("user_id", userId, userId != null);
        if (StringUtils.isNotBlank(sortField)) {
            // 前端排序字段为驼峰，转换为下划线列名
            String sortColumnName = sortField.replaceAll("([A-Z])", "_$1").toLowerCase();
            QueryColumn sortColumn = new QueryColumn(sortColumnName);
            boolean isAsc = "ascend".equals(sortOrder);
            queryWrapper.orderBy(isAsc ? sortColumn.asc() : sortColumn.desc());
        }
        return queryWrapper;
    }
}
