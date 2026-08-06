package com.zjc;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.baomidou.mybatisplus.generator.model.ClassAnnotationAttributes;

public class CodeGenerator {
    public static void main(String[] args) {
        FastAutoGenerator.create("jdbc:mysql://192.168.100.128:3306/spring_cloud_alibaba?useUnicode=true&characterEncoding=utf-8&zeroDateTimeBehavior=convertToNull&allowMultiQueries=true",
                        "spring_cloud_alibaba",
                        "fEmTS3Cfz3DTpMMS")
                .globalConfig(builder -> builder.author("jiancai.zhong")
                        .commentDate("yyyy-MM-dd")
                        .outputDir("src/main/java")
                        .disableOpenDir())
                .packageConfig(builder -> builder.parent("com.zjc")
                        .entity("entity")
                        .mapper("dao")
                        .service("service")
                        .serviceImpl("service.impl")
                        .xml("mapper"))
                .strategyConfig(builder -> builder.addInclude("t_user")
                        .addTablePrefix("t_")
                        .entityBuilder()
                        .enableLombok(new ClassAnnotationAttributes("@Data", "lombok.Data"))
                        .enableTableFieldAnnotation()
                        .mapperBuilder()
                        .convertMapperFileName((entityName -> entityName + "DAO"))
                        .enableBaseResultMap()
                        .enableBaseColumnList()
                        // 设置service接口
                        .serviceBuilder()
                        .convertServiceFileName((entityName -> entityName + "Service"))
                        .controllerBuilder()
                        .disable())
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();
    }
}
