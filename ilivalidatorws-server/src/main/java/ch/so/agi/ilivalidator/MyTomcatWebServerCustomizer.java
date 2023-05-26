package ch.so.agi.ilivalidator;

import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

@Component
public class MyTomcatWebServerCustomizer implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

    @Value("${app.docBase}")
    private String docBase;

    @Value("${app.configDirectoryName}")
    private String configDirectoryName;

    // TODO: Funktioniert das mit reverse proxy?
    
    /*
     * Achtung: Low level Tomcat Konfiguration, damit das Directory-Listing
     * verwendet werden kann. 
     * Andere statische (Spring Boot) Ressourcen funktionieren immer
     * noch (z.B. "src/main/resources/static").
     */
    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        TomcatContextCustomizer tomcatContextCustomizer = new TomcatContextCustomizer() {
            @Override
            public void customize(Context context) {
                String childFolder = configDirectoryName;
                context.setDocBase(docBase);                
                Wrapper defServlet = (Wrapper) context.findChild("default");
                defServlet.addInitParameter("listings", "true");
                defServlet.addInitParameter("sortListings", "true");
                defServlet.addInitParameter("sortDirectoriesFirst", "true");
                defServlet.addInitParameter("readOnly", "true");
                defServlet.addInitParameter("contextXsltFile", "/listing.xsl");
                defServlet.addMapping("/"+childFolder+"/*");                
            }
        };
        factory.addContextCustomizers(tomcatContextCustomizer);        
    }
}
