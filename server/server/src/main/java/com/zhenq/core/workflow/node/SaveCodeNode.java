package com.zhenq.core.workflow.node;

import com.zhenq.core.parser.CodeParser;
import com.zhenq.core.saver.CodeFileSaverExecutor;
import com.zhenq.core.workflow.CodeGenWorkflowState;
import com.zhenq.model.enums.CodeGenTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 工作流节点：解析并落盘生成的代码
 */
@Slf4j
@Component
public class SaveCodeNode implements NodeAction<CodeGenWorkflowState> {

    @Override
    public Map<String, Object> apply(CodeGenWorkflowState state) {
        CodeGenTypeEnum type = state.codeGenType();
        switch (type) {
            case HTML -> CodeFileSaverExecutor.executeSaver(
                    CodeParser.parseHtmlCode(state.generatedCode()), CodeGenTypeEnum.HTML, state.appId());
            case MULTI_FILE -> CodeFileSaverExecutor.executeSaver(
                    CodeParser.parseMultiFileCode(state.generatedCode()), CodeGenTypeEnum.MULTI_FILE, state.appId());
            default -> throw new IllegalArgumentException("不支持的落盘类型: " + type);
        }
        String qualityNote = state.qualityPassed() ? "passed" : "saved-with-issues";
        log.info("[workflow] save_code -> appId={}, quality={}", state.appId(), qualityNote);
        return Map.of(
                CodeGenWorkflowState.WORKFLOW_LOG,
                "[save_code] appId=" + state.appId() + ", quality=" + qualityNote
                        + ", retries=" + state.retryCount()
        );
    }
}
