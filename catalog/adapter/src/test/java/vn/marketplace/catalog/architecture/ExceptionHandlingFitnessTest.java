package vn.marketplace.catalog.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Exception-Handling fitness function: REST controllers must NOT wrap calls in a generic
 * {@code try/catch (Exception) -> HTTP 500}. They must let domain exceptions propagate to msfw's
 * GlobalExceptionHandler. Verified by scanning controller source (try/catch is not visible to ArchUnit).
 */
class ExceptionHandlingFitnessTest {

    private static final Path CONTROLLER_DIR = Path.of(
            "src/main/java/vn/marketplace/catalog/adapter/product/management/inbound/restful");

    @Test
    void controllersDoNotSwallowExceptionsIntoInternalServerError() throws IOException {
        try (Stream<Path> paths = Files.walk(CONTROLLER_DIR)) {
            List<Path> controllers = paths
                    .filter(p -> p.getFileName().toString().endsWith("Controller.java"))
                    .toList();

            assertFalse(controllers.isEmpty(), "expected to find controller sources to scan");

            for (Path controller : controllers) {
                String source = Files.readString(controller);
                assertFalse(source.contains("catch (Exception"),
                        controller + " must not catch a generic Exception");
                assertFalse(source.contains("catch (Throwable"),
                        controller + " must not catch Throwable");
                assertFalse(source.contains("INTERNAL_SERVER_ERROR"),
                        controller + " must not return HTTP 500 directly; let GlobalExceptionHandler map it");
            }
        }
    }

    @Test
    void atLeastOneControllerWasScanned() throws IOException {
        try (Stream<Path> paths = Files.walk(CONTROLLER_DIR)) {
            assertTrue(paths.anyMatch(p -> p.getFileName().toString().endsWith("Controller.java")));
        }
    }
}
