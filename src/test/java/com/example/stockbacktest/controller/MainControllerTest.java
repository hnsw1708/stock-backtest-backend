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
    // GET / -> index
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("index() should return view name 'index'")
    void index_shouldReturnIndexViewName() {
        String viewName = mainController.index(model);

        assertThat(viewName).isEqualTo("index");
    }

    @Test
    @DisplayName("index() should add 'title' attribute to model")
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
    // GET /status -> status()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("status() should return HTTP 200 OK")
    void status_shouldReturn200Ok() {
        ResponseEntity<Map<String, Object>> response = mainController.status();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("status() body should not be null")
    void status_bodyIsNotNull() {
        ResponseEntity<Map<String, Object>> response = mainController.status();

        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("status() body should contain 'status' = 'UP'")
    void status_shouldContainStatusUp() {
        Map<String, Object> body = mainController.status().getBody();

        assertThat(body).containsEntry("status", "UP");
    }

    @Test
    @DisplayName("status() body should contain expected service name")
    void status_shouldContainServiceName() {
        Map<String, Object> body = mainController.status().getBody();

        assertThat(body).containsEntry("service", "股票回溯策略分析系统后端");
    }

    @Test
    @DisplayName("status() body should contain version '1.0.0'")
    void status_shouldContainVersion() {
        Map<String, Object> body = mainController.status().getBody();

        assertThat(body).containsEntry("version", "1.0.0");
    }

    @Test
    @DisplayName("status() body should contain a non-null timestamp")
    void status_shouldContainTimestamp() {
        long before = System.currentTimeMillis();
        Map<String, Object> body = mainController.status().getBody();
        long after = System.currentTimeMillis();

        assertThat(body).containsKey("timestamp");
        long timestamp = (Long) body.get("timestamp");
        assertThat(timestamp).isBetween(before, after);
    }

    @Test
    @DisplayName("status() body should contain 'system' map with memory and java version info")
    void status_shouldContainSystemInfo() {
        Map<String, Object> body = mainController.status().getBody();

        assertThat(body).containsKey("system");

        @SuppressWarnings("unchecked")
        Map<String, Object> system = (Map<String, Object>) body.get("system");

        assertThat(system).containsKey("javaVersion");
        assertThat(system).containsKey("totalMemory");
        assertThat(system).containsKey("freeMemory");
        assertThat(system).containsKey("maxMemory");
    }

    @Test
    @DisplayName("status() system.javaVersion should not be blank")
    void status_javaVersionShouldNotBeBlank() {
        @SuppressWarnings("unchecked")
        Map<String, Object> system = (Map<String, Object>)
                mainController.status().getBody().get("system");

        String javaVersion = (String) system.get("javaVersion");
        assertThat(javaVersion).isNotBlank();
    }

    @Test
    @DisplayName("status() memory values should be expressed in MB")
    void status_memoryValuesShouldEndWithMB() {
        @SuppressWarnings("unchecked")
        Map<String, Object> system = (Map<String, Object>)
                mainController.status().getBody().get("system");

        assertThat((String) system.get("totalMemory")).endsWith(" MB");
        assertThat((String) system.get("freeMemory")).endsWith(" MB");
        assertThat((String) system.get("maxMemory")).endsWith(" MB");
    }

    @Test
    @DisplayName("status() should contain exactly the expected top-level keys")
    void status_shouldContainExpectedTopLevelKeys() {
        Map<String, Object> body = mainController.status().getBody();

        assertThat(body).containsOnlyKeys("status", "service", "timestamp", "version", "system");
    }

    // -------------------------------------------------------------------------
    // GET /api-test -> apiTest()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("apiTest() should return view name 'api-test'")
    void apiTest_shouldReturnApiTestViewName() {
        String viewName = mainController.apiTest();

        assertThat(viewName).isEqualTo("api-test");
    }

    @Test
    @DisplayName("apiTest() should never return null")
    void apiTest_shouldNotReturnNull() {
        assertThat(mainController.apiTest()).isNotNull();
    }

    // -------------------------------------------------------------------------
    // GET /test -> test()
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
    @DisplayName("test() should return a non-blank string")
    void test_shouldReturnNonBlankString() {
        assertThat(mainController.test()).isNotBlank();
    }
}
