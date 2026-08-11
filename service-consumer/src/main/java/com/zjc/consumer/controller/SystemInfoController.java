package com.zjc.consumer.controller;

import com.zjc.common.dto.SystemInfoDTO;
import com.zjc.common.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;

/**
 * 系统信息查询接口。
 *
 * <p>展示 pom.xml 中的项目元数据（名称、描述、版本）以及运行环境信息。
 * 未通过 Maven 构建时构建时间为当前时间，其余构建字段为 null。
 *
 * @author jiancai.zhong
 */
@Tag(name = "系统信息", description = "查询项目构建信息与运行环境")
@RestController
public class SystemInfoController {

    @Autowired(required = false)
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
                    : Instant.now(Clock.systemUTC()).toString());
        } else {
            dto.setBuildTime(Instant.now(Clock.systemUTC()).toString());
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
