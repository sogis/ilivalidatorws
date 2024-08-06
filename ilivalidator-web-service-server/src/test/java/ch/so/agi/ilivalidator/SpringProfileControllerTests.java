package ch.so.agi.ilivalidator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SpringProfileControllerTests extends ProfileControllerTests {

    public SpringProfileControllerTests(@Autowired TestRestTemplate restTemplate, @Autowired ObjectMapper mapper) {
        super(restTemplate, mapper);
    }
}
