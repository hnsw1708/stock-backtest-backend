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

    /**
     * Verifies that run() calls fileStorageService.init() exactly once.
     */
    @Test
    void run_shouldCallFileStorageServiceInit() throws Exception {
        application.run();
        verify(fileStorageService, times(1)).init();
    }

    /**
     * Verifies that run() completes without throwing any exception
     * under normal conditions.
     */
    @Test
    void run_shouldNotThrowWhenInitSucceeds() {
        assertDoesNotThrow(() -> application.run());
    }

    /**
     * Verifies that run() propagates exceptions thrown by fileStorageService.init().
     */
    @Test
    void run_shouldPropagateExceptionWhenInitFails() throws Exception {
        doThrow(new RuntimeException("Storage init failed"))
                .when(fileStorageService).init();

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> application.run()
        );
        assertEquals("Storage init failed", ex.getMessage());
    }

    /**
     * Verifies that run() propagates checked exceptions thrown by fileStorageService.init().
     */
    @Test
    void run_shouldPropagateCheckedExceptionWhenInitFails() throws Exception {
        doThrow(new Exception("Checked storage error"))
                .when(fileStorageService).init();

        Exception ex = assertThrows(
                Exception.class,
                () -> application.run()
        );
        assertEquals("Checked storage error", ex.getMessage());
    }

    /**
     * Verifies that run() prints the expected startup messages to stdout.
     */
    @Test
    void run_shouldPrintStartupMessages() throws Exception {
        // Capture System.out
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos, true, "UTF-8"));

        try {
            application.run();
        } finally {
            System.setOut(originalOut);
        }

        String output = baos.toString("UTF-8");

        // Use a Java 21 text block for the expected snippets to improve clarity
        String expectedLine1 = "股票回溯策略系统后端服务已启动！";
        String expectedLine2 = "访问地址: http://localhost:8080";

        assertTrue(output.contains(expectedLine1),
                () -> "Output should contain startup message, but was: " + output);
        assertTrue(output.contains(expectedLine2),
                () -> "Output should contain access URL, but was: " + output);
    }

    /**
     * Verifies that run() accepts and ignores extra vararg arguments
     * (simulating command-line args being passed).
     */
    @Test
    void run_shouldAcceptArbitraryArgs() throws Exception {
        assertDoesNotThrow(() -> application.run("--server.port=9090", "--spring.profiles.active=test"));
        verify(fileStorageService, times(1)).init();
    }

    /**
     * Verifies that run() works correctly when called with null varargs.
     */
    @Test
    void run_shouldHandleNullArgs() throws Exception {
        assertDoesNotThrow(() -> application.run((String[]) null));
        verify(fileStorageService, times(1)).init();
    }

    /**
     * Verifies that run() works correctly when called with an empty args array.
     */
    @Test
    void run_shouldHandleEmptyArgs() throws Exception {
        assertDoesNotThrow(() -> application.run(new String[]{}));
        verify(fileStorageService, times(1)).init();
    }

    /**
     * Verifies idempotent behaviour: calling run() multiple times
     * invokes init() the corresponding number of times.
     */
    @Test
    void run_calledMultipleTimes_shouldCallInitEachTime() throws Exception {
        application.run();
        application.run();
        application.run();

        verify(fileStorageService, times(3)).init();
    }
}
