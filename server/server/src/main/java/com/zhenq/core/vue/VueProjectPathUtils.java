package com.zhenq.core.vue;

import com.zhenq.constant.AppConstant;
import com.zhenq.model.enums.CodeGenTypeEnum;

import java.io.File;

/**
 * Vue 工程项目路径工具
 */
public final class VueProjectPathUtils {

    private VueProjectPathUtils() {
    }

    public static String getProjectDirPath(Long appId) {
        return AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator
                + CodeGenTypeEnum.VUE_PROJECT.getValue() + "_" + appId;
    }

    public static File getProjectDir(Long appId) {
        return new File(getProjectDirPath(appId));
    }

    public static File getDistDir(Long appId) {
        return new File(getProjectDir(appId), "dist");
    }
}
