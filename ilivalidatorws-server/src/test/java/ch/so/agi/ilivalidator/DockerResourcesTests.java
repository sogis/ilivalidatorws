package ch.so.agi.ilivalidator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.output.Slf4jLogConsumer;

@Tag("docker")
@Testcontainers
@TestInstance(Lifecycle.PER_CLASS)
public class DockerResourcesTests extends ResourcesTests {
    private static int exposedPort = 8080;

    @Container
    public static GenericContainer<?> ilivalidatorWebService = new GenericContainer<>("sogis/ilivalidator-web-service:latest")
            .waitingFor(Wait.forHttp("/actuator/health"))
            .withEnv("JDBC_URL", "jdbc:sqlite:./jobrunr_db.sqlite")
            .withEnv("DOC_BASE", "/tmp/")
            .withEnv("WORK_DIRECTORY", "/tmp/")
            .withExposedPorts(exposedPort)
            .withLogConsumer(new Slf4jLogConsumer(logger));

    @BeforeAll
    public void setup() {
        port = String.valueOf(ilivalidatorWebService.getMappedPort(exposedPort));
    }
}
