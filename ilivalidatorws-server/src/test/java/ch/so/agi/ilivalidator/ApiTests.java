package ch.so.agi.ilivalidator;

import org.junit.jupiter.api.Disabled;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ch.so.agi.ilivalidator.model.JobResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.awaitility.Awaitility.*;

public abstract class ApiTests {
    static Logger logger = LoggerFactory.getLogger(ApiTests.class);

    @LocalServerPort
    protected String port;

    @Autowired
    protected TestRestTemplate restTemplate;
    
    private String REST_ENDPOINT = "/api/jobs/";
    private String OPERATION_LOCATION_HEADER = "Operation-Location";
    //private String RETRY_AFTER_HEADER = "Retry-After";
    private int RESULT_POLL_DELAY = 5; // seconds
    private int RESULT_POLL_INTERVAL = 5; // seconds
    private int RESULT_WAIT = 5; // minutes
    
    @Test
    public void validation_Ok_Interlis1File() throws Exception {
        String serverUrl = "http://localhost:"+port+REST_ENDPOINT;

        MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<String, Object>();
        parameters.add("files", new FileSystemResource("src/test/data/2549.ch.so.agi.av.dm01_ch.itf"));

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
    
    // Dateien mit externer Rolle. 
    // Dateien sind fehlerfrei und standardmässig ist "allObjectsAccessible=true" gesetzt.
    @Test
    public void validation_Ok_Interlis2Files() throws Exception {
        String serverUrl = "http://localhost:"+port+REST_ENDPOINT;

        MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<String, Object>();
        parameters.add("files", new FileSystemResource("src/test/data/OeREBKRM_V2_0_Gesetze.xml"));
        parameters.add("files", new FileSystemResource("src/test/data/OeREBKRM_V2_0_Themen.xml"));

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
        
        assertEquals("SUCCEEDED", jobResponse.getBody().status());
        assertEquals("SUCCEEDED", jobResponse.getBody().validationStatus());
        
        URL logFileUrl = new URL(jobResponse.getBody().logFileLocation());

        String logFileContents = null;
        try (InputStream in = logFileUrl.openStream()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            logFileContents = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
        assertFalse(logFileContents.contains("Info: assume unknown external objects"));
        assertTrue(logFileContents.contains("Info: ...validation done"));
    } 

    // Dateien mit externer Rolle. 
    // Dateien weisen Fehler auf und standardmässig ist "allObjectsAccessible=true" gesetzt.
    @Test
    public void validation_Fail_Interlis2Files() throws Exception {
        String serverUrl = "http://localhost:"+port+REST_ENDPOINT;

        MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<String, Object>();
        parameters.add("files", new FileSystemResource("src/test/data/OeREBKRM_V2_0_Gesetze.xml"));
        parameters.add("files", new FileSystemResource("src/test/data/OeREBKRM_V2_0_Themen_dangling_office.xml"));

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
        
        assertEquals("SUCCEEDED", jobResponse.getBody().status());
        assertEquals("FAILED", jobResponse.getBody().validationStatus());

        URL logFileUrl = new URL(jobResponse.getBody().logFileLocation());

        String logFileContents = null;
        try (InputStream in = logFileUrl.openStream()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            logFileContents = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
        assertFalse(logFileContents.contains("Info: assume unknown external objects"));
        assertTrue(logFileContents.contains("Info: ...validation failed"));
        assertTrue(logFileContents.contains("OeREBKRMkvs_V2_0.Thema.ThemaGesetz: No object found with OID ERROR_ch.admin.bk.sr.700"));
    } 
    
    // Dateien mit externer Rolle. 
    // Dateien weisen Fehler auf und mit einem Config-File wird die allObjectsAccessible überschrieben und nicht geprüft.
    @Test
    public void validation_Ok_Interlis2Files_ConfigFile() throws Exception {
        String serverUrl = "http://localhost:"+port+REST_ENDPOINT;

        MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<String, Object>();
        parameters.add("files", new FileSystemResource("src/test/data/OeREBKRM_V2_0_Gesetze.xml"));
        parameters.add("files", new FileSystemResource("src/test/data/OeREBKRM_V2_0_Themen_dangling_office.xml"));
        parameters.add("files", new FileSystemResource("src/test/data/allObjectsAccessible_false.ini"));

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
        
        assertEquals("SUCCEEDED", jobResponse.getBody().status());
        assertEquals("SUCCEEDED", jobResponse.getBody().validationStatus());

        URL logFileUrl = new URL(jobResponse.getBody().logFileLocation());

        String logFileContents = null;
        try (InputStream in = logFileUrl.openStream()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            logFileContents = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
        assertTrue(logFileContents.contains("Info: assume unknown external objects"));
        assertTrue(logFileContents.contains("Info: ...validation done"));
    } 

    // meta-ini aus Repo
    // War früher Ok. Anscheinend hat man an der Konfig / am Modell was
    // geändert.
    @Test
    public void validation_Fail_Naturgefahren() throws Exception {
        String serverUrl = "http://localhost:"+port+REST_ENDPOINT;

        MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<String, Object>();
        parameters.add("files", new FileSystemResource("src/test/data/NGK_SO_Testbeddata.xtf"));
        parameters.add("theme", "naturgefahren");
        
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
        
        assertEquals("SUCCEEDED", jobResponse.getBody().status());
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
    
    // needs external ini file
    @Test
    public void validation_Fail_CustomFunctions() throws Exception {
        String serverUrl = "http://localhost:"+port+REST_ENDPOINT;

        MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<String, Object>();
        parameters.add("files", new FileSystemResource("src/test/data/2457_Messen_vorher.xtf"));
        parameters.add("theme", "nutzungsplanung");
        
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
        
        assertEquals("SUCCEEDED", jobResponse.getBody().status());
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
    
    // Eine fehlerfreie CSV-Datei wird geprüft. Es wird eine Config-Datei mit hochgeladen.
    @Disabled("Es gibt ein neues Modell.")
    @Test
    public void validation_Ok_CsvFile_ConfigFile() throws Exception {
        String serverUrl = "http://localhost:"+port+REST_ENDPOINT;

        MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<String, Object>();
        parameters.add("files", new FileSystemResource("src/test/data/ch.so.hba.gebaeude_sap_20230124.csv"));
        parameters.add("files", new FileSystemResource("src/test/data/ch.so.hba.gebaeude_sap.ini"));

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
        
        assertEquals("SUCCEEDED", jobResponse.getBody().status());
        assertEquals("SUCCEEDED", jobResponse.getBody().validationStatus());
        
        URL logFileUrl = new URL(jobResponse.getBody().logFileLocation());

        String logFileContents = null;
        try (InputStream in = logFileUrl.openStream()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            logFileContents = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
        assertTrue(logFileContents.contains("Info: assume unknown external objects"));
        assertTrue(logFileContents.contains("Info: ...validation done"));
    } 

    // Eine CSV-Datei mit Fehlern wird geprüft. Es wird eine Config-Datei mit hochgeladen. 
    @Disabled("Es gibt ein neues Modell.")
    @Test
    public void validation_Fail_CsvFile_ConfigFile() throws Exception {
        String serverUrl = "http://localhost:"+port+REST_ENDPOINT;

        MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<String, Object>();
        parameters.add("files", new FileSystemResource("src/test/data/ch.so.hba.gebaeude_sap_20230124_errors.csv"));
        parameters.add("files", new FileSystemResource("src/test/data/ch.so.hba.gebaeude_sap.ini"));

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
        
        assertEquals("SUCCEEDED", jobResponse.getBody().status());
        assertEquals("FAILED", jobResponse.getBody().validationStatus());
        
        URL logFileUrl = new URL(jobResponse.getBody().logFileLocation());

        String logFileContents = null;
        try (InputStream in = logFileUrl.openStream()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            logFileContents = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
        assertTrue(logFileContents.contains("Info: assume unknown external objects"));
        assertTrue(logFileContents.contains("Info: ...validation failed"));
        assertTrue(logFileContents.contains("Error: line 3: SO_HBA_Gebaeude_20230111.Gebaeude.Gebaeude: tid o2: value 9605951.200 is out of range in attribute xkoordinaten"));
        assertTrue(logFileContents.contains("Error: line 4: SO_HBA_Gebaeude_20230111.Gebaeude.Gebaeude: tid o3: Attribute EGID is length restricted to 20"));
    } 

    // Eine fehlerfreie CSV-Datei wird geprüft. Es wird eine Config-Datei und Modell mit hochgeladen. 
    @Disabled("Es gibt ein neues Modell.")
    @Test
    public void validation_Ok_CsvFile_ConfigFile_ModelFile() throws Exception {
        String serverUrl = "http://localhost:"+port+REST_ENDPOINT;

        MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<String, Object>();
        parameters.add("files", new FileSystemResource("src/test/data/ch.so.hba.gebaeude_sap_20230124.csv"));
        parameters.add("files", new FileSystemResource("src/test/data/ch.so.hba.gebaeude_sap_upload_model.ini"));
        parameters.add("files", new FileSystemResource("src/test/data/CSV_Model_A.ili"));

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
        
        assertEquals("SUCCEEDED", jobResponse.getBody().status());
        assertEquals("SUCCEEDED", jobResponse.getBody().validationStatus());
        
        URL logFileUrl = new URL(jobResponse.getBody().logFileLocation());

        String logFileContents = null;
        try (InputStream in = logFileUrl.openStream()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            logFileContents = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
        assertTrue(logFileContents.contains("Info: assume unknown external objects"));        
        assertTrue(logFileContents.contains("modelNames <CSV_Model_A>"));
        assertTrue(logFileContents.contains("Info: ...validation done"));
    } 

    // Eine fehlerfreie CSV-Datei wird geprüft. Weil keine Config-Datei hochgeladen wird
    // kann die CSV-Datei nicht geprüft werden und es wird ein Fehler zurückgeliefert.
    @Test
    public void validation_Fail_CsvFile() throws Exception {
        String serverUrl = "http://localhost:"+port+REST_ENDPOINT;

        MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<String, Object>();
        parameters.add("files", new FileSystemResource("src/test/data/ch.so.hba.gebaeude_sap_20230124.csv"));

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
            .until(new MyCallable(operationLocation, restTemplate, "FAILED"));

        // Logfile herunterladen und auswerten
        ResponseEntity<JobResponse> jobResponse = restTemplate.getForEntity(operationLocation, JobResponse.class);
        
        assertEquals("FAILED", jobResponse.getBody().status());
        assertNull(jobResponse.getBody().validationStatus());
        assertNull(jobResponse.getBody().validationStatus());
        assertNull(jobResponse.getBody().validationStatus());
        assertNull(jobResponse.getBody().validationStatus());        
    } 
    
    public class MyCallable implements Callable<Boolean> {  
        private final String operationLocation;
        private final TestRestTemplate restTemplate;
        private String status;
           
        public MyCallable(String operationLocation, TestRestTemplate restTemplate, String status) {
            this.operationLocation = operationLocation;
            this.restTemplate = restTemplate;
            this.status = status;
        }

        public MyCallable(String operationLocation, TestRestTemplate restTemplate) {
            this.operationLocation = operationLocation;
            this.restTemplate = restTemplate;
            this.status = "SUCCEEDED";
        }
        
        @Override
        public Boolean call() throws Exception {
            logger.info("*******************************************************");
            logger.info("polling: {}", operationLocation);
            logger.info("*******************************************************");
            ResponseEntity<JobResponse> jobResponse = restTemplate.getForEntity(operationLocation, JobResponse.class);
            return jobResponse.getBody().status().equalsIgnoreCase(this.status) ? true : false;            
        }
    }
}
