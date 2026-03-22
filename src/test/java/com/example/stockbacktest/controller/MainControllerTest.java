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
        // InjectMocks handles instantiation; nothing additional needed
    }

    // -------------------------------------------------------------------------
    // index()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET / — returns 'index' view name")
    void index_returnsIndexViewName() {
        String viewName = mainController.index(model);

        assertThat(viewName).isEqualTo("index");
    }

    @Test
    @DisplayName("GET / — adds expected title attribute to model")
    void index_addsExpectedTitleToModel() {
        mainController.index(model);

        verify(model, times(1))
                .addAttribute(eq("title"), eq("股票回溯策略分析系统 - 后端API服务"));
    }

    @Test
    @DisplayName("GET / — model interaction limited to one addAttribute call")
    void index_modelInteractionLimitedToOneAddAttributeCall() {
        mainController.index(model);

        verifyNoMoreInteractions(model);
    }

    // -------------------------------------------------------------------------
    // status()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /status — returns 200 OK")
    void status_returns200Ok() {
        ResponseEntity<Map<String, Object>> response = mainController.status();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("GET /status — body is not null")
    void status_bodyIsNotNull() {
        ResponseEntity<Map<String, Object>> response = mainController.status();

        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("GET /status — status field equals 'UP'")
    void status_statusFieldIsUp() {
        ResponseEntity<Map<String, Object>> response = mainController.status();
        Map<String, Object> body = response.getBody();

        assertThat(body).containsEntry("status", "UP");
    }

    @Test
    @DisplayName("GET /status — service field equals expected Chinese label")
    void status_serviceFieldEqualsExpectedLabel() {
        ResponseEntity<Map<String, Object>> response = mainController.status();
        Map<String, Object> body = response.getBody();

        assertThat(body).containsEntry("service", "股票回溯策略分析系统后端");
    }

    @Test
    @DisplayName("GET /status — version field equals '1.0.0'")
    void status_versionFieldIs100() {
        ResponseEntity<Map<String, Object>> response = mainController.status();
        Map<String, Object> body = response.getBody();

        assertThat(body).containsEntry("version", "1.0.0");
    }

    @Test
    @DisplayName("GET /status — timestamp is a positive long value")
    void status_timestampIsPositiveLong() {
        long before = System.currentTimeMillis();
        ResponseEntity<Map<String, Object>> response = mainController.status();
        long after = System.currentTimeMillis();

        Map<String, Object> body = response.getBody();
        assertThat(body).containsKey("timestamp");

        Object ts = body.get("timestamp");
        assertThat(ts).isInstanceOf(Long.class);
        long timestamp = (Long) ts;
        assertThat(timestamp).isBetween(before, after);
    }

    @Test
    @DisplayName("GET /status — system sub-map is present and non-null")
    void status_systemSubMapIsPresentAndNonNull() {
        ResponseEntity<Map<String, Object>> response = mainController.status();
        Map<String, Object> body = response.getBody();

        assertThat(body).containsKey("system");
        assertThat(body.get("system")).isNotNull();
    }

    @Test
    @DisplayName("GET /status — system sub-map contains javaVersion")
    void status_systemSubMapContainsJavaVersion() {
        ResponseEntity<Map<String, Object>> response = mainController.status();
        Map<String, Object> body = response.getBody();

        @SuppressWarnings("unchecked")
        Map<String, Object> system = (Map<String, Object>) body.get("system");

        assertThat(system).containsKey("javaVersion");
        assertThat(system.get("javaVersion")).isNotNull();
        assertThat(system.get("javaVersion").toString()).isNotBlank();
    }

    @Test
    @DisplayName("GET /status — system sub-map contains memory fields ending with ' MB'")
    void status_systemSubMapContainsMemoryFields() {
        ResponseEntity<Map<String, Object>> response = mainController.status();
        Map<String, Object> body = response.getBody();

        @SuppressWarnings("unchecked")
        Map<String, Object> system = (Map<String, Object>) body.get("system");

        for (String key : new String[]{"totalMemory", "freeMemory", "maxMemory"}) {
            assertThat(system).containsKey(key);
            String value = system.get(key).toString();
            assertThat(value)
                    .as("Memory field '%s' should end with ' MB'", key)
                    .endsWith(" MB");
        }
    }

    @Test
    @DisplayName("GET /status — body contains exactly the expected top-level keys")
    void status_bodyContainsExactExpectedTopLevelKeys() {
        ResponseEntity<Map<String, Object>> response = mainController.status();
        Map<String, Object> body = response.getBody();

        assertThat(body).containsOnlyKeys("status", "service", "timestamp", "version", "system");
    }

    @Test
    @DisplayName("GET /status — consecutive calls produce non-decreasing timestamps")
    void status_consecutiveCallsProduceNonDecreasingTimestamps() {
        ResponseEntity<Map<String, Object>> first = mainController.status();
        ResponseEntity<Map<String, Object>> second = mainController.status();

        long ts1 = (Long) first.getBody().get("timestamp");
        long ts2 = (Long) second.getBody().get("timestamp");

        assertThat(ts2).isGreaterThanOrEqualTo(ts1);
    }

    // -------------------------------------------------------------------------
    // apiTest()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api-test — returns 'api-test' view name")
    void apiTest_returnsApiTestViewName() {
        String viewName = mainController.apiTest();

        assertThat(viewName).isEqualTo("api-test");
    }

    // -------------------------------------------------------------------------
    // test()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /test — returns 'Test OK' string")
    void test_returnsTestOkString() {
        String result = mainController.test();

        assertThat(result).isEqualTo("Test OK");
    }

    @Test
    @DisplayName("GET /test — return value is not null")
    void test_returnValueIsNotNull() {
        String result = mainController.test();

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("GET /test — return value is not blank")
    void test_returnValueIsNotBlank() {
        String result = mainController.test();

        assertThat(result).isNotBlank();
    }
}
