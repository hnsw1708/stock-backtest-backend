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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MainController Unit Tests")
class MainControllerTest {

    @InjectMocks
    private MainController mainController;

    @Mock
    private Model model;

    // -----------------------------------------------------------------------
    // index()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET / - should return 'index' view name")
    void index_shouldReturnIndexViewName() {
        String viewName = mainController.index(model);

        assertThat(viewName).isEqualTo("index");
    }

    @Test
    @DisplayName("GET / - should add 'title' attribute to model")
    void index_shouldAddTitleAttributeToModel() {
        mainController.index(model);

        verify(model, times(1)).addAttribute(
                eq("title"),
                eq("股票回溯策略分析系统 - 后端API服务")
        );
    }

    @Test
    @DisplayName("GET / - should interact with model exactly once")
    void index_shouldInteractWithModelExactlyOnce() {
        mainController.index(model);

        verifyNoMoreInteractions(model);
    }

    // -----------------------------------------------------------------------
    // status()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /status - should return HTTP 200 OK")
    void status_shouldReturnHttpOk() {
        ResponseEntity<Map<String, Object>> response = mainController.status();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("GET /status - response body must not be null")
    void status_bodyMustNotBeNull() {
        ResponseEntity<Map<String, Object>> response = mainController.status();

        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("GET /status - body should contain 'status' = 'UP'")
    void status_bodyShouldContainStatusUp() {
        Map<String, Object> body = mainController.status().getBody();

        assertThat(body).containsEntry("status", "UP");
    }

    @Test
    @DisplayName("GET /status - body should contain 'service' key with correct value")
    void status_bodyShouldContainServiceKey() {
        Map<String, Object> body = mainController.status().getBody();

        assertThat(body).containsEntry("service", "股票回溯策略分析系统后端");
    }

    @Test
    @DisplayName("GET /status - body should contain 'version' = '1.0.0'")
    void status_bodyShouldContainVersion() {
        Map<String, Object> body = mainController.status().getBody();

        assertThat(body).containsEntry("version", "1.0.0");
    }

    @Test
    @DisplayName("GET /status - body should contain a numeric 'timestamp'")
    void status_bodyShouldContainTimestamp() {
        long before = System.currentTimeMillis();
        Map<String, Object> body = mainController.status().getBody();
        long after = System.currentTimeMillis();

        assertThat(body).containsKey("timestamp");
        long timestamp = (Long) body.get("timestamp");
        assertThat(timestamp).isBetween(before, after);
    }

    @Test
    @DisplayName("GET /status - body should contain a 'system' sub-map")
    void status_bodyShouldContainSystemSubMap() {
        Map<String, Object> body = mainController.status().getBody();

        assertThat(body).containsKey("system");
        Object systemObj = body.get("system");
        assertThat(systemObj).isInstanceOf(Map.class);
    }

    @Test
    @DisplayName("GET /status - 'system' map should contain javaVersion")
    void status_systemShouldContainJavaVersion() {
        Map<String, Object> body = mainController.status().getBody();

        @SuppressWarnings("unchecked")
        Map<String, Object> system = (Map<String, Object>) body.get("system");

        assertThat(system).containsKey("javaVersion");
        assertThat(system.get("javaVersion")).isNotNull();
    }

    @Test
    @DisplayName("GET /status - 'system' map should contain totalMemory, freeMemory, maxMemory")
    void status_systemShouldContainMemoryKeys() {
        Map<String, Object> body = mainController.status().getBody();

        @SuppressWarnings("unchecked")
        Map<String, Object> system = (Map<String, Object>) body.get("system");

        assertThat(system).containsKeys("totalMemory", "freeMemory", "maxMemory");
    }

    @Test
    @DisplayName("GET /status - memory values should end with ' MB'")
    void status_memoryValuesShouldEndWithMb() {
        Map<String, Object> body = mainController.status().getBody();

        @SuppressWarnings("unchecked")
        Map<String, Object> system = (Map<String, Object>) body.get("system");

        for (String key : new String[]{"totalMemory", "freeMemory", "maxMemory"}) {
            Object value = system.get(key);
            assertThat(value).isInstanceOf(String.class);
            assertThat((String) value).endsWith(" MB");
        }
    }

    @Test
    @DisplayName("GET /status - top-level body should have exactly 5 keys")
    void status_bodyTopLevelShouldHaveFiveKeys() {
        // status, service, timestamp, version, system
        Map<String, Object> body = mainController.status().getBody();

        assertThat(body).hasSize(5);
    }

    @Test
    @DisplayName("GET /status - system sub-map should have exactly 4 keys")
    void status_systemSubMapShouldHaveFourKeys() {
        // javaVersion, totalMemory, freeMemory, maxMemory
        Map<String, Object> body = mainController.status().getBody();

        @SuppressWarnings("unchecked")
        Map<String, Object> system = (Map<String, Object>) body.get("system");

        assertThat(system).hasSize(4);
    }

    // -----------------------------------------------------------------------
    // apiTest()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /api-test - should return 'api-test' view name")
    void apiTest_shouldReturnApiTestViewName() {
        String viewName = mainController.apiTest();

        assertThat(viewName).isEqualTo("api-test");
    }

    // -----------------------------------------------------------------------
    // test()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /test - should return 'Test OK'")
    void test_shouldReturnTestOkString() {
        String result = mainController.test();

        assertThat(result).isEqualTo("Test OK");
    }

    @Test
    @DisplayName("GET /test - result should not be null or blank")
    void test_resultShouldNotBeNullOrBlank() {
        String result = mainController.test();

        assertThat(result).isNotNull().isNotBlank();
    }
}
