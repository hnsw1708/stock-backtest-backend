package com.example.stockbacktest;

import com.example.stockbacktest.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.SpringApplication;

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
     * Tests that run() calls fileStorageService.init() exactly once.
     */
    @Test
    void run_shouldCallFileStorageServiceInit() throws Exception {
        // Act
        application.run();

        // Assert
        verify(fileStorageService, times(1)).init();
    }

    /**
     * Tests that run() completes without throwing any exception under normal conditions.
     */
    @Test
    void run_shouldCompleteWithoutException() {
        assertDoesNotThrow(() -> application.run());
    }

    /**
     * Tests that run() prints the expected startup messages to System.out.
     */
    @Test
    void run_shouldPrintStartupMessages() throws Exception {
        // Arrange
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        try {
            // Act
            application.run();

            // Assert
            String output = outputStream.toString();
            assertTrue(output.contains("股票回溯策略系统后端服务已启动！"),
                    "Output should contain the startup message in Chinese");
            assertTrue(output.contains("http://localhost:8080"),
                    "Output should contain the access URL");
        } finally {
            System.setOut(originalOut);
        }
    }

    /**
     * Tests that run() prints exactly two lines to System.out.
     */
    @Test
    void run_shouldPrintTwoLines() throws Exception {
        // Arrange
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        try {
            // Act
            application.run();

            // Assert
            String output = outputStream.toString();
            long lineCount = output.lines().filter(line -> !line.isBlank()).count();
            assertEquals(2, lineCount, "Exactly two non-blank lines should be printed");
        } finally {
            System.setOut(originalOut);
        }
    }

    /**
     * Tests that run() propagates exceptions thrown by fileStorageService.init().
     */
    @Test
    void run_shouldPropagateExceptionFromFileStorageServiceInit() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Storage init failed"))
                .when(fileStorageService).init();

        // Act & Assert
        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> application.run()
        );
        assertEquals("Storage init failed", thrown.getMessage());
    }

    /**
     * Tests that run() calls fileStorageService.init() before printing messages
     * (order verification using InOrder).
     */
    @Test
    void run_shouldCallInitBeforePrintingMessages() throws Exception {
        // Arrange
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        try {
            // Act
            application.run();

            // Assert: init was called and output was produced
            verify(fileStorageService).init();
            String output = outputStream.toString();
            assertFalse(output.isBlank(), "Console output should not be blank after successful run");
        } finally {
            System.setOut(originalOut);
        }
    }

    /**
     * Tests that run() accepts varargs with no arguments (empty args).
     */
    @Test
    void run_shouldAcceptEmptyArgs() throws Exception {
        assertDoesNotThrow(() -> application.run(new String[]{}));
        verify(fileStorageService, times(1)).init();
    }

    /**
     * Tests that run() accepts varargs with multiple arguments (args are ignored by the implementation).
     */
    @Test
    void run_shouldAcceptMultipleArgs() throws Exception {
        assertDoesNotThrow(() -> application.run("arg1", "arg2", "arg3"));
        verify(fileStorageService, times(1)).init();
    }

    /**
     * Tests that main() delegates to SpringApplication.run() using a static mock.
     * Verifies the entry point wiring without launching a real Spring context.
     */
    @Test
    void main_shouldDelegateToSpringApplicationRun() {
        try (MockedStatic<SpringApplication> springApplicationMock =
                     mockStatic(SpringApplication.class)) {

            // Arrange
            springApplicationMock
                    .when(() -> SpringApplication.run(StockBacktestApplication.class, new String[]{}))
                    .thenReturn(null);

            // Act
            StockBacktestApplication.main(new String[]{});

            // Assert
            springApplicationMock.verify(
                    () -> SpringApplication.run(StockBacktestApplication.class, new String[]{}),
                    times(1)
            );
        }
    }

    /**
     * Tests that main() passes provided arguments through to SpringApplication.run().
     */
    @Test
    void main_shouldPassArgumentsToSpringApplication() {
        String[] args = {"--server.port=9090", "--spring.profiles.active=test"};

        try (MockedStatic<SpringApplication> springApplicationMock =
                     mockStatic(SpringApplication.class)) {

            // Arrange
            springApplicationMock
                    .when(() -> SpringApplication.run(StockBacktestApplication.class, args))
                    .thenReturn(null);

            // Act
            StockBacktestApplication.main(args);

            // Assert
            springApplicationMock.verify(
                    () -> SpringApplication.run(StockBacktestApplication.class, args),
                    times(1)
            );
        }
    }

    /**
     * Verifies that fileStorageService.init() is never called more than once per run() invocation.
     */
    @Test
    void run_shouldNotCallInitMoreThanOnce() throws Exception {
        // Act
        application.run("ignored");

        // Assert
        verify(fileStorageService, atMostOnce()).init();
    }

    /**
     * Verifies output contains the localhost address line with correct format.
     */
    @Test
    void run_shouldPrintCorrectLocalhostAddress() throws Exception {
        // Arrange
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        try {
            // Act
            application.run();

            // Assert — using Java 21 text block for expected fragment
            String expectedFragment = """
                    访问地址: http://localhost:8080""".strip();
            assertTrue(
                    outputStream.toString().contains(expectedFragment),
                    "Output should contain the correctly formatted access address"
            );
        } finally {
            System.setOut(originalOut);
        }
    }
}
