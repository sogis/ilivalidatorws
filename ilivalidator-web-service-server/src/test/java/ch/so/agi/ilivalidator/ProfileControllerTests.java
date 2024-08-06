package ch.so.agi.ilivalidator;

import static org.awaitility.Awaitility.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

import org.junit.jupiter.api.Test;

public abstract class ProfileControllerTests {
    protected static Logger logger = LoggerFactory.getLogger(ProfileControllerTests.class);

    @LocalServerPort
    protected String port;
    
    protected TestRestTemplate restTemplate;

    protected ObjectMapper mapper;
    
    private String API_ENDPOINT_PROFILES = "/api/profiles";

    public ProfileControllerTests(TestRestTemplate restTemplate, ObjectMapper mapper) {
        this.restTemplate = restTemplate;
        this.mapper = mapper;
    }
    
    @Test
    public void getValidationProfiles_Ok() throws Exception {
        String serverUrl = "http://localhost:"+port+API_ENDPOINT_PROFILES;

        ResponseEntity<String> response = restTemplate.getForEntity(serverUrl, String.class);
        Map<String, Object> body = mapper.readValue(response.getBody(), Map.class);
        Map<String, String> profiles = (Map<String, String>) body.get("profiles");
        
        assertEquals(200, response.getStatusCode().value());
        assertEquals("ilidata:VSADSSMINI_2020_LV95_Drainage_20230731-meta", profiles.get("Drainagen"));
    }
}
