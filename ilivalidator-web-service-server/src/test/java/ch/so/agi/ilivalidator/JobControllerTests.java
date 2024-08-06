package ch.so.agi.ilivalidator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.awaitility.Awaitility.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.so.agi.ilivalidator.job.JobResponse;

public abstract class JobControllerTests {
    static Logger logger = LoggerFactory.getLogger(JobControllerTests.class);

    @LocalServerPort
    protected String port;
    
    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected ObjectMapper mapper;
    
    private String API_ENDPOINT_JOBS = "/api/jobs";
    private String OPERATION_LOCATION_HEADER = "Operation-Location";
    private int RESULT_POLL_DELAY = 5; // seconds
    private int RESULT_POLL_INTERVAL = 5; // seconds
    private int RESULT_WAIT = 5; // minutes
    
    // Prüft, ob eine INTERLIS2-Datei validiert werden kann.
    // Die Datei ist modellkonform. Es dürfen keine Fehler
    // gefunden werden.
    @Test
    public void validate_File_Interlis2_Ok() throws Exception {        
        String serverUrl = "http://localhost:"+port+API_ENDPOINT_JOBS;

        MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<String, Object>();
        parameters.add("files", new FileSystemResource("src/test/data/ch.so.avt.kunstbauten.xtf"));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "multipart/form-data");
        headers.set("Accept", "text/plain");

        // Datei hochladen und Response-Status-Code auswerten
        ResponseEntity<String> postResponse = restTemplate.postForEntity(
                serverUrl, new HttpEntity<MultiValueMap<String, Object>>(parameters, headers), String.class);

        assertEquals(202, postResponse.getStatusCode().value());
        
        // Warten, bis die Validierung durch ist (=SUCCEEDED)
        String operationLocation = postResponse.getHeaders().toSingleValueMap().get(OPERATION_LOCATION_HEADER);

        await()
            .with().pollInterval(RESULT_POLL_INTERVAL, TimeUnit.SECONDS)
            .and()
            .with().atMost(RESULT_WAIT, TimeUnit.MINUTES)
            .until(new MyCallable(operationLocation, restTemplate));

        // Logfile herunterladen und auswerten
        ResponseEntity<JobResponse> jobResponse = restTemplate.getForEntity(operationLocation, JobResponse.class);        
        
        URL logFileUrl = new URL(jobResponse.getBody().logFileLocation());
        String logFileContents = null;
        try (InputStream in = logFileUrl.openStream()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            logFileContents = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
        assertTrue(logFileContents.contains("Info: ...validation done"));
    }
 
    // Prüft, ob eine INTERLIS2-Datei validiert werden kann.
    // Die Datei ist nicht modellkonform. Es müssen Fehler
    // gefunden werden.
    @Test
    public void validate_File_Interlis2_Fail() throws Exception {        
        String serverUrl = "http://localhost:"+port+API_ENDPOINT_JOBS;

        MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<String, Object>();
        parameters.add("files", new FileSystemResource("src/test/data/error_VOLLZUG_SO0300002511_1153_20210329115028.xml"));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "multipart/form-data");
        headers.set("Accept", "text/plain");

        // Datei hochladen und Response-Status-Code auswerten
        ResponseEntity<String> postResponse = restTemplate.postForEntity(
                serverUrl, new HttpEntity<MultiValueMap<String, Object>>(parameters, headers), String.class);

        assertEquals(202, postResponse.getStatusCode().value());
        
        // Warten, bis die Validierung durch ist (=SUCCEEDED)
        String operationLocation = postResponse.getHeaders().toSingleValueMap().get(OPERATION_LOCATION_HEADER);

        await()
            .with().pollInterval(RESULT_POLL_INTERVAL, TimeUnit.SECONDS)
            .and()
            .with().atMost(RESULT_WAIT, TimeUnit.MINUTES)
            .until(new MyCallable(operationLocation, restTemplate));

        // Logfile herunterladen und auswerten
        ResponseEntity<JobResponse> jobResponse = restTemplate.getForEntity(operationLocation, JobResponse.class);        
        assertEquals("FAILED", jobResponse.getBody().validationStatus());
        
