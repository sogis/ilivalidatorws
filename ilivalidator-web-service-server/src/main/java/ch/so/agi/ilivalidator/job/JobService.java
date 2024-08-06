package ch.so.agi.ilivalidator.job;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.interlis2.validator.Validator;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.context.JobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ch.ehi.basics.settings.Settings;

@Service
public class JobService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    private String ilidirs;    
        
    public JobService(@Value("${app.ilidirs}") String ilidirs) {
        this.ilidirs = ilidirs;
    }
    
    @Job(name = "Ilivalidator", retries=0)
    public synchronized boolean validate(JobContext jobContext, Path[] transferFiles, String profile) {
        String jobId = jobContext.getJobId().toString();
        
        List<String> transferFileNames = new ArrayList<>();
        for (Path transferFile : transferFiles) {
            transferFileNames.add(transferFile.toAbsolutePath().toString());
        }

        Path logFilePath = Paths.get(transferFiles[0].getParent().toString(), jobId + ".log");
        String logFileName = logFilePath.toFile().getAbsolutePath();                
        log.debug("<{}> Log file name: {}", jobId, logFileName);

        Settings settings = new Settings();
        settings.setValue(Validator.SETTING_LOGFILE, logFileName);
        settings.setValue(Validator.SETTING_XTFLOG, logFileName + ".xtf");
        settings.setValue(Validator.SETTING_CSVLOG, logFileName + ".csv");
        
        if (ilidirs != null) {
            settings.setValue(Validator.SETTING_ILIDIRS, ilidirs);
            log.debug("<{}> Setting ilidirs: {}", jobId, ilidirs);
        }        

        if (!profile.isEmpty()) {
            settings.setValue(Validator.SETTING_META_CONFIGFILE, profile);
        }
        
        log.info("Validation start");
        boolean valid = Validator.runValidation(transferFileNames.toArray(new String[0]), settings);
        log.info("Validation end");
        
        return valid;
    }

}
