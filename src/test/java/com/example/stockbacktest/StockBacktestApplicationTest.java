package com.example.stockbacktest;

import com.example.stockbacktest.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.SpringApplication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockBacktestApplicationTest {

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private StockBacktestApplication stockBacktestApplication;

    /**
     * Tests that run() calls fileStorageService.init() exactly once.
     */
    @Test
    void run_shouldCallFileStorageServiceInit() throws Exception {
        // when
        stockBacktestApplication.run();

        // then
        verify(fileStorageService, times(1)).init();
    }

    /**
     * Tests that run() completes without throwing any exception under normal conditions.
     */
    @Test
    void run_shouldNotThrowException() {
        assertDoesNotThrow(() -> stockBacktestApplication.run());
    }

    /**
     * Tests that run() propagates exceptions thrown by fileStorageService.init().
     */
    @Test
    void run_shouldPropagateExceptionFromFileStorageServiceInit() throws Exception {
        // given
        doThrow(new RuntimeException("Storage init failed"))
                .when(fileStorageService).init();

        // when / then
        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> stockBacktestApplication.run()
        );
        assertEquals("Storage init failed", thrown.getMessage());
        verify(fileStorageService, times(1)).init();
    }

    /**
     * Tests that run() works correctly when called with no arguments (empty varargs).
     */
    @Test
    void run_withNoArgs_shouldCallFileStorageServiceInit() throws Exception {
        // when
        stockBacktestApplication.run(new String[0]);

        // then
        verify(fileStorageService, times(1)).init();
    }

    /**
     * Tests that run() works correctly when called with multiple string arguments.
     */
    @Test
    void run_withMultipleArgs_shouldCallFileStorageServiceInit() throws Exception {
        // when
        stockBacktestApplication.run("arg1", "arg2", "arg3");

        // then
        verify(fileStorageService, times(1)).init();
    }

    /**
     * Tests that run() calls fileStorageService.init() before any console output.
     * Verifies ordering via InOrder.
     */
    @Test
    void run_shouldCallInitBeforeOtherOperations() throws Exception {
        // given
        var inOrder = inOrder(fileStorageService);

        // when
        stockBacktestApplication.run();

        // then
        inOrder.verify(fileStorageService).init();
        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Tests that the main method delegates to SpringApplication.run without error.
     * Uses MockedStatic to avoid actually starting a Spring context.
     */
    @Test
    void main_shouldDelegateToSpringApplication() {
        try (MockedStatic<SpringApplication> mockedSpring = mockStatic(SpringApplication.class)) {
            // given
            mockedSpring.when(() -> SpringApplication.run(
                    StockBacktestApplication.class, new String[]{}))
            .thenReturn(null);

            // when
            assertDoesNotThrow(() -> StockBacktestApplication.main(new String[]{}));

            // then
            mockedSpring.verify(() ->
                    SpringApplication.run(StockBacktestApplication.class, new String[]{}),
                    times(1)
            );
        }
    }

    /**
     * Tests that main() passes supplied args to SpringApplication.run.
     */
    @Test
    void main_shouldPassArgsToSpringApplication() {
        var args = new String[]{"--server.port=9090", "--spring.profiles.active=test"};

        try (MockedStatic<SpringApplication> mockedSpring = mockStatic(SpringApplication.class)) {
            mockedSpring.when(() -> SpringApplication.run(
                    StockBacktestApplication.class, args))
            .thenReturn(null);

            // when
            assertDoesNotThrow(() -> StockBacktestApplication.main(args));

            // then
            mockedSpring.verify(() ->
                    SpringApplication.run(StockBacktestApplication.class, args),
                    times(1)
            );
        }
    }

    /**
     * Tests that run() does not interact with fileStorageService more than once
     * (guards against accidental double-initialisation).
     */
    @Test
    void run_shouldNotCallInitMoreThanOnce() throws Exception {
        // when
        stockBacktestApplication.run();

        // then — init is called exactly once, not more
        verify(fileStorageService, times(1)).init();
        verify(fileStorageService, atMostOnce()).init();
    }

    /**
     * Tests that run() handles a RuntimeException thrown by fileStorageService.init().
     */
    @Test
    void run_shouldPropagateCheckedExceptionFromInit() throws Exception {
        // given
        doThrow(new RuntimeException("Checked storage error"))
                .when(fileStorageService).init();

        // when / then
        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> stockBacktestApplication.run()
        );
        assertEquals("Checked storage error", thrown.getMessage());
    }
}