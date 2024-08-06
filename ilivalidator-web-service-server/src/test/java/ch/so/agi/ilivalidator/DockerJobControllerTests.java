package ch.so.agi.ilivalidator;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.containers.output.Slf4jLogConsumer;

//@Testcontainers
@TestInstance(Lifecycle.PER_CLASS)
@Tag("docker")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class DockerJobControllerTests extends JobControllerTests {
    private static int exposedPort = 8080;

    static Network network = Network.newNetwork();

    public static GenericContainer<?> repoContainer = new GenericContainer<>(DockerImageName.parse("sogis/interlis-repository-test:local"))
            .waitingFor(Wait.forHttp("/ilimodels.xml"))
            .withExposedPorts(8080) // Braucht es nicht für die Kommunikation zwischen den Container. Für den "Wait"-Check muss ein Port jedoch exposed sein.
            .withNetwork(network)
            .withNetworkAliases("repo")
            .withLogConsumer(new Slf4jLogConsumer(logger));

    
    public static GenericContainer<?> appContainer = new GenericContainer<>(DockerImageName.parse("sogis/ilivalidator-web-service:latest"))
            .waitingFor(Wait.forHttp("/actuator/health"))
            .withEnv("JDBC_URL", "jdbc:sqlite:/tmp/jobrunr_db.sqlite")
            .withEnv("WORK_DIRECTORY", "/tmp/")
            .withEnv("ILIDIRS", "http://repo:8080")
            .withNetwork(network)
            .withExposedPorts(8080)
            .withLogConsumer(new Slf4jLogConsumer(logger));

    @BeforeAll
    public void startContainers() {
        repoContainer.start();
        
        appContainer.start();
        
        // Damit die Tests den zufälligen Port des ilivalidator-web-services kennen und mit ihm
        // kommunizieren können.
        port = String.valueOf(appContainer.getMappedPort(exposedPort)); 
    }
    
    @AfterAll
    public void stopContainers() {
        appContainer.stop();
        repoContainer.stop();
    }

}
