package com.zjc;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.baomidou.mybatisplus.generator.model.ClassAnnotationAttributes;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;

/**
 * MyBatis-Plus 代码生成器入口。
 * <p>
 * 连接数据库，按 {@code resources/generator.properties} 中的配置，
 * 自动生成 Entity / Mapper / Service / ServiceImpl / Mapper XML 等代码。
 * 所有可变参数（数据库连接、表名、表前缀、父包名）均外置到配置文件，
 * 修改后无需改动本类即可重新生成。
 */
public class CodeGenerator {
    public static void main(String[] args) throws Exception {
        // ========== 1. 读取配置文件（classpath: generator.properties） ==========
        Properties props = new Properties();
        try (InputStream is = CodeGenerator.class.getResourceAsStream("/generator.properties")) {
            // 用 UTF-8 读取，避免中文注释乱码（Properties.load(InputStream) 默认 ISO-8859-1）
            props.load(new InputStreamReader(Objects.requireNonNull(is), StandardCharsets.UTF_8));
        }
        String url = props.getProperty("db.url");
        String username = props.getProperty("db.username");
        String password = props.getProperty("db.password");
        String[] tables = split(props.getProperty("generator.tables"));
        String[] tablePrefix = split(props.getProperty("generator.tablePrefix"));
        String parentPackage = props.getProperty("package.parent");

        // ========== 2. 计算输出目录 ==========
        // 以 CodeGenerator.class 的编译位置（target/classes）反推模块根目录，
        // 保证无论从哪个工作目录运行，生成的代码都稳定落在本模块的 src/main/java 下
        File moduleRoot = new File(CodeGenerator.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI()).getParentFile().getParentFile();
        String outputDir = moduleRoot.toPath().resolve("src/main/java").toString();

        // ========== 3. 组装并执行生成器 ==========
        FastAutoGenerator.create(url, username, password)
                // 全局配置：作者、注释日期格式、输出目录
                .globalConfig(builder -> builder.author("jiancai.zhong")
                        .commentDate("yyyy-MM-dd")
                        .outputDir(outputDir)
                        .disableOpenDir())
                // 包配置：父包及各层子包名
                .packageConfig(builder -> builder.parent(parentPackage)
                        .entity("entity")
                        .mapper("mapper")
                        .service("service")
                        .serviceImpl("service.impl")
                        .xml("mapper"))
                // 策略配置：目标表、表前缀、各层生成规则
                .strategyConfig(builder -> builder.addInclude(tables)
                        .addTablePrefix(tablePrefix)
                        // 实体：使用 Lombok @Data、字段加 @TableField 注解、serialVersionUID 加 @Serial
                        .entityBuilder()
                        .enableLombok(new ClassAnnotationAttributes("@Data", "lombok.Data"))
                        .enableTableFieldAnnotation()
                        .enableSerialAnnotation()
                        // Mapper：文件名以 Mapper 结尾，生成 BaseResultMap 和 BaseColumnList
                        .mapperBuilder()
                        .convertMapperFileName((entityName -> entityName + "Mapper"))
                        .enableBaseResultMap()
                        .enableBaseColumnList()
                        // Service：接口名以 Service 结尾
                        .serviceBuilder()
                        .convertServiceFileName((entityName -> entityName + "Service"))
                        // 不生成 Controller
                        .controllerBuilder()
                        .disable())
                // 使用 Freemarker 模板引擎
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();
    }

    /**
     * 按英文逗号拆分配置项，自动去除首尾空白和空值。
     *
     * @param value 原始配置值，如 "t_user, t_order"
     * @return 拆分后的数组，如 ["t_user", "t_order"]；输入为空时返回空数组
     */
    private static String[] split(String value) {
        if (value == null || value.isBlank()) {
            return new String[0];
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }
}
