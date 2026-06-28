package com.zhenq.core.vue;

import com.zhenq.constant.AppConstant;
import com.zhenq.model.enums.CodeGenTypeEnum;

import java.io.File;

/**
 * 预览就绪检查
 */
public final class VueProjectPreviewUtils {

    private VueProjectPreviewUtils() {
    }

    public static boolean isPreviewReady(String codeGenType, Long appId) {
        if (appId == null || appId <= 0 || codeGenType == null) {
            return false;
        }
        if (CodeGenTypeEnum.VUE_PROJECT.getValue().equals(codeGenType)) {
            File distIndex = new File(VueProjectPathUtils.getDistDir(appId), "index.html");
            return distIndex.exists() && distIndex.isFile();
        }
        File nativeIndex = new File(
                AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + codeGenType + "_" + appId,
                "index.html");
        return nativeIndex.exists() && nativeIndex.isFile();
    }
}
