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
        // InjectMocks handles instantiation; nothing extra needed
    }

    // -------------------------------------------------------------------------
    // GET /
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("index() should return 'index' view name")
    void index_shouldReturnIndexViewName() {
        String viewName = mainController.index(model);

        assertThat(viewName).isEqualTo("index");
    }

    @Test
    @DisplayName("index() should set 'title' attribute on model")
    void index_shouldAddTitleAttribute() {
        mainController.index(model);

        verify(model, times(1)).addAttribute(
                eq("title"),
                eq("股票回溯策略分析系统 - 后端API服务")
        );
    }

    @Test
    @DisplayName("index() should not add any unexpected attributes to the model")
    void index_shouldAddExactlyOneAttribute() {
        mainController.index(model);

        // Only one addAttribute call should happen
        verify(model, times(1)).addAttribute(anyString(), any());
    }

    @Test
    @DisplayName("index() should not return null view name")
    void index_viewNameShouldNotBeNull() {
        String viewName = mainController.index(model);

        assertThat(viewName).isNotNull();
    }

    // -------------------------------------------------------------------------
    // GET /status
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("status() should return HTTP 200 OK")
    void status_shouldReturnOkHttpStatus() {
        ResponseEntity<Map<String, Object>> response = mainController.status();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("status() body should not be null")
    void status_bodyNotNull() {
        ResponseEntity<Map<String, Object>> response = mainController.status();

        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("status() should contain 'status' key with value 'UP'")
    void status_shouldContainStatusUp() {
        Map<String, Object> body = mainController.status().getBody();

        assertThat(body).containsEntry("status", "UP");
    }

    @Test
    @DisplayName("status() should contain 'service' key with correct service name")
    void status_shouldContainServiceName() {
        Map<String, Object> body = mainController.status().getBody();

        assertThat(body).containsEntry("service", "股票回溯策略分析系统后端");
    }

    @Test
    @DisplayName("status() should contain 'version' key with value '1.0.0'")
    void status_shouldContainVersion() {
        Map<String, Object> body = mainController.status().getBody();

        assertThat(body).containsEntry("version", "1.0.0");
    }

    @Test
    @DisplayName("status() should contain 'timestamp' key with a positive long value")
    void status_shouldContainPositiveTimestamp() {
        long before = System.currentTimeMillis();
        Map<String, Object> body = mainController.status().getBody();
        long after = System.currentTimeMillis();

        assertThat(body).containsKey("timestamp");
        long timestamp = (Long) body.get("timestamp");
        assertThat(timestamp)
                .isGreaterThanOrEqualTo(before)
                .isLessThanOrEqualTo(after);
    }

    @Test
    @DisplayName("status() should contain 'system' key with a non-null Map")
    void status_shouldContainSystemMap() {
        Map<String, Object> body = mainController.status().getBody();

        assertThat(body).containsKey("system");
        Object systemObj = body.get("system");
        assertThat(systemObj).isInstanceOf(Map.class);
    }

    @Test
    @DisplayName("status() system map should contain 'javaVersion' matching current JVM")
    void status_systemMap_shouldContainJavaVersion() {
        Map<String, Object> body = mainController.status().getBody();

        @SuppressWarnings("unchecked")
        Map<String, Object> system = (Map<String, Object>) body.get("system");

        assertThat(system).containsKey("javaVersion");
        assertThat(system.get("javaVersion"))
                .isEqualTo(System.getProperty("java.version"));
    }

    @Test
    @DisplayName("status() system map should contain memory fields in MB format")
    void status_systemMap_shouldContainMemoryFields() {
        Map<String, Object> body = mainController.status().getBody();

        @SuppressWarnings("unchecked")
        Map<String, Object> system = (Map<String, Object>) body.get("system");

        assertThat(system).containsKeys("totalMemory", "freeMemory", "maxMemory");

        // Each value should end with " MB" — use pattern matching (Java 16+)
        for (String key : new String[]{"totalMemory", "freeMemory", "maxMemory"}) {
            Object value = system.get(key);
            assertThat(value).isInstanceOf(String.class);
            if (value instanceof String memValue) {
                assertThat(memValue).endsWith(" MB");
                // The numeric portion should be parseable and non-negative
                String numeric = memValue.replace(" MB", "").trim();
                assertThat(Long.parseLong(numeric)).isGreaterThanOrEqualTo(0L);
            }
        }
    }

    @Test
    @DisplayName("status() should expose exactly four top-level keys")
    void status_bodyExactlyFourTopLevelKeys() {
        Map<String, Object> body = mainController.status().getBody();

        assertThat(body).hasSize(5); // status, service, timestamp, version, system
    }

    // -------------------------------------------------------------------------
    // GET /api-test
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("apiTest() should return 'api-test' view name")
    void apiTest_shouldReturnApiTestViewName() {
        String viewName = mainController.apiTest();

        assertThat(viewName).isEqualTo("api-test");
    }

    @Test
    @DisplayName("apiTest() should not return null")
    void apiTest_shouldNotReturnNull() {
        assertThat(mainController.apiTest()).isNotNull();
    }

    // -------------------------------------------------------------------------
    // GET /test
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("test() should return 'Test OK'")
    void test_shouldReturnTestOk() {
        String result = mainController.test();

        assertThat(result).isEqualTo("Test OK");
    }

    @Test
    @DisplayName("test() should not return null")
    void test_shouldNotReturnNull() {
        assertThat(mainController.test()).isNotNull();
    }

    @Test
    @DisplayName("test() should not return an empty string")
    void test_shouldNotReturnEmptyString() {
        assertThat(mainController.test()).isNotBlank();
    }
}
