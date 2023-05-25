package ch.so.agi.ilivalidator.controller;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.so.agi.ilivalidator.Settings;

@RestController
public class MainController {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

//    @Value("${lucene.queryDefaultRecords}")
//    private Integer QUERY_DEFAULT_RECORDS;
//
//    @Value("${lucene.queryMaxRecords}")
//    private Integer QUERY_MAX_RECORDS;   

    @Autowired
    Settings settings;

    @PostConstruct
    public void init() throws Exception {
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return new ResponseEntity<String>("ilivalidatorws", HttpStatus.OK);
    }
}
