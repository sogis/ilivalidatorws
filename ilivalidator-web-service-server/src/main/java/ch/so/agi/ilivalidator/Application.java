package ch.so.agi.ilivalidator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
//@ServletComponentScan
@Configuration
@EnableScheduling
public class Application {
  
    @Value("${app.connectTimeout}")
    private String connectTimeout;
    
    @Value("${app.readTimeout}")
    private String readTimeout;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }  
        
    @Bean
    CommandLineRunner init() {
        return args -> {
            System.setProperty("sun.net.client.defaultConnectTimeout", connectTimeout);
            System.setProperty("sun.net.client.defaultReadTimeout", readTimeout);
            
            System.setProperty("ch.ehi.iox-ili.pluginClasses",
                    "ch.so.agi.ilivalidator.ext.AreaIoxPlugin," 
                            + "ch.so.agi.ilivalidator.ext.LengthIoxPlugin,"
                            + "ch.so.agi.ilivalidator.ext.IsHttpResourceIoxPlugin,"
                            + "ch.so.agi.ilivalidator.ext.IsValidDocumentsCycleIoxPlugin,"
                            + "ch.so.agi.ilivalidator.ext.RingSelfIntersectionIoxPlugin,"
                            + "ch.so.agi.ilivalidator.ext.TooFewPointsPolylineIoxPlugin,"
                            + "ch.geowerkstatt.ilivalidator.extensions.functions.GetAreaIoxPlugin,"
                            + "ch.geowerkstatt.ilivalidator.extensions.functions.GetLengthIoxPlugin,"
                            + "ch.geowerkstatt.ilivalidator.extensions.functions.GetInnerRingsCountIoxPlugin,"
                            + "ch.geowerkstatt.ilivalidator.extensions.functions.GetInGroupsIoxPlugin,"
                            + "ch.geowerkstatt.ilivalidator.extensions.functions.IsInsideExternalXtfIoxPlugin,"
                            + "ch.geowerkstatt.ilivalidator.extensions.functions.IsInsideExternalXtfResourceIoxPlugin,"
                            + "ch.geowerkstatt.ilivalidator.extensions.functions.IsInsideIoxPlugin,"
                            + "ch.geowerkstatt.ilivalidator.extensions.functions.UnionIoxPlugin,"
                            + "ch.geowerkstatt.ilivalidator.extensions.functions.PolylinesOverlapIoxPlugin,"
                            + "ch.geowerkstatt.ilivalidator.extensions.functions.FindObjectsIoxPlugin,"
                            + "ch.geowerkstatt.ilivalidator.extensions.functions.FilterIoxPlugin,"
                            + "ch.geowerkstatt.ilivalidator.extensions.functions.ngk.IsInsideAreaByCodeIoxPlugin");
        };
    }
}
