package ch.so.agi.ilivalidator.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import org.apache.commons.io.FilenameUtils;
import org.interlis2.validator.Validator;
import org.jobrunr.jobs.annotations.Job;

@Service
public class IlivalidatorService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Value("${app.docBase}")
    private String docBase;

    @Value("${app.configDirectoryName}")
    private String configDirectoryName;

    private static final String ILI_SUBDIRECTORY = "ili";
    private static final String INI_SUBDIRECTORY = "ini";
    
    /**
     * This method validates an INTERLIS transfer file with
     * <a href="https://github.com/claeis/ilivalidator">ilivalidator library</a>.
     * 
     * @param transferFileNames
     * @param logFileName
     * @param modelFileNames
     * @param configFileNames
     * @throws IoxException   If an error occurred when trying to figure out model
     *                        name.
     * @throws IOException    If config file cannot be read or copied to file system.
     * @return boolean        True, if transfer file is valid. False, if errors were found.
     */
    @Job(name = "Ilivalidator")
    public synchronized boolean validate(String[] transferFileNames, String logFileName, String[] modelFileNames,
            String[] configFileNames) throws IoxException, IOException {
        Settings settings = new Settings();
        settings.setValue(Validator.SETTING_LOGFILE, logFileName);
        settings.setValue(Validator.SETTING_XTFLOG, logFileName + ".xtf");

        log.info("1 {}", transferFileNames);
        log.info("2 {}", logFileName);
        log.info("3 {}", modelFileNames);
        log.info("4 {}", configFileNames);

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
        settingIlidirs = builtinIliFiles.getAbsolutePath() + ";" + settingIlidirs;
        settings.setValue(Validator.SETTING_ILIDIRS, Validator.SETTING_DEFAULT_ILIDIRS);

        log.debug("setting ilidirs: {}", settingIlidirs);

        // Falls eine ini-Datei mitgeliefert wird, wird diese verwendet. Sonst wird im
        // config/ini-Ordner (bei den mit der
        // Anwendung mitgelieferten) Config-Dateien geschaut, ob eine passende vorhanden
        // ist.
        // "Passend" heisst: Weil nun mehrere XTF-Dateien mitgeliefert werden können,
        // ist es nicht mehr ganz eindeutig.
        // Es werden alle Modellnamen aus den XTF-Dateien eruiert und dann wird der
        // erste Config-Datei-Match verwendet.
        if (configFileNames.length > 0) {
            settings.setValue(Validator.SETTING_CONFIGFILE, configFileNames[0]);
            log.debug("Uploaded config file used: {}", configFileNames[0]);
        } else {
            for (String transferFileName : transferFileNames) {
                String modelName = getModelNameFromTransferFile(transferFileName);
                File configFile = Paths.get(docBase, configDirectoryName, INI_SUBDIRECTORY, modelName.toLowerCase() + ".ini").toFile();
                if (configFile.exists()) {
                    settings.setValue(Validator.SETTING_CONFIGFILE, configFile.getAbsolutePath());
                    log.debug("Config file found in config directory: {}", configFile.getAbsolutePath());
                    break;
                }
            }
        }
        
        // Falls man SETTING_ALL_OBJECTS_ACCESSIBLE ausschalten will, muss dies mit einem Config-File
        // gemacht werden. 
        // TODO Prüfen, ob das jetzt geht.
        settings.setValue(Validator.SETTING_ALL_OBJECTS_ACCESSIBLE, Validator.TRUE);

        log.info("Validation start.");
        boolean valid = Validator.runValidation(transferFileNames, settings);
        log.info("Validation end.");

        return valid;

//        // TODO
//        // Das sollte nun gehen:
//        // Leider scheint es nicht steuerbar zu sein via toml.
//        // https://github.com/claeis/ilivalidator/issues/350
//        // https://github.com/claeis/ilivalidator/issues/83
    }

    /**
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