        URL logFileUrl = new URL(jobResponse.getBody().logFileLocation());
        String logFileContents = null;
        try (InputStream in = logFileUrl.openStream()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            logFileContents = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
        
        assertTrue(logFileContents.contains("Attribute Mutationsnummer[0]/NBIdent is length restricted to 12"));
        assertTrue(logFileContents.contains("Info: ...validation failed"));
    }
    
    // Es wird geprüft, ob Zusatzfunktionen verwendet werden können.
    // Dazu wird die Prüfung mit einem Prüfprofile aufgerufen.
    // Siehe https://geo.so.ch/models/ARP/SO_Nutzungsplanung_20171118_Validierung_20231101.ili
    // Die Transferdatei weist Fehler auf und es müssen Fehler gefunden
    // werden.
    @Test
    public void validation_File_CustomFunctions_Fail() throws Exception {
        String serverUrl = "http://localhost:"+port+API_ENDPOINT_JOBS;

        MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<String, Object>();
        parameters.add("files", new FileSystemResource("src/test/data/2457_Messen_vorher.xtf"));
        parameters.add("profile", "ilidata:SO_Nutzungsplanung_20171118_20231101-meta");
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "multipart/form-data");
        headers.set("Accept", "text/plain");

        // Datei hochladen und Response-Status-Code auswerten
        ResponseEntity<String> postResponse = restTemplate.postForEntity(
                serverUrl, new HttpEntity<MultiValueMap<String, Object>>(parameters, headers), String.class);

        assertEquals(202, postResponse.getStatusCode().value());
        
        // Warten, bis die Validierung durch ist (=SUCCEEDED)
        String operationLocation = postResponse.getHeaders().toSingleValueMap().get(OPERATION_LOCATION_HEADER);
        
        await()
            .with().pollDelay(RESULT_POLL_DELAY, TimeUnit.SECONDS).pollInterval(RESULT_POLL_INTERVAL, TimeUnit.SECONDS)
            .and()
            .with().atMost(RESULT_WAIT, TimeUnit.MINUTES)
            .until(new MyCallable(operationLocation, restTemplate));

        // Logfile herunterladen und auswerten
        ResponseEntity<JobResponse> jobResponse = restTemplate.getForEntity(operationLocation, JobResponse.class);        
        
        assertEquals("SUCCEEDED", jobResponse.getBody().jobStatus());
        assertEquals("FAILED", jobResponse.getBody().validationStatus());

        URL logFileUrl = new URL(jobResponse.getBody().logFileLocation());

