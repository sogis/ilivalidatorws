package ch.so.agi.ilivalidator.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ch.ehi.basics.settings.Settings;
import ch.interlis.iom_j.itf.ItfReader;
import ch.interlis.iom_j.xtf.XtfReader;
import ch.interlis.iox.IoxEvent;
import ch.interlis.iox.IoxException;
import ch.interlis.iox.IoxReader;
import ch.interlis.iox_j.EndTransferEvent;
import ch.interlis.iox_j.StartBasketEvent;
import ch.so.agi.ilivalidator.Utils;

import org.apache.commons.io.FilenameUtils;
import org.interlis2.validator.Validator;
import org.jobrunr.jobs.annotations.Job;

@Service
public class IlivalidatorService {
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
    
    @Value("${app.preferredIliRepo}")
    private String preferredIliRepo;    
    
    public IlivalidatorService(StorageService storageService) {
        this.storageService = storageService;
    }
    
    /**
     * This method validates an INTERLIS transfer file with
     * <a href="https://github.com/claeis/ilivalidator">ilivalidator library</a>.
     * 
     * @param transferFiles
     * @param modelFiles
     * @param configFiles
     * @throws IoxException   If an error occurred when trying to figure out model
     *                        name.
     * @throws IOException    If config file cannot be read or copied to file system.
     * @return boolean        True, if transfer file is valid. False, if errors were found.
     */
    @Job(name = "Ilivalidator", retries=0)
    public synchronized boolean validate(Path[] transferFiles, Path[] modelFiles,
            Path[] configFiles, String theme) throws IoxException, IOException {
        // Wenn wir nicht das "local"-Filesystem verwenden, müssen die Daten zuerst lokal
        // verfügbar gemacht werden, weil ilivalidator nur mit "File" und nicht mit "Path" umgehen kann.
        Path tmpDirectory = Files.createTempDirectory(Paths.get(System.getProperty("java.io.tmpdir")), folderPrefix);            
        List<String> transferFileNames = new ArrayList<>();
        List<String> modelFileNames = new ArrayList<>();
        List<String> configFileNames = new ArrayList<>();
        Path logFile;
        String logFileName;
        Path jobDirectoryPath = null; // Original-Jobdirectory-Pfad. Entspricht der Job-Id. Wird beim "Hochladen"/Speichern des lokalen Logfiles gebraucht.
        if (storageService instanceof LocalStorageService) {
            for (Path transferFile : transferFiles) {
                if (jobDirectoryPath == null) {
                    jobDirectoryPath = transferFile.getParent().getFileName();
                }
                transferFileNames.add(transferFile.toFile().getAbsolutePath());
            }
            for (Path modelFile : modelFiles) {
                modelFileNames.add(modelFile.toFile().getAbsolutePath());
            }
            for (Path configFile : configFiles) {
                configFileNames.add(configFile.toFile().getAbsolutePath());
            }

        } else {
            for (Path transferFile : transferFiles) {
                if (jobDirectoryPath == null) {
                    jobDirectoryPath = transferFile.getParent().getFileName();
                }
                Path localCopy = storageService.load(transferFile, tmpDirectory);
                transferFileNames.add(localCopy.toFile().getAbsolutePath());
            }
            for (Path modelFile : modelFiles) {
                Path localCopy = storageService.load(modelFile, tmpDirectory);
                modelFileNames.add(localCopy.toFile().getAbsolutePath());
            }
            for (Path configFile : configFiles) {
                Path localCopy = storageService.load(configFile, tmpDirectory);
                configFileNames.add(localCopy.toFile().getAbsolutePath());
            }
        }
        
        // Weil das Verzeichnis nicht nur aus der JobId besteht, sondern ein Prefix besitzt,
        // muss man dieses wieder entfernen, um den Logfile-Namen zu erhalten (der nur aus JobId)
        // bestehen soll.
        logFile = Paths.get(new File(transferFileNames.get(0)).getParent(), jobDirectoryPath.toString().substring(folderPrefix.length()) + ".log");
        logFileName = logFile.toFile().getAbsolutePath();                

        Settings settings = new Settings();
        settings.setValue(Validator.SETTING_LOGFILE, logFileName);
        settings.setValue(Validator.SETTING_XTFLOG, logFileName + ".xtf");
        settings.setValue(Validator.SETTING_CSVLOG, logFileName + ".csv");

        String settingIlidirs = Validator.SETTING_DEFAULT_ILIDIRS;

        // Es wird immer der config/ili-Ordner (d.h. dort wo die mit der Anwendung
        // mitgelieferten Modelle gespeichert sind) als
        // zusätzliches Repo verwendet.
        // Weil allenfalls mit der Transferdatei hochgeladene Modelle im gleichen
        // Verzeichnis wie die Transferdateien liegen,
        // muss dieses Verzeichnis nicht zusätzlich behandelt werden. Es wird in
        // Validator.SETTING_DEFAULT_ILIDIRS
        // bereits berücksichtigt.
        File builtinIliFiles = Paths.get(docBase, configDirectoryName, ILI_SUBDIRECTORY).toFile();
        settingIlidirs = builtinIliFiles.getAbsolutePath() + ";" + preferredIliRepo + ";" + settingIlidirs;
        settings.setValue(Validator.SETTING_ILIDIRS, settingIlidirs);

        log.debug("Setting ilidirs: {}", settingIlidirs);
        
        // Falls man SETTING_ALL_OBJECTS_ACCESSIBLE ausschalten will, muss dies mit einem Config-File
        // gemacht werden. 
        settings.setValue(Validator.SETTING_ALL_OBJECTS_ACCESSIBLE, Validator.TRUE);

        // Falls eine ini-Datei mitgeliefert wird, wird diese verwendet. Sonst wird im
        // config/ini-Ordner (bei den mit der
        // Anwendung mitgelieferten) Config-Dateien geschaut, ob eine passende vorhanden
        // ist.
        // "Passend" heisst: Weil nun mehrere XTF-Dateien mitgeliefert werden können,
        // ist es nicht mehr ganz eindeutig.
        // Es werden alle Modellnamen aus den XTF-Dateien eruiert und dann wird der
        // erste Config-Datei-Match verwendet.
        if (configFileNames.size() > 0) {
            
            // Dateien ohne "-meta" sind normale ini-Dateien.
            for (String fileName : configFileNames) {
                if (!fileName.contains("-meta")) {
                    settings.setValue(Validator.SETTING_CONFIGFILE, fileName);
                    break;
                }
            }
            
            // Dateien mit "-meta" sind config ini-Dateien.
            for (String fileName : configFileNames) {
                if (fileName.contains("-meta")) {
                    settings.setValue(Validator.SETTING_META_CONFIGFILE, fileName);
                    break;
                }
            }

            // Option muss explizit auf NULL gesetzt werden, dann macht ilivalidator nichts
            // resp. es wird der Wert aus der ini-Datei verwendet.
            // Siehe Quellcode ilivalidator.
            // Ggf. noch heikel, da m.E. das auf der Konsole nicht funktionert. Man
            // kann nicht --allObjectsAccessible verwenden und mit einem config-File
            // überschreiben.
            settings.setValue(Validator.SETTING_ALL_OBJECTS_ACCESSIBLE, null);
            log.debug("Uploaded config file used: {}", configFileNames.get(0));            
        } else if (theme != null && !theme.isBlank()) {            
            Map<String,String> themes = Utils.themes();
            String themeConfig = themes.get(theme);
            if (themeConfig.startsWith("ilidata:")) {
                settings.setValue(Validator.SETTING_META_CONFIGFILE, themeConfig);                
            } else  {
                File metaConfigFile = Paths
                        .get(docBase, configDirectoryName, INI_SUBDIRECTORY, theme.toLowerCase() + "-meta.ini")
                        .toFile();
                if (metaConfigFile.exists()) {
                    settings.setValue(Validator.SETTING_META_CONFIGFILE, metaConfigFile.getAbsolutePath());
                    log.debug("Meta config file by theme found in config directory: {}", metaConfigFile.getAbsolutePath());
                } else {
                    log.warn("Meta config file by theme NOT found in config directory: {}", metaConfigFile.getAbsolutePath());                    
                }               
            }
        } else {
              for (String transferFileName : transferFileNames) {
                  String modelName = getModelNameFromTransferFile(transferFileName);
                  File configFile = Paths.get(docBase, configDirectoryName, INI_SUBDIRECTORY, modelName.toLowerCase() + ".ini").toFile();
                  if (configFile.exists()) {
                      settings.setValue(Validator.SETTING_CONFIGFILE, configFile.getAbsolutePath());
                      log.debug("Config file by model name found in config directory: {}", configFile.getAbsolutePath());
                      break;
                  }
              }            
        }
        
        log.info("Validation start");
        boolean valid = Validator.runValidation(transferFileNames.toArray(new String[0]), settings);
        log.info("Validation end");

        // Die lokalen Dateien löschen und das Logfile hochladen.
        if (!(storageService instanceof LocalStorageService)) {
            for (String transferFileName : transferFileNames) {
                Files.delete(Paths.get(transferFileName));
            }
            {
                // Auch wenn ich ilivalidator ohne Jobrunr aufrufe (und somit der Path ein S3Path ist), 
                // funktioniert das Kopieren nicht:
                // java.lang.NullPointerException: Cannot invoke "org.carlspring.cloud.storage.s3fs.S3FileStore.name()" because the return value of "org.carlspring.cloud.storage.s3fs.S3Path.getFileStore()" is null
                
                storageService.store(logFile, Paths.get(jobDirectoryPath.toString(), logFile.getFileName().toString()));
                Files.delete(logFile);
            }
            for (String modelFileName : modelFileNames) {
                Files.delete(Paths.get(modelFileName));
            }
            for (String configFileName : configFileNames) {
                Files.delete(Paths.get(configFileName));
            }
        }
        // Da das tmp-Verzeichnis immer erstellt wird (auch für lokal),
        // muss es auch immer gelöscht werden. Hier ist es immer 
        // leer und kann gelöscht werden.
        Files.delete(tmpDirectory);
        
        return valid;
    }

