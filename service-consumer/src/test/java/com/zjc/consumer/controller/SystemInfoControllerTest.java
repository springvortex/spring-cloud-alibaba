package com.zjc.consumer.controller;

import com.zjc.common.dto.SystemInfoDTO;
import com.zjc.common.web.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SystemInfoController} 单元测试。
 *
 * <p>验证构建元数据存在/不存在两种场景，以及运行环境字段的正确性。
 *
 * @author jiancai.zhong
 */
@DisplayName("系统信息 Controller")
class SystemInfoControllerTest {

    private SystemInfoController systemInfoController;

    @BeforeEach
    void setUp() {
        systemInfoController = new SystemInfoController();
    }

    /**
     * 构造模拟的 BuildProperties。
     */
    private BuildProperties mockBuildProperties() {
        Properties props = new Properties();
        props.setProperty("name", "Service Consumer");
        props.setProperty("version", "1.0.0");
        props.setProperty("artifact", "service-consumer");
        props.setProperty("group", "com.zjc");
        props.setProperty("description", "服务消费者");
        props.setProperty("time", "2026-08-07T10:17:32.177Z");
        return new BuildProperties(props);
    }

    /**
     * 验证 BuildProperties 存在时，构建元数据被正确填充。
     */
    @Test
    @DisplayName("info: BuildProperties 存在时返回构建元数据")
    void testInfoWithBuildProperties() {
        ReflectionTestUtils.setField(systemInfoController,
                "buildProperties", mockBuildProperties());

        ApiResponse<SystemInfoDTO> resp = systemInfoController.info();

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData().getProjectName()).isEqualTo("Service Consumer");
        assertThat(resp.getData().getVersion()).isEqualTo("1.0.0");
        assertThat(resp.getData().getArtifact()).isEqualTo("service-consumer");
    }

    /**
     * 验证 BuildProperties 为 null 时，构建字段为 null，运行环境字段仍正常。
     */
    @Test
    @DisplayName("info: BuildProperties 为 null 时仅返回运行环境信息")
    void testInfoWithoutBuildProperties() {
        ApiResponse<SystemInfoDTO> resp = systemInfoController.info();

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData().getProjectName()).isNull();
        assertThat(resp.getData().getVersion()).isNull();
    }

    /**
     * 验证运行环境字段始终被正确填充。
     */
    @Test
    @DisplayName("info: 运行环境字段始终被填充")
    void testInfoRuntimeFieldsAlwaysPopulated() {
        ApiResponse<SystemInfoDTO> resp = systemInfoController.info();

        SystemInfoDTO data = resp.getData();
        assertThat(data.getJavaVersion()).isEqualTo(System.getProperty("java.version"));
        assertThat(data.getOsName()).isEqualTo(System.getProperty("os.name"));
        assertThat(data.getUserName()).isEqualTo(System.getProperty("user.name"));
        assertThat(data.getSpringBootVersion()).isNotNull();
    }
}
