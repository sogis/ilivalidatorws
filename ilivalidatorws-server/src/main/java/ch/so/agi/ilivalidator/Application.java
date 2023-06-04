package ch.so.agi.ilivalidator;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.filter.ForwardedHeaderFilter;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
@ServletComponentScan
@Configuration
@EnableScheduling
public class Application extends SpringBootServletInitializer {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Value("${app.connectTimeout}")
    private String connectTimeout;
    
    @Value("${app.readTimeout}")
    private String readTimeout;
    
    @Value("${app.docBase}")
    private String docBase;

    @Value("${app.configDirectoryName}")
    private String configDirectoryName;

    @Value("${app.unpackConfigFiles}")
    private boolean unpackConfigFiles;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
  
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(Application.class);
    }

    @Bean
    public ForwardedHeaderFilter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    }  
  
    // TODO: Konfigurierbar?
    @Bean
    void setPluginClasses() {
        System.setProperty("ch.ehi.iox-ili.pluginClasses",
                "ch.so.agi.ilivalidator.ext.AreaIoxPlugin," 
                        + "ch.so.agi.ilivalidator.ext.LengthIoxPlugin,"
                        + "ch.so.agi.ilivalidator.ext.IsHttpResourceIoxPlugin,"
                        + "ch.so.agi.ilivalidator.ext.IsValidDocumentsCycleIoxPlugin,"
                        + "ch.so.agi.ilivalidator.ext.RingSelfIntersectionIoxPlugin,"
                        + "ch.so.agi.ilivalidator.ext.TooFewPointsPolylineIoxPlugin");
    }
    
    // CommandLineRunner: Anwendung live aber nicht ready.
    @Bean
    CommandLineRunner init() {
        return args -> {
            System.setProperty("sun.net.client.defaultConnectTimeout", connectTimeout);
            System.setProperty("sun.net.client.defaultReadTimeout", readTimeout);

            // Root-Verzeichnis und das sichtbare "config"-Verzeichnis und
            // Unterverzeichnisse für das Directory-Listing erstellen.
            // Die ini- und ili-Dateien werden in die entsprechenden
            // Verzeichnisse kopiert. 
            if (!new File(docBase).exists()) {
                new File(docBase).mkdir();
            }
            
            File configDirectory = Paths.get(docBase, configDirectoryName).toFile();
            if (!configDirectory.exists()) {
                configDirectory.mkdir();
            }

            File iliDirectory = Paths.get(docBase, configDirectoryName, "ili").toFile();
            if (!iliDirectory.exists()) {
                iliDirectory.mkdir();
            }

            if (unpackConfigFiles) {
                ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
                Resource[] resources = resolver.getResources("classpath:ili/*.ili");
                for (Resource resource : resources) {
                    InputStream is = resource.getInputStream();
                    File tomlFile = Paths.get(iliDirectory.getAbsolutePath(), resource.getFilename()).toFile();
                    log.info("Copying {} to {}", resource.getFilename(), iliDirectory.getAbsolutePath());
                    Files.copy(is, tomlFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    IOUtils.closeQuietly(is);
                }
            }

            File iniDirectory = Paths.get(docBase, configDirectoryName, "ini").toFile();
            if (!iniDirectory.exists()) {
                iniDirectory.mkdir();
            }
            
            if (unpackConfigFiles) {
                ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
                Resource[] resources = resolver.getResources("classpath:ini/*.ini");
                for (Resource resource : resources) {
                    InputStream is = resource.getInputStream();
                    File tomlFile = Paths.get(iniDirectory.getAbsolutePath(), resource.getFilename()).toFile();
                    log.info("Copying {} to {}", resource.getFilename(), iniDirectory.getAbsolutePath());
                    Files.copy(is, tomlFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    IOUtils.closeQuietly(is);
                }
            }
            
            // Die XSL-Datei in das Directory-Listing-Root-Verzeichnis
            // kopieren.
            String LISTING_XSL = "listing.xsl";
            File listingXslFile = Paths.get(docBase, LISTING_XSL).toFile();
            InputStream listingXslResource = new ClassPathResource(LISTING_XSL).getInputStream();
            Files.copy(listingXslResource, listingXslFile.toPath(), StandardCopyOption.REPLACE_EXISTING);      
            
        };
    }
}
