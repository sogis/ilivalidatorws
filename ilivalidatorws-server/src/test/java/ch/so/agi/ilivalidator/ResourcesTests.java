package ch.so.agi.ilivalidator;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
public abstract class ResourcesTests {
    static Logger logger = LoggerFactory.getLogger(ResourcesTests.class);

    @LocalServerPort
    protected String port;

    private String CONFIG_ENDPOINT = "/config/";

    // Achtung: Das prüft nicht die GWT-Anwendung, da Javascript nicht aktiviert wird
    // vom Client.
    @Test
    public void isIndexAvailable() throws Exception {
        String serverUrl = "http://localhost:"+port+"/";
        
        URL indexFileUrl = new URL(serverUrl + "index.html");

        String fileContents = null;
        try (InputStream in = indexFileUrl.openStream()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            fileContents = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
        assertTrue(fileContents.contains("ilivalidator web service • Kanton Solothurn"));
    }

    @Test
    public void isIniAvailable() throws Exception {
        String serverUrl = "http://localhost:"+port+CONFIG_ENDPOINT+"ini/";

        URL logFileUrl = new URL(serverUrl + "gb2av.ini");

        String fileContents = null;
        try (InputStream in = logFileUrl.openStream()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            fileContents = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
        assertTrue(fileContents.contains("multiplicity=\"warning\""));
    }
    
    @Test
    public void isIliAvailable() throws Exception {
        String serverUrl = "http://localhost:"+port+CONFIG_ENDPOINT+"ili/";

        URL logFileUrl = new URL(serverUrl + "SO_AGI_MOpublic_20190424_Validierung_20230825.ili");

        String fileContents = null;
        try (InputStream in = logFileUrl.openStream()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            fileContents = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
        assertTrue(fileContents.contains("CONTRACTED MODEL SO_AGI_MOpublic_20190424_Validierung_20230825 (de)"));
    }
}
