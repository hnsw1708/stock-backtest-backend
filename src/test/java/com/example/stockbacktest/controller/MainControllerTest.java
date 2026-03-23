package com.example.stockbacktest.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MainController Unit Tests")
class MainControllerTest {

    @InjectMocks
    private MainController mainController;

    @Mock
    private Model model;

    @BeforeEach
    void setUp() {
        // InjectMocks handles instantiation
    }

    // -------------------------------------------------------------------------
    // index()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET / - should add title attribute and return 'index' view")
    void index_shouldAddTitleAndReturnIndexView() {
        // Act
        String viewName = mainController.index(model);

        // Assert
        assertThat(viewName).isEqualTo("index");
        verify(model, times(1)).addAttribute(
                eq("title"),
                eq("股票回溯策略分析系统 - 后端API服务")
        );
    }

    @Test
    @DisplayName("GET / - should not add unexpected attributes")
    void index_shouldNotAddExtraAttributes() {
        mainController.index(model);

        // Only one addAttribute call expected
        verify(model, times(1)).addAttribute(anyString(), any());
    }

    @Test
    @DisplayName("GET / - should work with a real Model (no mock)")
    void index_withRealModel_shouldReturnIndexView() {
        var realModel = new org.springframework.ui.ExtendedModelMap();

        String viewName = mainController.index(realModel);

        assertThat(viewName).isEqualTo("index");
        assertThat(realModel.get("title"))
                .isEqualTo("股票回溯策略分析系统 - 后端API服务");
    }

    // -------------------------------------------------------------------------
    // status()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /status - should return HTTP 200")
    void status_shouldReturn200() {
        ResponseEntity<Map<String, Object>> response = mainController.status();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("GET /status - body must not be null")
    void status_bodyMustNotBeNull() {
        ResponseEntity<Map<String, Object>> response = mainController.status();

        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("GET /status - should contain 'status' = 'UP'")
    void status_shouldContainStatusUp() {
        var body = mainController.status().getBody();

        assertThat(body).containsEntry("status", "UP");
    }

    @Test
    @DisplayName("GET /status - should contain correct service name")
    void status_shouldContainServiceName() {
        var body = mainController.status().getBody();

        assertThat(body).containsEntry("service", "股票回溯策略分析系统后端");
    }

    @Test
    @DisplayName("GET /status - should contain version 1.0.0")
    void status_shouldContainVersion() {
        var body = mainController.status().getBody();

        assertThat(body).containsEntry("version", "1.0.0");
    }

    @Test
    @DisplayName("GET /status - timestamp should be a positive long")
    void status_timestampShouldBePositiveLong() {
        var body = mainController.status().getBody();

        assertThat(body).containsKey("timestamp");
        Object timestamp = body.get("timestamp");
        assertThat(timestamp).isInstanceOf(Long.class);
        assertThat((Long) timestamp).isPositive();
    }

    @Test
    @DisplayName("GET /status - timestamp should be close to current time")
    void status_timestampShouldBeCloseToCurrentTime() {
        long before = System.currentTimeMillis();
        var body = mainController.status().getBody();
        long after = System.currentTimeMillis();

        long timestamp = (Long) body.get("timestamp");
        assertThat(timestamp).isBetween(before, after);
    }

    @Test
    @DisplayName("GET /status - should contain 'system' map with memory keys")
    void status_shouldContainSystemMapWithMemoryInfo() {
        var body = mainController.status().getBody();

        assertThat(body).containsKey("system");
        Object systemObj = body.get("system");
        assertThat(systemObj).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> system = (Map<String, Object>) systemObj;

        assertThat(system).containsKeys("javaVersion", "totalMemory", "freeMemory", "maxMemory");
    }

    @Test
    @DisplayName("GET /status - javaVersion must not be blank")
    void status_javaVersionMustNotBeBlank() {
        var body = mainController.status().getBody();

        @SuppressWarnings("unchecked")
        Map<String, Object> system = (Map<String, Object>) body.get("system");

        assertThat(system.get("javaVersion")).isNotNull();
        assertThat(system.get("javaVersion").toString()).isNotBlank();
    }

    @Test
    @DisplayName("GET /status - memory values should end with ' MB'")
    void status_memoryValuesShouldEndWithMB() {
        var body = mainController.status().getBody();

        @SuppressWarnings("unchecked")
        Map<String, Object> system = (Map<String, Object>) body.get("system");

        for (String key : new String[]{"totalMemory", "freeMemory", "maxMemory"}) {
            assertThat(system.get(key).toString())
                    .as("Key '%s' should end with ' MB'", key)
                    .endsWith(" MB");
        }
    }

    @Test
    @DisplayName("GET /status - Java version should be 21 or higher after migration")
    void status_javaVersionShouldBe21OrHigher() {
        var body = mainController.status().getBody();

        @SuppressWarnings("unchecked")
        Map<String, Object> system = (Map<String, Object>) body.get("system");

        String javaVersion = system.get("javaVersion").toString();
        // Parsing major version — handles formats like "21", "21.0.1", "21.0.1+12"
        int majorVersion = Integer.parseInt(javaVersion.split("[.+\\-]")[0]);
        assertThat(majorVersion).isGreaterThanOrEqualTo(21);
    }

    @Test
    @DisplayName("GET /status - should return all expected top-level keys")
    void status_shouldContainAllTopLevelKeys() {
        var body = mainController.status().getBody();

        assertThat(body).containsKeys("status", "service", "timestamp", "version", "system");
    }

    // -------------------------------------------------------------------------
    // apiTest()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api-test - should return 'api-test' view name")
    void apiTest_shouldReturnApiTestView() {
        String viewName = mainController.apiTest();

        assertThat(viewName).isEqualTo("api-test");
    }

    @Test
    @DisplayName("GET /api-test - view name must not be blank")
    void apiTest_viewNameMustNotBeBlank() {
        String viewName = mainController.apiTest();

        assertThat(viewName).isNotBlank();
    }

    // -------------------------------------------------------------------------
    // test()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /test - should return 'Test OK'")
    void test_shouldReturnTestOk() {
        String result = mainController.test();

        assertThat(result).isEqualTo("Test OK");
    }

    @Test
    @DisplayName("GET /test - result must not be null or empty")
    void test_resultMustNotBeNullOrEmpty() {
        String result = mainController.test();

        assertThat(result).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("GET /test - should return consistent value across multiple calls")
    void test_shouldBeIdempotent() {
        String first  = mainController.test();
        String second = mainController.test();
        String third  = mainController.test();

        assertThat(first).isEqualTo(second).isEqualTo(third);
    }
}
