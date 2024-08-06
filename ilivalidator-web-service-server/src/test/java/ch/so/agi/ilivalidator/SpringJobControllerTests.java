package ch.so.agi.ilivalidator;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@TestInstance(Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SpringJobControllerTests extends JobControllerTests {
    
    public static GenericContainer<?> repoContainer = new GenericContainer<>(DockerImageName.parse("sogis/interlis-repository-test:local"))
        .waitingFor(Wait.forHttp("/ilimodels.xml"))
        .withExposedPorts(8080)
        .withLogConsumer(new Slf4jLogConsumer(logger));
    // Andere Variante. Benötigt @Container Annotation. Dafür muss der Container nicht manuell gestartet werden.
//        .withCreateContainerCmdModifier(cmd -> 
//            cmd.withHostConfig(new HostConfig().withPortBindings(new PortBinding(Ports.Binding.bindPort(8083), new ExposedPort(8080)))));

    @BeforeAll
    public void startContainers() {
        repoContainer.setPortBindings(List.of("8083:8080"));   
        repoContainer.start();       
    }
    
    @AfterAll
    public void stopContainers() {
       repoContainer.stop();
    }
}
