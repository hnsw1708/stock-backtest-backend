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

    @BeforeEach
    void setUp() {
        // No additional setup required; Mockito handles injection
    }

    // -------------------------------------------------------------------------
    // GET /  ->  index(Model)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("index() should return 'index' view name")
    void index_shouldReturnIndexViewName() {
        String viewName = mainController.index(model);

        assertThat(viewName).isEqualTo("index");
    }

    @Test
    @DisplayName("index() should add 'title' attribute to the model")
    void index_shouldAddTitleAttributeToModel() {
        mainController.index(model);

        verify(model, times(1)).addAttribute(
                eq("title"),
                eq("股票回溯策略分析系统 - 后端API服务")
        );
    }

    @Test
    @DisplayName("index() should interact with model exactly once")
    void index_shouldInteractWithModelExactlyOnce() {
        mainController.index(model);

        verify(model, times(1)).addAttribute(anyString(), any());
        verifyNoMoreInteractions(model);
    }

    // -------------------------------------------------------------------------
    // GET /status  ->  status()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("status() should return HTTP 200 OK")
    void status_shouldReturnHttp200() {
        ResponseEntity<Map<String, Object>> response = mainController.status();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("status() body should contain 'status' = 'UP'")
    void status_bodyContainsStatusUp() {
        ResponseEntity<Map<String, Object>> response = mainController.status();

        assertThat(response.getBody())
                .isNotNull()
                .containsEntry("status", "UP");
    }

    @Test
    @DisplayName("status() body should contain correct 'service' value")
    void status_bodyContainsServiceName() {
        ResponseEntity<Map<String, Object>> response = mainController.status();

        assertThat(response.getBody())
                .isNotNull()
                .containsEntry("service", "股票回溯策略分析系统后端");
    }

    @Test
    @DisplayName("status() body should contain 'version' = '1.0.0'")
    void status_bodyContainsVersion() {
        ResponseEntity<Map<String, Object>> response = mainController.status();

        assertThat(response.getBody())
                .isNotNull()
                .containsEntry("version", "1.0.0");
    }

    @Test
    @DisplayName("status() body should contain a numeric 'timestamp'")
    void status_bodyContainsTimestamp() {
        long before = System.currentTimeMillis();
        ResponseEntity<Map<String, Object>> response = mainController.status();
        long after = System.currentTimeMillis();

        assertThat(response.getBody()).isNotNull();
        Object timestamp = response.getBody().get("timestamp");
        assertThat(timestamp).isInstanceOf(Long.class);

        long ts = (Long) timestamp;
        assertThat(ts).isBetween(before, after);
    }

    @Test
    @DisplayName("status() body should contain 'system' as a Map")
    void status_bodyContainsSystemMap() {
        ResponseEntity<Map<String, Object>> response = mainController.status();

        assertThat(response.getBody()).isNotNull();
        Object system = response.getBody().get("system");
        assertThat(system).isInstanceOf(Map.class);
    }

    @Test
    @DisplayName("status() 'system' map should contain 'javaVersion'")
    void status_systemMapContainsJavaVersion() {
        ResponseEntity<Map<String, Object>> response = mainController.status();

        assertThat(response.getBody()).isNotNull();

        @SuppressWarnings("unchecked")
        Map<String, Object> system = (Map<String, Object>) response.getBody().get("system");

        assertThat(system).containsKey("javaVersion");
        assertThat(system.get("javaVersion")).isEqualTo(System.getProperty("java.version"));
    }

    @Test
    @DisplayName("status() 'system' map should contain memory fields ending with ' MB'")
    void status_systemMapContainsMemoryFields() {
        ResponseEntity<Map<String, Object>> response = mainController.status();

        assertThat(response.getBody()).isNotNull();

        @SuppressWarnings("unchecked")
        Map<String, Object> system = (Map<String, Object>) response.getBody().get("system");

        for (String key : new String[]{"totalMemory", "freeMemory", "maxMemory"}) {
            assertThat(system).containsKey(key);
            assertThat(system.get(key).toString()).endsWith(" MB");
        }
    }

    @Test
    @DisplayName("status() body should have exactly four top-level keys")
    void status_bodyHasExactlyFourTopLevelKeys() {
        ResponseEntity<Map<String, Object>> response = mainController.status();

        assertThat(response.getBody())
                .isNotNull()
                .containsOnlyKeys("status", "service", "timestamp", "version", "system");
    }

    // -------------------------------------------------------------------------
    // GET /api-test  ->  apiTest()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("apiTest() should return 'api-test' view name")
    void apiTest_shouldReturnApiTestViewName() {
        String viewName = mainController.apiTest();

        assertThat(viewName).isEqualTo("api-test");
    }

    // -------------------------------------------------------------------------
    // GET /test  ->  test()
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
    @DisplayName("test() should not return blank string")
    void test_shouldNotReturnBlank() {
        assertThat(mainController.test()).isNotBlank();
    }
}
