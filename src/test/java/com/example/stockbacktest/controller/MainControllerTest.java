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
        // MainController has no dependencies to inject beyond what Mockito handles
    }

    // -----------------------------------------------------------------------
    // index()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("index() should return 'index' view name")
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
    @DisplayName("index() should not add any extra attributes beyond title")
    void index_shouldAddOnlyOneAttribute() {
        mainController.index(model);

        verify(model, times(1)).addAttribute(anyString(), any());
    }

    @Test
    @DisplayName("index() with null model should throw NullPointerException")
    void index_withNullModel_shouldThrowNullPointerException() {
        org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class,
                () -> mainController.index(null)
        );
    }

    // -----------------------------------------------------------------------
    // status()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("status() should return HTTP 200 OK")
    void status_shouldReturnHttpOk() {
        ResponseEntity<Map<String, Object>> response = mainController.status();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("status() response body should not be null")
    void status_bodyShould_notBeNull() {
        ResponseEntity<Map<String, Object>> response = mainController.status();

        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("status() should return status=UP")
    void status_shouldContainStatusUp() {
        ResponseEntity<Map<String, Object>> response = mainController.status();
        Map<String, Object> body = response.getBody();

        assertThat(body).containsEntry("status", "UP");
    }

    @Test
    @DisplayName("status() should contain expected service name")
    void status_shouldContainServiceName() {
        ResponseEntity<Map<String, Object>> response = mainController.status();
        Map<String, Object> body = response.getBody();

        assertThat(body).containsEntry("service", "股票回溯策略分析系统后端");
    }

    @Test
    @DisplayName("status() should contain version 1.0.0")
    void status_shouldContainVersion() {
        ResponseEntity<Map<String, Object>> response = mainController.status();
        Map<String, Object> body = response.getBody();

        assertThat(body).containsEntry("version", "1.0.0");
    }

    @Test
    @DisplayName("status() should contain a positive numeric timestamp")
    void status_shouldContainPositiveTimestamp() {
        ResponseEntity<Map<String, Object>> response = mainController.status();
        Map<String, Object> body = response.getBody();

        assertThat(body).containsKey("timestamp");
        Object timestamp = body.get("timestamp");
        assertThat(timestamp).isInstanceOf(Long.class);
        assertThat((Long) timestamp).isPositive();
    }

    @Test
    @DisplayName("status() timestamp should be close to current system time")
    void status_timestampShouldBeCloseToNow() {
        long before = System.currentTimeMillis();
        ResponseEntity<Map<String, Object>> response = mainController.status();
        long after = System.currentTimeMillis();

        long timestamp = (Long) response.getBody().get("timestamp");
        assertThat(timestamp).isBetween(before, after);
    }

    @Test
    @DisplayName("status() should contain 'system' map with memory and java version keys")
    void status_shouldContainSystemInfoMap() {
        ResponseEntity<Map<String, Object>> response = mainController.status();
        Map<String, Object> body = response.getBody();

        assertThat(body).containsKey("system");
        Object systemObj = body.get("system");
        assertThat(systemObj).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> system = (Map<String, Object>) systemObj;

        assertThat(system).containsKey("javaVersion");
        assertThat(system).containsKey("totalMemory");
        assertThat(system).containsKey("freeMemory");
        assertThat(system).containsKey("maxMemory");
    }

    @Test
    @DisplayName("status() system.javaVersion should match JVM system property")
    void status_javaVersionShouldMatchSystemProperty() {
        ResponseEntity<Map<String, Object>> response = mainController.status();

        @SuppressWarnings("unchecked")
        Map<String, Object> system = (Map<String, Object>) response.getBody().get("system");

        assertThat(system.get("javaVersion")).isEqualTo(System.getProperty("java.version"));
    }

    @Test
    @DisplayName("status() memory values should end with ' MB'")
    void status_memoryValuesShouldEndWithMB() {
        ResponseEntity<Map<String, Object>> response = mainController.status();

        @SuppressWarnings("unchecked")
        Map<String, Object> system = (Map<String, Object>) response.getBody().get("system");

        var memoryKeys = new String[]{"totalMemory", "freeMemory", "maxMemory"};
        for (String key : memoryKeys) {
            assertThat(system.get(key).toString())
                    .as("Memory key '%s' should end with ' MB'", key)
                    .endsWith(" MB");
        }
    }

    @Test
    @DisplayName("status() should always return a new independent response on repeated calls")
    void status_repeatedCallsShouldReturnIndependentResponses() {
        ResponseEntity<Map<String, Object>> first = mainController.status();
        ResponseEntity<Map<String, Object>> second = mainController.status();

        assertThat(first.getBody()).isNotSameAs(second.getBody());
        assertThat(first.getStatusCode()).isEqualTo(second.getStatusCode());
        assertThat(first.getBody().get("status")).isEqualTo(second.getBody().get("status"));
    }

    // -----------------------------------------------------------------------
    // apiTest()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("apiTest() should return 'api-test' view name")
    void apiTest_shouldReturnApiTestViewName() {
        String viewName = mainController.apiTest();

        assertThat(viewName).isEqualTo("api-test");
    }

    @Test
    @DisplayName("apiTest() should never return null")
    void apiTest_shouldNeverReturnNull() {
        assertThat(mainController.apiTest()).isNotNull();
    }

    // -----------------------------------------------------------------------
    // test()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("test() should return 'Test OK'")
    void test_shouldReturnTestOk() {
        String result = mainController.test();

        assertThat(result).isEqualTo("Test OK");
    }

    @Test
    @DisplayName("test() should never return null")
    void test_shouldNeverReturnNull() {
        assertThat(mainController.test()).isNotNull();
    }

    @Test
    @DisplayName("test() result should not be blank")
    void test_shouldNotReturnBlank() {
        assertThat(mainController.test()).isNotBlank();
    }

    @Test
    @DisplayName("test() should be idempotent across multiple calls")
    void test_shouldBeIdempotent() {
        String first = mainController.test();
        String second = mainController.test();
        String third = mainController.test();

        assertThat(first).isEqualTo(second).isEqualTo(third);
    }
}
