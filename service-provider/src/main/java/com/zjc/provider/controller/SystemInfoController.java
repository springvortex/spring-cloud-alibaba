package com.zjc.provider.controller;

import com.zjc.common.dto.SystemInfoDTO;
import com.zjc.common.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * 系统信息查询接口。
 *
 * <p>展示 pom.xml 中的项目元数据（名称、描述、版本）以及运行环境信息
 *（Java 版本、操作系统、Spring Boot 版本等）。
 *
 * <p>构建元数据由 {@code spring-boot-maven-plugin} 的 {@code build-info} 目标
 * 在编译期生成到 {@code META-INF/build-info.properties}，
 * 未通过 Maven 构建时（如直接 IDE 运行）{@link BuildProperties} 为 null，
 * 此时构建时间为当前时间，其余构建字段为 null。
 *
 * @author jiancai.zhong
 */
@Tag(name = "系统信息", description = "查询项目构建信息与运行环境")
@RestController
public class SystemInfoController {

    @Resource
    private BuildProperties buildProperties;

    @Operation(summary = "查询系统信息")
    @GetMapping("/system/info")
    public ApiResponse<SystemInfoDTO> info() {
        SystemInfoDTO dto = new SystemInfoDTO();
        if (buildProperties != null) {
            dto.setProjectName(buildProperties.getName());
            dto.setDescription(buildProperties.get("description"));
            dto.setVersion(buildProperties.getVersion());
            dto.setArtifact(buildProperties.getArtifact());
            dto.setGroup(buildProperties.getGroup());
            dto.setBuildTime(buildProperties.getTime() != null
                    ? buildProperties.getTime().toString()
                    : Instant.now().toString());
        } else {
            dto.setBuildTime(Instant.now().toString());
        }
        dto.setJavaVersion(System.getProperty("java.version"));
        dto.setJavaVendor(System.getProperty("java.vendor"));
        dto.setOsName(System.getProperty("os.name"));
        dto.setOsArch(System.getProperty("os.arch"));
        dto.setSpringBootVersion(SpringBootVersion.getVersion());
        dto.setUserName(System.getProperty("user.name"));
        return ApiResponse.success(dto);
    }
}
