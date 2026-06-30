package com.zhenq.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.zhenq.annotation.AuthCheck;
import com.zhenq.common.BaseResponse;
import com.zhenq.common.DeleteRequest;
import com.zhenq.common.ErrorCode;
import com.zhenq.common.ResultUtils;
import com.zhenq.constant.AppConstant;
import com.zhenq.constant.UserConstant;
import com.zhenq.config.AppProperties;
import com.zhenq.core.AiCodeGeneratorFacade;
import com.zhenq.core.cover.AppCoverService;
import com.zhenq.core.vue.VueProjectBuildService;
import com.zhenq.core.vue.VueProjectCodegenExecutor;
import com.zhenq.core.vue.VueProjectPathUtils;
import com.zhenq.core.vue.VueProjectPreviewUtils;
import com.zhenq.core.vue.VueProjectStreamCallback;
import com.zhenq.exception.BusinessException;
import com.zhenq.exception.ThrowUtils;
import com.zhenq.model.dto.app.AppAddRequest;
import com.zhenq.model.dto.app.AppAdminUpdateRequest;
import com.zhenq.model.dto.app.AppDeployRequest;
import com.zhenq.model.dto.app.AppQueryRequest;
import com.zhenq.model.dto.app.AppUpdateRequest;
import com.zhenq.model.dto.chat.ChatHistoryQueryRequest;
import com.zhenq.model.entity.App;
import com.zhenq.model.entity.User;
import com.zhenq.model.enums.CodeGenTypeEnum;
import com.zhenq.model.vo.AppVO;
import com.zhenq.model.vo.ChatHistoryCursorPageVO;
import com.zhenq.service.AppService;
import com.zhenq.service.ChatHistoryService;
import com.zhenq.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 应用接口
 */
@RestController
@RequestMapping("/app")
public class AppController {

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private VueProjectCodegenExecutor vueProjectCodegenExecutor;

    @Resource
    private VueProjectBuildService vueProjectBuildService;

    @Resource
    private AppProperties appProperties;

    @Resource
    private AppCoverService appCoverService;

    /**
     * 每页最大数量（用户侧分页限制）
     */
    private static final int MAX_PAGE_SIZE = 20;

    /**
     * 对话历史默认每页条数
     */
    private static final int DEFAULT_CHAT_PAGE_SIZE = 10;

    // region 用户功能

