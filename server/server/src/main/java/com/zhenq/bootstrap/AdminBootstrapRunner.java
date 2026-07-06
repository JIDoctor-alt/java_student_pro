package com.zhenq.bootstrap;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.zhenq.model.entity.User;
import com.zhenq.model.enums.UserRoleEnum;
import com.zhenq.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 管理员账号自举：项目首次部署（全新数据库）时，若不存在任何管理员，
 * 则自动创建一个默认管理员，避免“无人可登录后台”的初始化困境。
 * <p>
 * 相关配置（可通过环境变量注入）：
 * <ul>
 *     <li>app.admin.init-enabled：是否开启自举，默认 true</li>
 *     <li>app.admin.account：管理员账号，默认 admin</li>
 *     <li>app.admin.password：管理员密码，为空时随机生成并打印到日志</li>
 * </ul>
 */
@Slf4j
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    @Resource
    private UserService userService;

    @Value("${app.admin.init-enabled:true}")
    private boolean initEnabled;

    @Value("${app.admin.account:admin}")
    private String adminAccount;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (!initEnabled) {
            return;
        }
        // 幂等：只要「配置指定的管理员账号」已存在就跳过，避免重复创建。
        // 注意：这里按账号判断而非“是否存在任意 admin”，
        // 以保证部署后配置的默认管理员账号一定可登录（即使库里已有其他管理员）。
        User existing = userService.getOne(
                QueryWrapper.create().eq("user_account", adminAccount));
        if (existing != null) {
            syncBootstrapAdminDisplayName(existing);
            if (!UserRoleEnum.ADMIN.getValue().equals(existing.getUserRole())) {
                log.warn("[AdminBootstrap] 账号 [{}] 已存在但角色非 admin，未做任何修改，请手动处理。", adminAccount);
            }
            return;
        }

        // 密码：优先使用配置；未配置则随机生成并打印，避免生产出现公开默认密码
        boolean generated = StrUtil.isBlank(adminPassword);
        String rawPassword = generated ? RandomUtil.randomString(12) : adminPassword;

        User admin = new User();
        admin.setUserAccount(adminAccount);
        admin.setUserPassword(userService.getEncryptPassword(rawPassword));
        admin.setUserName("超级管理员");
        admin.setUserRole(UserRoleEnum.ADMIN.getValue());
        boolean saved = userService.save(admin);
        if (!saved) {
            log.error("[AdminBootstrap] 初始管理员创建失败，请检查数据库。");
            return;
        }

        log.warn("========================================================");
        log.warn("[AdminBootstrap] 已创建初始管理员账号：");
        log.warn("[AdminBootstrap]   账号: {}", adminAccount);
        if (generated) {
            log.warn("[AdminBootstrap]   密码(随机生成，请尽快登录修改): {}", rawPassword);
        } else {
            log.warn("[AdminBootstrap]   密码: 使用配置项 app.admin.password");
        }
        log.warn("[AdminBootstrap] 生产环境请务必修改默认密码！");
        log.warn("========================================================");
    }

    /**
     * 修复因服务器非 UTF-8 环境写入数据库的管理员昵称乱码。
     */
    private void syncBootstrapAdminDisplayName(User existing) {
        if (!adminAccount.equals(existing.getUserAccount())) {
            return;
        }
        String expectedName = "超级管理员";
        if (expectedName.equals(existing.getUserName())) {
            return;
        }
        User update = new User();
        update.setId(existing.getId());
        update.setUserName(expectedName);
        boolean ok = userService.updateById(update);
        if (ok) {
            log.warn("[AdminBootstrap] 已修复管理员昵称乱码，同步为「{}」", expectedName);
        }
    }
}
