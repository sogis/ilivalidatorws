package ch.so.agi.ilivalidator.profile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@ConditionalOnProperty(
        value="app.restApiEnabled", 
        havingValue = "true", 
        matchIfMissing = false)
@RestController
public class ProfileController {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    private ProfileProperties profileProperties;
    
    public ProfileController(ProfileProperties profileProperties) {
        this.profileProperties = profileProperties;
    }
    
    @GetMapping("/api/profiles")
    public ResponseEntity<?> getProfiles() throws IOException {
        // TreeMap: Sortiert die Map by key
        TreeMap<String, String> treeMap = new TreeMap<>(profileProperties.getProfiles());

        HashMap<String, Map<String,String>> profiles = new HashMap<>();
        profiles.put("profiles", treeMap);
        
        return ResponseEntity.ok().body(profiles);
    }
}