    /**
     * 创建应用
     */
    @PostMapping("/add")
    public BaseResponse<Long> addApp(@RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR);
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "初始化 prompt 不能为空");
        User loginUser = userService.getLoginUser(request);
        App app = new App();
        app.setInitPrompt(initPrompt);
        // 应用名称默认取 prompt 前 12 个字符
        app.setAppName(StrUtil.maxLength(initPrompt, 12));
        app.setUserId(loginUser.getId());
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(appAddRequest.getCodeGenType());
        app.setCodeGenType(codeGenTypeEnum != null ? codeGenTypeEnum.getValue() : CodeGenTypeEnum.HTML.getValue());
        app.setPriority(AppConstant.DEFAULT_APP_PRIORITY);
        app.setEditTime(LocalDateTime.now());
        boolean result = appService.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(app.getId());
    }

    /**
     * 更新自己的应用（仅可改名称）
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateApp(@RequestBody AppUpdateRequest appUpdateRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appUpdateRequest == null || appUpdateRequest.getId() == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        App oldApp = appService.getById(appUpdateRequest.getId());
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人可修改
        if (!oldApp.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        App app = new App();
        app.setId(appUpdateRequest.getId());
        app.setAppName(appUpdateRequest.getAppName());
        app.setEditTime(LocalDateTime.now());
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 删除自己的应用
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        App oldApp = appService.getById(deleteRequest.getId());
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        // 本人或管理员可删除
        if (!oldApp.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = appService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 查看应用详情
     */
    @GetMapping("/get/vo")
    public BaseResponse<AppVO> getAppVOById(@RequestParam("id") Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(appService.getAppVO(app));
    }

    /**
     * 分页查询自己的应用列表（每页最多 20 个）
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<AppVO>> listMyAppVOByPage(@RequestBody AppQueryRequest appQueryRequest,
                                                       HttpServletRequest request) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        int pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > MAX_PAGE_SIZE, ErrorCode.PARAMS_ERROR, "每页最多查询 20 个");
        User loginUser = userService.getLoginUser(request);
        // 只查自己的应用
        appQueryRequest.setUserId(loginUser.getId());
        int current = appQueryRequest.getCurrent();
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        Page<App> appPage = appService.page(new Page<>(current, pageSize), queryWrapper);
        Page<AppVO> appVOPage = new Page<>(current, pageSize, appPage.getTotalRow());
        appVOPage.setRecords(appService.getAppVOList(appPage.getRecords()));
        return ResultUtils.success(appVOPage);
    }

    /**
     * 分页查询精选应用列表（每页最多 20 个）
     */
    @PostMapping("/good/list/page/vo")
    public BaseResponse<Page<AppVO>> listGoodAppVOByPage(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        int pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > MAX_PAGE_SIZE, ErrorCode.PARAMS_ERROR, "每页最多查询 20 个");
        // 只查精选应用
        appQueryRequest.setPriority(AppConstant.GOOD_APP_PRIORITY);
        int current = appQueryRequest.getCurrent();
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        Page<App> appPage = appService.page(new Page<>(current, pageSize), queryWrapper);
        Page<AppVO> appVOPage = new Page<>(current, pageSize, appPage.getTotalRow());
        appVOPage.setRecords(appService.getAppVOList(appPage.getRecords()));
        return ResultUtils.success(appVOPage);
    }

    /**
     * 与 AI 对话生成应用代码（SSE 流式，按应用维度落盘）
     *
     * @param appId   应用 id
     * @param message 用户消息（首次为应用初始 prompt）
     */
    @GetMapping(value = "/chat/gen/code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatToGenCode(@RequestParam Long appId,
                                    @RequestParam String message,
                                    HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 id 错误");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "提示词不能为空");
        User loginUser = userService.getLoginUser(request);
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        CodeGenTypeEnum typeEnum = CodeGenTypeEnum.getEnumByValue(app.getCodeGenType());
        if (typeEnum == null) {
            typeEnum = CodeGenTypeEnum.HTML;
        }

        // 构建带记忆的 prompt，再保存用户消息（避免当前消息重复注入）
        String promptWithMemory = chatHistoryService.buildPromptWithMemory(appId, message);
        chatHistoryService.saveUserMessage(appId, loginUser.getId(), message);

        if (CodeGenTypeEnum.VUE_PROJECT == typeEnum) {
            return chatToGenVueProject(appId, promptWithMemory);
        }
        return chatToGenNativeCode(appId, promptWithMemory, typeEnum);
    }

    /**
     * 原生 HTML / 多文件模式 SSE 生成
     */
    private SseEmitter chatToGenNativeCode(Long appId, String promptWithMemory, CodeGenTypeEnum typeEnum) {
        // 使用 SseEmitter：客户端断开不会取消 DeepSeek 流，避免 closed 错误
        SseEmitter emitter = new SseEmitter(600_000L);
        StringBuilder aiContentBuilder = new StringBuilder();
        Disposable disposable = aiCodeGeneratorFacade.generateAndSaveCodeStream(promptWithMemory, typeEnum, appId)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        chunk -> {
                            aiContentBuilder.append(chunk);
                            try {
                                emitter.send(SseEmitter.event().data(chunk));
                            } catch (IOException e) {
                                // 客户端已断开，继续读取 AI 流并落盘
                            }
                        },
                        error -> {
                            chatHistoryService.saveErrorMessage(appId, "生成失败：" + error.getMessage());
                            try {
                                emitter.send(SseEmitter.event().name("gen-error")
                                        .data("生成失败：" + error.getMessage()));
                                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                                emitter.complete();
                            } catch (IOException ignored) {
                                emitter.complete();
                            }
                        },
                        () -> {
                            if (!aiContentBuilder.isEmpty()) {
                                chatHistoryService.saveAiMessage(appId, aiContentBuilder.toString());
                            }
                            try {
                                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                                emitter.complete();
                            } catch (IOException ignored) {
                                emitter.complete();
                            }
                            // 不阻塞 SSE：后台截图并回写 app.cover
                            appCoverService.generateCoverAsync(appId);
                        }
                );
        emitter.onTimeout(disposable::dispose);
        emitter.onCompletion(disposable::dispose);
        return emitter;
    }

    /**
     * Vue3 工程模式：工具调用 Agent + npm build
     */
    private SseEmitter chatToGenVueProject(Long appId, String promptWithMemory) {
        SseEmitter emitter = new SseEmitter(600_000L);
        StringBuilder aiContentBuilder = new StringBuilder();
        Schedulers.boundedElastic().schedule(() -> vueProjectCodegenExecutor.executeStream(appId, promptWithMemory,
                new VueProjectStreamCallback() {
                    @Override
                    public void onPartialResponse(String partial) {
                        aiContentBuilder.append(partial);
                        sendSseData(emitter, partial);
                    }

                    @Override
                    public void onToolExecuted(String detail) {
                        sendSseEvent(emitter, "tool-start", detail);
                    }

                    @Override
                    public void onBuildLog(String line) {
                        sendSseEvent(emitter, "build-log", line);
                    }

                    @Override
                    public void onComplete(String fullText) {
                        chatHistoryService.saveAiMessage(appId, buildVueAiSummary(aiContentBuilder, fullText));
                        sendSseEvent(emitter, "preview-ready", "ok");
                        appCoverService.generateCoverAsync(appId, coverUrl -> {
                            if (StrUtil.isNotBlank(coverUrl)) {
                                sendSseEvent(emitter, "cover-ready", coverUrl);
                            }
                            sendSseEvent(emitter, "done", "[DONE]");
                            emitter.complete();
                        });
                    }

                    @Override
                    public void onError(String message) {
                        chatHistoryService.saveErrorMessage(appId, message);
                        sendSseEvent(emitter, "gen-error", message);
                        emitter.complete();
                    }
                }));
        return emitter;
    }

    private String buildVueAiSummary(StringBuilder aiContentBuilder, String fullText) {
        if (aiContentBuilder.length() > 500) {
            return aiContentBuilder.substring(0, 500) + "...（Vue 工程已生成并完成构建）";
        }
        return StrUtil.isNotBlank(fullText) ? fullText : "Vue 工程已生成并完成构建";
    }

    private void sendSseData(SseEmitter emitter, String data) {
        try {
            emitter.send(SseEmitter.event().data(data));
        } catch (IOException ignored) {
            // 客户端已断开
        }
    }

    private void sendSseEvent(SseEmitter emitter, String eventName, String data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException ignored) {
            // 客户端已断开
        }
    }

    /**
     * 游标分页查询应用对话历史（创建者或管理员可见）
     */
    @GetMapping("/chat/history")
    public BaseResponse<ChatHistoryCursorPageVO> listChatHistory(ChatHistoryQueryRequest queryRequest,
                                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(queryRequest == null || queryRequest.getAppId() == null || queryRequest.getAppId() <= 0,
                ErrorCode.PARAMS_ERROR, "应用 id 错误");
        User loginUser = userService.getLoginUser(request);
        App app = appService.getById(queryRequest.getAppId());
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        chatHistoryService.checkChatHistoryViewAuth(app, loginUser);
        int pageSize = queryRequest.getPageSize() == null ? DEFAULT_CHAT_PAGE_SIZE : queryRequest.getPageSize();
        ChatHistoryCursorPageVO pageVO = chatHistoryService.listHistoryByCursor(
                queryRequest.getAppId(), queryRequest.getLastId(), pageSize);
        return ResultUtils.success(pageVO);
    }

    /**
     * 手动生成应用封面（Selenium 截图预览页）
     */
    @PostMapping("/cover/generate")
    public BaseResponse<String> generateAppCover(@RequestParam Long appId, HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 id 错误");
        User loginUser = userService.getLoginUser(request);
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        if (!app.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        String coverUrl = appCoverService.generateCover(appId);
        ThrowUtils.throwIf(StrUtil.isBlank(coverUrl), ErrorCode.OPERATION_ERROR,
                "封面生成失败，请确认预览页可访问且本机已安装 Chrome");
        return ResultUtils.success(coverUrl);
    }

    /**
     * 检查应用预览是否就绪（Vue 工程需 dist/index.html 存在）
     */
    @GetMapping("/preview/ready")
    public BaseResponse<Boolean> isPreviewReady(@RequestParam Long appId, HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 id 错误");
        User loginUser = userService.getLoginUser(request);
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        chatHistoryService.checkChatHistoryViewAuth(app, loginUser);
        String codeGenType = StrUtil.isNotBlank(app.getCodeGenType())
                ? app.getCodeGenType() : CodeGenTypeEnum.HTML.getValue();
        return ResultUtils.success(VueProjectPreviewUtils.isPreviewReady(codeGenType, appId));
    }

    /**
     * 部署应用：将生成的代码复制到部署目录并返回可访问地址
     */
    @PostMapping("/deploy")
    public BaseResponse<String> deployApp(@RequestBody AppDeployRequest appDeployRequest,
                                          HttpServletRequest request) {
        ThrowUtils.throwIf(appDeployRequest == null || appDeployRequest.getAppId() == null
                || appDeployRequest.getAppId() <= 0, ErrorCode.PARAMS_ERROR, "应用 id 错误");
        Long appId = appDeployRequest.getAppId();
        User loginUser = userService.getLoginUser(request);
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人可部署
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 部署标识：复用已有，否则生成 6 位随机串
        String deployKey = app.getDeployKey();
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(6);
        }
        // 源代码目录：类型_appId
        String codeGenType = StrUtil.isNotBlank(app.getCodeGenType())
                ? app.getCodeGenType() : CodeGenTypeEnum.HTML.getValue();
        File sourceDir;
        if (CodeGenTypeEnum.VUE_PROJECT.getValue().equals(codeGenType)) {
            File distDir = VueProjectPathUtils.getDistDir(appId);
            if (!distDir.exists() || !new File(distDir, "index.html").exists()) {
                try {
                    vueProjectBuildService.build(appId, null);
                } catch (Exception e) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "构建 Vue 工程失败：" + e.getMessage());
                }
            }
            sourceDir = distDir;
        } else {
            String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + codeGenType + "_" + appId;
            sourceDir = new File(sourceDirPath);
        }
        ThrowUtils.throwIf(!sourceDir.exists() || !sourceDir.isDirectory(),
                ErrorCode.OPERATION_ERROR, "请先生成应用代码后再部署");
        // 复制到部署目录
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try {
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "部署失败：" + e.getMessage());
        }
        // 更新应用部署信息
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean result = appService.updateById(updateApp);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "更新部署信息失败");
        // 返回可访问地址
        String deployUrl = String.format("%s/%s/", appProperties.getDeployHost(), deployKey);
        return ResultUtils.success(deployUrl);
    }

    // endregion

    // region 管理员功能

    /**
     * 删除任意应用（管理员）
     */
    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteAppByAdmin(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR);
        App oldApp = appService.getById(deleteRequest.getId());
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = appService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    /**
     * 更新任意应用（管理员，可改名称/封面/优先级）
     */
    @PostMapping("/admin/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateAppByAdmin(@RequestBody AppAdminUpdateRequest appAdminUpdateRequest) {
        ThrowUtils.throwIf(appAdminUpdateRequest == null || appAdminUpdateRequest.getId() == null,
                ErrorCode.PARAMS_ERROR);
        App oldApp = appService.getById(appAdminUpdateRequest.getId());
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        App app = new App();
        BeanUtils.copyProperties(appAdminUpdateRequest, app);
        app.setEditTime(LocalDateTime.now());
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 分页查询应用列表（管理员，每页数量不限）
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AppVO>> listAppVOByPageByAdmin(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        int current = appQueryRequest.getCurrent();
        int pageSize = appQueryRequest.getPageSize();
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        Page<App> appPage = appService.page(new Page<>(current, pageSize), queryWrapper);
        Page<AppVO> appVOPage = new Page<>(current, pageSize, appPage.getTotalRow());
        appVOPage.setRecords(appService.getAppVOList(appPage.getRecords()));
        return ResultUtils.success(appVOPage);
    }

    /**
     * 根据 id 查看应用详情（管理员）
     */
    @GetMapping("/admin/get/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AppVO> getAppVOByIdByAdmin(@RequestParam("id") Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(appService.getAppVO(app));
    }

    // endregion
}
