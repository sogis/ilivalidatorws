package ch.so.agi.ilivalidator.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.interlis2.validator.Validator;
import org.jobrunr.jobs.annotations.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ch.ehi.basics.settings.Settings;
import ch.interlis.iox.IoxException;
import ch.interlis.iox_j.inifile.IniFileReader;
import ch.interlis.iox_j.validator.ValidationConfig;

@Service
public class CsvValidatorService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final String ILI_SUBDIRECTORY = "ili";
    private static final String INI_SUBDIRECTORY = "ini";
        
    @Value("${app.docBase}")
    private String docBase;

    @Value("${app.configDirectoryName}")
    private String configDirectoryName;

    @Job(name = "Ilivalidator", retries=0)
    public synchronized boolean validate(String[] transferFileNames, String logFileName, String[] modelFileNames,
            String[] configFileNames) throws IoxException, IOException {
        Settings settings = new Settings();
        settings.setValue(Validator.SETTING_LOGFILE, logFileName);
        settings.setValue(Validator.SETTING_XTFLOG, logFileName + ".xtf");

        String settingIlidirs = Validator.SETTING_DEFAULT_ILIDIRS;

        File builtinIliFiles = Paths.get(docBase, configDirectoryName, ILI_SUBDIRECTORY).toFile();
        settingIlidirs = builtinIliFiles.getAbsolutePath() + ";" + settingIlidirs;
        settings.setValue(Validator.SETTING_ILIDIRS, Validator.SETTING_DEFAULT_ILIDIRS);
        log.debug("Setting ilidirs: {}", settingIlidirs);

        log.debug("Uploaded config file used: {}", configFileNames[0]);
        ValidationConfig validationConfig = IniFileReader.readFile(new File(configFileNames[0]));
                
        String firstLineIsHeader = validationConfig.getConfigValue(CsvConfig.SETTING_SECTION_PARAMETER, CsvConfig.SETTING_FIRSTLINE_IS_HEADER);
        settings.setValue(CsvConfig.SETTING_FIRSTLINE_IS_HEADER, firstLineIsHeader);
        
        String valueDelimiter = validationConfig.getConfigValue(CsvConfig.SETTING_SECTION_PARAMETER, CsvConfig.SETTING_VALUEDELIMITER);
        settings.setValue(CsvConfig.SETTING_VALUEDELIMITER, valueDelimiter);

        String valueSeparator = validationConfig.getConfigValue(CsvConfig.SETTING_SECTION_PARAMETER, CsvConfig.SETTING_VALUESEPARATOR);
        settings.setValue(CsvConfig.SETTING_VALUESEPARATOR, valueSeparator);

        String encoding = validationConfig.getConfigValue(CsvConfig.SETTING_SECTION_PARAMETER, CsvConfig.SETTING_ENCODING);
        settings.setValue(CsvConfig.SETTING_ENCODING, encoding);

        String models = validationConfig.getConfigValue(CsvConfig.SETTING_SECTION_PARAMETER, CsvConfig.SETTING_MODELS);
        if (models == null) {
            throw new IllegalArgumentException("Option "+CsvConfig.SETTING_MODELS+" cannot be empty");
        }
        settings.setValue(CsvConfig.SETTING_MODELS, models);

        
        
        
//      settings.setValue(Validator.SETTING_ALL_OBJECTS_ACCESSIBLE, null); // siehe ilivalidator service

        
   
        
        
        return true;
    }
}
