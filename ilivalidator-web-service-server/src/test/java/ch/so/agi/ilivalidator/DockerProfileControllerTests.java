package ch.so.agi.ilivalidator;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.databind.ObjectMapper;

@TestInstance(Lifecycle.PER_CLASS)
@Tag("docker")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class DockerProfileControllerTests extends ProfileControllerTests {
    private static int exposedPort = 8080;

    public static GenericContainer<?> appContainer = new GenericContainer<>(DockerImageName.parse("sogis/ilivalidator-web-service:latest"))
            .waitingFor(Wait.forHttp("/actuator/health"))
            .withEnv("JDBC_URL", "jdbc:sqlite:/tmp/jobrunr_db.sqlite")
            .withEnv("WORK_DIRECTORY", "/tmp/")
            .withExposedPorts(8080)
            .withLogConsumer(new Slf4jLogConsumer(logger));

    public DockerProfileControllerTests(@Autowired TestRestTemplate restTemplate, @Autowired ObjectMapper mapper) {
        super(restTemplate, mapper);
    }
    
    @BeforeAll
    public void startContainers() {        
        appContainer.start();
        
        // Damit die Tests den zufälligen Port des ilivalidator-web-services kennen und mit ihm
        // kommunizieren können.
        port = String.valueOf(appContainer.getMappedPort(exposedPort)); 
    }
    
    @AfterAll
    public void stopContainers() {
        appContainer.stop();
    }
}
