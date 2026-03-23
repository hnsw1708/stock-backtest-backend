package com.example.stockbacktest;

import com.example.stockbacktest.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockBacktestApplicationTest {

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private StockBacktestApplication application;

    // -----------------------------------------------------------------------
    // run() — happy path
    // -----------------------------------------------------------------------

    @Test
    void run_shouldCallFileStorageServiceInit() throws Exception {
        // Act
        application.run();

        // Assert
        verify(fileStorageService, times(1)).init();
    }

    @Test
    void run_shouldPrintStartupMessages() throws Exception {
        // Arrange — capture stdout
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(captured));

        try {
            // Act
            application.run();
        } finally {
            System.setOut(original);
        }

        // Assert — use a text block for expected fragment matching
        String output = captured.toString();
        String expectedLine1 = "股票回溯策略系统后端服务已启动！";
        String expectedLine2 = "访问地址: http://localhost:8080";

        assertTrue(output.contains(expectedLine1),
                "Output should contain the Chinese startup message");
        assertTrue(output.contains(expectedLine2),
                "Output should contain the localhost URL");
    }

    @Test
    void run_withVarArgs_shouldStillCallInit() throws Exception {
        // Act — pass arbitrary arguments (mirrors real CLI invocation)
        application.run("--server.port=9090", "--spring.profiles.active=test");

        // Assert
        verify(fileStorageService, times(1)).init();
    }

    @Test
    void run_withEmptyArgs_shouldCallInit() throws Exception {
        // Act
        application.run(new String[0]);

        // Assert
        verify(fileStorageService, times(1)).init();
    }

    // -----------------------------------------------------------------------
    // run() — error propagation
    // -----------------------------------------------------------------------

    @Test
    void run_whenInitThrowsException_shouldPropagateException() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Storage init failed"))
                .when(fileStorageService).init();

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> application.run());

        assertEquals("Storage init failed", ex.getMessage());
    }

    @Test
    void run_whenInitThrowsCheckedException_shouldPropagateAsException() throws Exception {
        // Arrange
        Exception cause = new Exception("Checked storage error");
        doThrow(cause).when(fileStorageService).init();

        // Act & Assert
        Exception thrown = assertThrows(Exception.class,
                () -> application.run());

        assertEquals("Checked storage error", thrown.getMessage());
    }

    // -----------------------------------------------------------------------
    // run() — interaction ordering
    // -----------------------------------------------------------------------

    @Test
    void run_initCalledBeforePrintMessages() throws Exception {
        // Arrange
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(captured));

        // Track whether init was called; output should appear only after init
        doAnswer(invocation -> {
            // At the moment init() is called, nothing should have been printed yet
            String outputSoFar = captured.toString();
            assertTrue(outputSoFar.isEmpty(),
                    "No output expected before init() is called");
            return null;
        }).when(fileStorageService).init();

        try {
            application.run();
        } finally {
            System.setOut(original);
        }

        // Verify init was actually invoked (the doAnswer above confirms ordering)
        verify(fileStorageService, times(1)).init();
    }

    // -----------------------------------------------------------------------
    // run() — idempotency / multiple calls
    // -----------------------------------------------------------------------

    @Test
    void run_calledTwice_shouldCallInitTwice() throws Exception {
        // Act
        application.run();
        application.run();

        // Assert
        verify(fileStorageService, times(2)).init();
    }

    // -----------------------------------------------------------------------
    // Structural / sanity checks
    // -----------------------------------------------------------------------

    @Test
    void application_shouldBeCommandLineRunner() {
        // The production class must implement CommandLineRunner
        assertInstanceOf(org.springframework.boot.CommandLineRunner.class, application);
    }

    @Test
    void application_shouldCarrySpringBootApplicationAnnotation() {
        boolean annotated = StockBacktestApplication.class
                .isAnnotationPresent(org.springframework.boot.autoconfigure.SpringBootApplication.class);
        assertTrue(annotated,
                "StockBacktestApplication must be annotated with @SpringBootApplication");
    }
}