    /*
     * Figure out INTERLIS model name from INTERLIS transfer file. Works with ili1
     * and ili2.
     */
    private String getModelNameFromTransferFile(String transferFileName) throws IoxException {
        String model = null;
        String ext = FilenameUtils.getExtension(transferFileName);
        IoxReader ioxReader = null;

        try {
            File transferFile = new File(transferFileName);

            if (ext.equalsIgnoreCase("itf")) {
                ioxReader = new ItfReader(transferFile);
            } else {
                ioxReader = new XtfReader(transferFile);
            }

            IoxEvent event;
            StartBasketEvent be = null;
            do {
                event = ioxReader.read();
                if (event instanceof StartBasketEvent) {
                    be = (StartBasketEvent) event;
                    break;
                }
            } while (!(event instanceof EndTransferEvent));

            ioxReader.close();
            ioxReader = null;

            if (be == null) {
                throw new IllegalArgumentException("no baskets in transfer-file");
            }

            String namev[] = be.getType().split("\\.");
            model = namev[0];

        } catch (IoxException e) {
            log.error(e.getMessage());
            e.printStackTrace();
            throw new IoxException("could not parse file: " + new File(transferFileName).getName());
        } finally {
            if (ioxReader != null) {
                try {
                    ioxReader.close();
                } catch (IoxException e) {
                    log.error(e.getMessage());
                    e.printStackTrace();
                    throw new IoxException(
                            "could not close interlise transfer file: " + new File(transferFileName).getName());
                }
                ioxReader = null;
            }
        }
        return model;
    }
}
