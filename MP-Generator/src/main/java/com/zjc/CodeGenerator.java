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

public class CodeGenerator {
    public static void main(String[] args) throws Exception {
        // 读取配置文件（classpath: generator.properties）
        Properties props = new Properties();
        try (InputStream is = CodeGenerator.class.getResourceAsStream("/generator.properties")) {
            props.load(new InputStreamReader(Objects.requireNonNull(is), StandardCharsets.UTF_8));
        }
        String url = props.getProperty("db.url");
        String username = props.getProperty("db.username");
        String password = props.getProperty("db.password");
        String[] tables = split(props.getProperty("generator.tables"));
        String[] tablePrefix = split(props.getProperty("generator.tablePrefix"));
        String parentPackage = props.getProperty("package.parent");

        File moduleRoot = new File(CodeGenerator.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI()).getParentFile().getParentFile();
        String outputDir = moduleRoot.toPath().resolve("src/main/java").toString();

        FastAutoGenerator.create(url, username, password)
                .globalConfig(builder -> builder.author("jiancai.zhong")
                        .commentDate("yyyy-MM-dd")
                        .outputDir(outputDir)
                        .disableOpenDir())
                .packageConfig(builder -> builder.parent(parentPackage)
                        .entity("entity")
                        .mapper("mapper")
                        .service("service")
                        .serviceImpl("service.impl")
                        .xml("mapper"))
                .strategyConfig(builder -> builder.addInclude(tables)
                        .addTablePrefix(tablePrefix)
                        .entityBuilder()
                        .enableLombok(new ClassAnnotationAttributes("@Data", "lombok.Data"))
                        .enableTableFieldAnnotation()
                        .mapperBuilder()
                        .convertMapperFileName((entityName -> entityName + "Mapper"))
                        .enableBaseResultMap()
                        .enableBaseColumnList()
                        .serviceBuilder()
                        .convertServiceFileName((entityName -> entityName + "Service"))
                        .controllerBuilder()
                        .disable())
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();
    }

    /** 按逗号拆分配置项，自动去空白和空值 */
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
