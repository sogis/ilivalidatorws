package ch.so.agi.ilivalidator.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.interlis2.validator.Validator;
import org.jobrunr.jobs.annotations.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ch.ehi.basics.settings.Settings;
import ch.interlis.iom_j.csv.CsvReader;
import ch.interlis.iox.IoxException;
import ch.interlis.iox_j.inifile.IniFileReader;
import ch.interlis.iox_j.validator.ValidationConfig;

@Service
public class CsvValidatorService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final String ILI_SUBDIRECTORY = "ili";
    private static final String INI_SUBDIRECTORY = "ini";
            
    private StorageService storageService;

    @Value("${app.docBase}")
    private String docBase;

    @Value("${app.configDirectoryName}")
    private String configDirectoryName;
    
    @Value("${app.workDirectory}")
    private String workDirectory;

    @Value("${app.folderPrefix}")
    private String folderPrefix;

    public CsvValidatorService(StorageService storageService) {
        this.storageService = storageService;
    }

    @Job(name = "Ilivalidator", retries=0)
    public synchronized boolean validate(Path[] csvFiles, Path[] modelFiles, Path[] configFiles) throws IoxException, IOException {
        Path tmpDirectory = Files.createTempDirectory(Paths.get(System.getProperty("java.io.tmpdir")), folderPrefix);            
        List<String> csvFileNames = new ArrayList<>();
        List<String> modelFileNames = new ArrayList<>();
        List<String> configFileNames = new ArrayList<>();
        Path logFile;
        String logFileName;
        Path parentDirectory = null; // Wird beim "Hochladen"/Speichern des lokalen Logfiles gebraucht.
        if (storageService instanceof ch.so.agi.ilivalidator.service.LocalStorageService) {
            for (Path csvFile : csvFiles) {
                if (parentDirectory == null) {
                    parentDirectory = csvFile.getParent().getFileName();
                }
                csvFileNames.add(csvFile.toFile().getAbsolutePath());
            }
            for (Path modelFile : modelFiles) {
                modelFileNames.add(modelFile.toFile().getAbsolutePath());
            }
            for (Path configFile : configFiles) {
                configFileNames.add(configFile.toFile().getAbsolutePath());
            }

        } else {
            for (Path csvFile : csvFiles) {
                if (parentDirectory == null) {
                    parentDirectory = csvFile.getParent().getFileName();
                }
                Path localCopy = tmpDirectory.resolve(Paths.get(csvFile.getFileName().toString()));
                Files.copy(csvFile, localCopy, StandardCopyOption.REPLACE_EXISTING);
                csvFileNames.add(localCopy.toFile().getAbsolutePath());
            }
            for (Path modelFile : modelFiles) {
                Path localCopy = tmpDirectory.resolve(Paths.get(modelFile.getFileName().toString()));
                Files.copy(modelFile, localCopy, StandardCopyOption.REPLACE_EXISTING);
                modelFileNames.add(localCopy.toFile().getAbsolutePath());
            }
            for (Path configFile : configFiles) {
                Path localCopy = tmpDirectory.resolve(Paths.get(configFile.getFileName().toString()));
                Files.copy(configFile, localCopy, StandardCopyOption.REPLACE_EXISTING);
                configFileNames.add(localCopy.toFile().getAbsolutePath());
            }
        }
        logFile = Paths.get(new File(csvFileNames.get(0)).getParent(), parentDirectory.toString() + ".log");
        logFileName = logFile.toFile().getAbsolutePath();                
    
        Settings settings = new Settings();
        settings.setValue(Validator.SETTING_LOGFILE, logFileName);
        settings.setValue(Validator.SETTING_XTFLOG, logFileName + ".xtf");

        String settingIlidirs = Validator.SETTING_DEFAULT_ILIDIRS;

        File builtinIliFiles = Paths.get(docBase, configDirectoryName, ILI_SUBDIRECTORY).toFile();
        settingIlidirs = builtinIliFiles.getAbsolutePath() + ";" + settingIlidirs;
        settings.setValue(Validator.SETTING_ILIDIRS, settingIlidirs);
        log.debug("Setting ilidirs: {}", settingIlidirs);

        if (configFileNames.size() == 0) {
            throw new IllegalArgumentException("You must provide an config file.");
        }
        
        log.debug("Uploaded config file used: {}", configFileNames.get(0));
        ValidationConfig validationConfig = IniFileReader.readFile(new File(configFileNames.get(0)));
                
        String firstLineIsHeader = validationConfig.getConfigValue(CsvConfig.SETTING_SECTION_PARAMETER, CsvConfig.SETTING_FIRSTLINE_IS_HEADER);
        settings.setValue(CsvConfig.SETTING_FIRSTLINE_IS_HEADER, Boolean.parseBoolean(firstLineIsHeader) ? CsvConfig.SETTING_FIRSTLINE_AS_HEADER : CsvConfig.SETTING_FIRSTLINE_AS_VALUE);
        
        String valueDelimiter = validationConfig.getConfigValue(CsvConfig.SETTING_SECTION_PARAMETER, CsvConfig.SETTING_VALUEDELIMITER);
        settings.setValue(CsvConfig.SETTING_VALUEDELIMITER, valueDelimiter.replace("\\", ""));

        String valueSeparator = validationConfig.getConfigValue(CsvConfig.SETTING_SECTION_PARAMETER, CsvConfig.SETTING_VALUESEPARATOR);
        settings.setValue(CsvConfig.SETTING_VALUESEPARATOR, valueSeparator.replace("\\", ""));

        String encoding = validationConfig.getConfigValue(CsvConfig.SETTING_SECTION_PARAMETER, CsvConfig.SETTING_ENCODING);
        settings.setValue(CsvReader.ENCODING, encoding);

        String models = validationConfig.getConfigValue(CsvConfig.SETTING_SECTION_PARAMETER, CsvConfig.SETTING_MODELS);
        if (models == null) {
            throw new IllegalArgumentException("Option "+CsvConfig.SETTING_MODELS+" cannot be empty");
        }
        settings.setValue(Validator.SETTING_MODELNAMES, models);
        
        boolean validationOk = new CsvValidatorImpl().validate(csvFileNames.toArray(new String[0]), settings);
        
        // Die lokalen Dateien löschen und das Logfile hochladen.
        if (!(storageService instanceof ch.so.agi.ilivalidator.service.LocalStorageService)) {
            for (String csvFileName : csvFileNames) {
                Files.delete(Paths.get(csvFileName));
            }
            {
                storageService.store(logFile, Paths.get(parentDirectory.toString(), logFile.getFileName().toString()));
                Files.delete(logFile);
            }
            for (String modelFileName : modelFileNames) {
                Files.delete(Paths.get(modelFileName));
            }
            for (String configFileName : configFileNames) {
                Files.delete(Paths.get(configFileName));
            }
        }

        return validationOk;        
    }
}