        String logFileContents = null;
        try (InputStream in = logFileUrl.openStream()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            logFileContents = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
        
        assertTrue(logFileContents.contains("ilidata:SO_Nutzungsplanung_20171118_20231101-meta"));
        assertTrue(logFileContents.contains("Warning: line 5: SO_Nutzungsplanung_20171118.Rechtsvorschriften.Dokument: tid d3c20374-f6c5-48f9-8e1e-232b87a9d80a: invalid format of INTERLIS.URI value <34-Messen/Entscheide/34-36_45-E.pdf> in attribute TextImWeb"));
        assertTrue(logFileContents.contains("Error: line 61: SO_Nutzungsplanung_20171118.Rechtsvorschriften.HinweisWeitereDokumente: tid 6: Association SO_Nutzungsplanung_20171118.Rechtsvorschriften.HinweisWeitereDokumente must not have an OID (6)"));
        assertTrue(logFileContents.contains("Error: line 412: SO_Nutzungsplanung_20171118.Nutzungsplanung.Grundnutzung: tid 2d285daf-a5ab-4106-a453-58eef2e921ab: duplicate coord at (2599932.281, 1216063.38, NaN)"));
        assertTrue(logFileContents.contains("Error: line 140: SO_Nutzungsplanung_20171118.Nutzungsplanung.Typ_Ueberlagernd_Flaeche: tid 0723a0c8-46e4-4e4f-aba4-c75e90bece14: Attributwert Verbindlichkeit ist nicht identisch zum Objektkatalog: 'orientierend' - '6110'"));
        assertTrue(logFileContents.contains("Dokument '24-Brunnenthal/Reglemente/024_BZR.pdf' wurde nicht gefunden"));
        assertTrue(logFileContents.contains("Error: SO_Nutzungsplanung_20171118.Nutzungsplanung.Ueberlagernd_Flaeche: tid 7478a32c-45d6-4b3f-8507-0b9f4bd308bf/Geometrie[1]: Intersection coord1 (2600228.240, 1217472.518), tids 7478a32c-45d6-4b3f-8507-0b9f4bd308bf/Geometrie[1], 9b8a1966-1482-4b1f-b576-968f4246e80a/Geometrie[1]"));
        assertTrue(logFileContents.contains("Error: Set Constraint SO_Nutzungsplanung_20171118_Validierung_20231101.Nutzungsplanung_Validierung.v_Ueberlagernd_Flaeche.laermempfindlichkeitsAreaCheck is not true"));
        assertTrue(logFileContents.contains("Info: validate set constraint SO_Nutzungsplanung_20171118_Validierung_20231101.Rechtsvorschriften_Validierung.v_HinweisWeitereDokumente.isValidDocumentsCycle..."));
        assertTrue(logFileContents.contains("Error: line 61: SO_Nutzungsplanung_20171118.Rechtsvorschriften.HinweisWeitereDokumente: tid 6: self loop found: 95efebb8-24df-4462-9af1-15500e341f04"));       
        assertTrue(logFileContents.contains("Info: ...validation failed"));
    }

    // Prüfen, ob die Naturgefahren-Konfiguration greift:
    // - metaConfig
    // - Zusatzfunktionen
    @Test
    public void validation_File_Naturgefahren_Fail() throws Exception {
        String serverUrl = "http://localhost:"+port+API_ENDPOINT_JOBS;

        MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<String, Object>();
        parameters.add("files", new FileSystemResource("src/test/data/NGK_SO_Testbeddata.xtf"));
        parameters.add("profile", "ilidata:SO_AFU_Naturgefahren_20240515-web-meta");
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "multipart/form-data");
        headers.set("Accept", "text/plain");

        // Datei hochladen und Response-Status-Code auswerten
        ResponseEntity<String> postResponse = restTemplate.postForEntity(
                serverUrl, new HttpEntity<MultiValueMap<String, Object>>(parameters, headers), String.class);

        assertEquals(202, postResponse.getStatusCode().value());

        // Warten, bis die Validierung durch ist (=SUCCEEDED)
        String operationLocation = postResponse.getHeaders().toSingleValueMap().get(OPERATION_LOCATION_HEADER);
        
        await()
            .with().pollDelay(RESULT_POLL_DELAY, TimeUnit.SECONDS).pollInterval(RESULT_POLL_INTERVAL, TimeUnit.SECONDS)
            .and()
            .with().atMost(RESULT_WAIT, TimeUnit.MINUTES)
            .until(new MyCallable(operationLocation, restTemplate));

        // Logfile herunterladen und auswerten
        ResponseEntity<JobResponse> jobResponse = restTemplate.getForEntity(operationLocation, JobResponse.class);        
        
        assertEquals("SUCCEEDED", jobResponse.getBody().jobStatus());
        assertEquals("FAILED", jobResponse.getBody().validationStatus());

        URL logFileUrl = new URL(jobResponse.getBody().logFileLocation());

        String logFileContents = null;
        try (InputStream in = logFileUrl.openStream()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            logFileContents = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
        assertTrue(logFileContents.contains("metaConfigFile <ilidata:SO_AFU_Naturgefahren_20240515-web-meta>"));
        assertTrue(logFileContents.contains("modelNames <SO_AFU_Naturgefahren_20240515>"));
        assertTrue(logFileContents.contains("Der Befund Jaehrlichkeit muss einem Teilauftrag des Hauptprozess-Typs 'Sturz' zugeordnet sein"));
        assertTrue(logFileContents.contains("Info: ...validation failed"));
        assertFalse(logFileContents.contains("is not yet implemented"));
    }

