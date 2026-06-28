package com.zhenq.generator;

import com.mybatisflex.codegen.Generator;
import com.mybatisflex.codegen.config.GlobalConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/**
 * MyBatis Flex 代码生成器（工具类，按需手动运行 main 方法）。
 * <p>
 * 运行前请确认数据库已创建并执行 resources/sql/create_table.sql。
 * 生成的 Entity / Mapper / Service / Controller 会写入对应包下，
 * 本项目已手写一份用户模块代码，运行生成器前请注意避免覆盖已修改的文件。
 */
public class MyBatisFlexCodeGenerator {

    // 数据库连接配置（与 application.yml 保持一致）
    private static final String JDBC_URL =
            "jdbc:mysql://localhost:3306/student_pro?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true";
    private static final String JDBC_USERNAME = "root";
    private static final String JDBC_PASSWORD = "root";

    // 需要生成的表名
    private static final String[] GENERATE_TABLES = {"user"};

    public static void main(String[] args) {
        // 1. 配置数据源
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(JDBC_URL);
        dataSource.setUsername(JDBC_USERNAME);
        dataSource.setPassword(JDBC_PASSWORD);
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // 2. 配置全局策略
        GlobalConfig globalConfig = createGlobalConfig();

        // 3. 通过 datasource 和 globalConfig 创建代码生成器，生成代码
        Generator generator = new Generator(dataSource, globalConfig);
        generator.generate();

        dataSource.close();
    }

    private static GlobalConfig createGlobalConfig() {
        GlobalConfig globalConfig = new GlobalConfig();

        // 包配置
        globalConfig.getPackageConfig()
                .setBasePackage("com.zhenq.generated");

        // 生成策略：表名、逻辑删除字段
        globalConfig.getStrategyConfig()
                .setGenerateTable(GENERATE_TABLES)
                .setLogicDeleteColumn("isDelete");

        // 开启生成 Entity，并使用 Lombok
        globalConfig.enableEntity()
                .setWithLombok(true);

        // 开启生成 Mapper / Service / ServiceImpl / Controller
        globalConfig.enableMapper();
        globalConfig.enableService();
        globalConfig.enableServiceImpl();
        globalConfig.enableController();

        return globalConfig;
    }
}
