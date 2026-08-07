package com.zjc.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 系统信息 DTO，展示项目构建元数据与运行环境信息。
 *
 * <p>构建元数据（项目名称、版本、构建时间等）由 Spring Boot 的
 * {@code build-info} 插件在编译期写入 {@code META-INF/build-info.properties}，
 * 运行时通过 {@code BuildProperties} 读取。
 *
 * @author jiancai.zhong
 */
@Schema(description = "系统信息")
@Data
public class SystemInfoDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "项目名称（pom.xml 中的 name）")
    private String projectName;

    @Schema(description = "项目描述（pom.xml 中的 description）")
    private String description;

    @Schema(description = "项目版本（pom.xml 中的 version）")
    private String version;

    @Schema(description = "构件 ID（artifactId）")
    private String artifact;

    @Schema(description = "groupId")
    private String group;

    @Schema(description = "构建时间")
    private String buildTime;

    @Schema(description = "Java 运行时版本")
    private String javaVersion;

    @Schema(description = "JVM 供应商")
    private String javaVendor;

    @Schema(description = "操作系统名称")
    private String osName;

    @Schema(description = "操作系统架构")
    private String osArch;

    @Schema(description = "Spring Boot 版本")
    private String springBootVersion;

    @Schema(description = "启动用户")
    private String userName;
}