    // Prüft, ob ein Fehler geworfen wird, wenn eine Transferdatei
    // mit einem falschen Profile geprüft wird.
    @Test
    public void validation_File_WrongProfile_Fail() throws Exception {
        String serverUrl = "http://localhost:"+port+API_ENDPOINT_JOBS;

        MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<String, Object>();
        parameters.add("files", new FileSystemResource("src/test/data/ch.so.avt.kunstbauten.xtf"));        
        parameters.add("profile", "ilidata:SO_Nutzungsplanung_20171118_20231101-meta");
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "multipart/form-data");
        headers.set("Accept", "text/plain");

        // Datei hochladen und Response-Status-Code auswerten
        ResponseEntity<String> postResponse = restTemplate.postForEntity(
                serverUrl, new HttpEntity<MultiValueMap<String, Object>>(parameters, headers), String.class);

        assertEquals(202, postResponse.getStatusCode().value());
        
        // Warten, bis die Validierung durch ist (=SUCCEEDED)
        String operationLocation = postResponse.getHeaders().toSingleValueMap().get(OPERATION_LOCATION_HEADER);
        
        await()
            .with().pollDelay(RESULT_POLL_DELAY, TimeUnit.SECONDS).pollInterval(RESULT_POLL_INTERVAL, TimeUnit.SECONDS)
            .and()
            .with().atMost(RESULT_WAIT, TimeUnit.MINUTES)
            .until(new MyCallable(operationLocation, restTemplate));

        // Logfile herunterladen und auswerten
        ResponseEntity<JobResponse> jobResponse = restTemplate.getForEntity(operationLocation, JobResponse.class);        
        
        assertEquals("SUCCEEDED", jobResponse.getBody().jobStatus());
        assertEquals("FAILED", jobResponse.getBody().validationStatus());

        URL logFileUrl = new URL(jobResponse.getBody().logFileLocation());

        String logFileContents = null;
        try (InputStream in = logFileUrl.openStream()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            logFileContents = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
        
        assertTrue(logFileContents.contains("ilidata:SO_Nutzungsplanung_20171118_20231101-meta"));
        assertTrue(logFileContents.contains("modelNames <SO_Nutzungsplanung_20171118>"));
        assertTrue(logFileContents.contains("Error: SO_AVT_Kunstbauten_Publikation_20220207.Kunstbauten: tid SO_AVT_Kunstbauten_Publikation_20220207.Kunstbauten: Invalid basket element name SO_AVT_Kunstbauten_Publikation_20220207.Kunstbauten"));
        assertTrue(logFileContents.contains("Error: line 5: SO_AVT_Kunstbauten_Publikation_20220207.Kunstbauten.Kunstbaute: tid f0cc0e37-9db7-492e-8ad7-9df33585939e: unknown class <SO_AVT_Kunstbauten_Publikation_20220207.Kunstbauten.Kunstbaute>"));
    }

    public class MyCallable implements Callable<Boolean> {

        private final String operationLocation;
        private final TestRestTemplate restTemplate;
        private String jobStatus;
           
        public MyCallable(String operationLocation, TestRestTemplate restTemplate, String jobStatus) {
            this.operationLocation = operationLocation;
            this.restTemplate = restTemplate;
            this.jobStatus = jobStatus;
        }

        public MyCallable(String operationLocation, TestRestTemplate restTemplate) {
            this.operationLocation = operationLocation;
            this.restTemplate = restTemplate;
            this.jobStatus = "SUCCEEDED";
        }
        
        @Override
        public Boolean call() throws Exception {
            logger.info("*******************************************************");
            logger.info("polling: {}", operationLocation);
            logger.info("*******************************************************");
            ResponseEntity<JobResponse> jobResponse = restTemplate.getForEntity(operationLocation, JobResponse.class);
            return jobResponse.getBody().jobStatus().equalsIgnoreCase(this.jobStatus) ? true : false;            
        }        
    }
}


