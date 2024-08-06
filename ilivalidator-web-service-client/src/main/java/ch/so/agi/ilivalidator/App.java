package ch.so.agi.ilivalidator;

import static elemental2.dom.DomGlobal.console;

import static org.jboss.elemento.Elements.body;
import static org.jboss.elemento.Elements.div;
import static org.jboss.elemento.Elements.p;
import static org.jboss.elemento.Elements.span;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gwtproject.safehtml.shared.SafeHtmlUtils;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;

import elemental2.core.Global;
import elemental2.core.JsString;
import elemental2.dom.DomGlobal;
import elemental2.dom.Event;
import elemental2.dom.EventListener;
import elemental2.dom.File;
import elemental2.dom.FormData;
import elemental2.dom.FormData.AppendValueUnionType;
import elemental2.dom.HTMLButtonElement;
import elemental2.dom.HTMLDivElement;
import elemental2.dom.HTMLDocument;
import elemental2.dom.HTMLElement;
import elemental2.dom.HTMLFormElement;
import elemental2.dom.HTMLInputElement;
import elemental2.dom.HTMLOptionElement;
import elemental2.dom.HTMLOptionsCollection;
import elemental2.dom.HTMLSelectElement;
import elemental2.dom.RequestInit;
import elemental2.dom.URL;
import elemental2.dom.URLSearchParams;
import elemental2.dom.XMLHttpRequest;
import elemental2.promise.Promise;
import jsinterop.base.Js;
import jsinterop.base.JsForEachCallbackFn;
import jsinterop.base.JsPropertyMap;

public class App implements EntryPoint {

    // Kann auch via Settings vom Server kommen.
    // Wäre momentan aber einziges Setting vom Server. Momentan sein lassen.
    private static int MAX_FILES_SIZE_MB = 200; 

    private static final String API_ENDPOINT_JOBS = "/api/jobs";
    private static final String API_ENDPOINT_PROFILES = "/api/profiles";
    private static final String HEADER_OPERATION_LOCATION = "Operation-Location";

    private Timer apiTimer;
    private static final int API_REQUEST_PERIOD_MILLIS = 5000;

    private HTMLFormElement form;
    private HTMLSelectElement select;
    private HTMLInputElement input;
    private HTMLButtonElement button;
    private HTMLDivElement protocolContainer;
    
    private String host; 
    private String protocol;
    private String pathname;
    
    private Map<String,String> profiles;

	public void onModuleLoad() {
        URL url = new URL(DomGlobal.location.href);
        host = url.host;
        protocol = url.protocol;
        pathname = url.pathname.length()==1?"":url.pathname;
        
        String requestUrl = protocol + "//" + host + pathname + API_ENDPOINT_PROFILES;
        
        DomGlobal.fetch(requestUrl).then(response -> {
            if (!response.ok) {
                DomGlobal.window.alert(response.statusText + ": " + response.body);
                return null;
            }
            return response.text();
        }).then(json -> {            
            JsPropertyMap<Object> responseMap = Js.asPropertyMap(Global.JSON.parse(json));
            JsPropertyMap<Object> profilesMap = Js.asPropertyMap(responseMap.get("profiles"));
            profiles = new HashMap<>();
            profilesMap.forEach(new JsForEachCallbackFn() {
                @Override
                public void onKey(String key) {
                    String value = String.valueOf(profilesMap.get(key));
                    profiles.put(key, value);
                }                
            });
            init(); 
            return null;
        }).catch_(error -> {
            console.log(error);
            DomGlobal.window.alert(error.toString());
            return null;
        });
	}
	
	private void init() {
	    select = (HTMLSelectElement) getDocument().getElementById("profileSelect");
        HTMLOptionsCollection options = select.options;

        for (Map.Entry<String, String> entry : profiles.entrySet()) {
            HTMLOptionElement option = (HTMLOptionElement) getDocument().createElement("option");        
            option.text = entry.getKey();
            option.value = entry.getValue();
            options.add(option);            
        }        

        select.options = options;
        
        select.addEventListener("change", new EventListener() {
            @Override
            public void handleEvent(Event evt) {
                String theme = select.selectedOptions.getAt(0).text;
                if (select.selectedIndex == 0) {
                    updateUrlLocation(null);
                } else {
                    updateUrlLocation(theme);
                }                
            }
        });

        input = (HTMLInputElement) getDocument().getElementById("fileInput");
                	    
	    button = (HTMLButtonElement) getDocument().getElementById("submitButton");
        
        form = (HTMLFormElement) getDocument().getElementById("uploadForm");
        form.addEventListener("submit", new EventListener() {
            @Override
            public void handleEvent(Event evt) {
                protocolContainer.style.display = "none";
                protocolContainer.innerHTML = "";
                
                evt.preventDefault();

                if (input.files.length < 1) {
                    return;
                }
                                
                input.disabled = true;
                button.disabled = true;

                FormData formData = new FormData();
                                
                if (select.selectedIndex > 0) {
                    formData.append("profile", select.selectedOptions.getAt(0).value);                    
                }
                
                List<String> fileNames = new ArrayList<>();
                int filesSize = 0;
                for (int i=0; i<input.files.length; i++) {
                    File file = input.files.getAt(i);
                    fileNames.add(file.name);
                    // Falls Safari Probleme macht:
                    // https://github.com/hal/console/blob/d435e24a837adedbd6aefe06495eafaa97c08a65/dmr/src/main/java/org/jboss/hal/dmr/dispatch/Dispatcher.java#L263
                    formData.append("files", AppendValueUnionType.of(file), file.name);
                    filesSize += file.size;
                    int filesSizeMb = filesSize / 1024 / 1024;
                    if (filesSizeMb > MAX_FILES_SIZE_MB) {
                        Window.alert("Datei(en) zu gross. Maximum: " + String.valueOf(MAX_FILES_SIZE_MB) + "MB");
                        resetInputElements();
                        return;
                    }
                }
                
                protocolContainer.style.display = "block";
                
                for (String fileName : fileNames) {
                    logMessage(fileName + " hochladen ...");
                }
                
                RequestInit init = RequestInit.create();
                init.setMethod("POST");
                init.setBody(formData);

                String requestUrl = protocol + "//" + host + pathname + API_ENDPOINT_JOBS;

                DomGlobal.fetch(requestUrl, init)
                .then(response -> {
                    if (!response.ok) {
                        resetInputElements();
                        
                        Promise<String> promise  = response.text();
                        promise.then(r -> { 
                            logMessage(r);
                            return null;
                        });
                        
                        return null;
                    }
                    String jobUrl = response.headers.get(HEADER_OPERATION_LOCATION);
                    console.log(jobUrl);
                    
                    if (apiTimer != null) {
                        apiTimer.cancel();
                    }

                    logMessage("Datei wird validiert ...");
                    
                    apiTimer = new Timer() {
                        // Statt "fetch" wird XMLHttpRequest verwendet. 
                        // Diese Klasse erlaubt den Request NICHT asyncron
                        // zu machen.
                        public void run() {                          
                            XMLHttpRequest httpRequest = new XMLHttpRequest();
                            httpRequest.open("GET", jobUrl, false);
                            httpRequest.onload = event -> {
                                if (httpRequest.status == 200) {
                                    JsPropertyMap<?> resultJsonObj = (JsPropertyMap<?>) Global.JSON.parse(httpRequest.responseText);
                                    String jobStatus = ((JsString) resultJsonObj.get("jobStatus")).normalize();
                                    
                                    if (jobStatus.equalsIgnoreCase("SUCCEEDED")) {
                                        apiTimer.cancel();
                                        resetInputElements();

                                        String validationStatus = ((JsString) resultJsonObj.get("validationStatus")).normalize();
                                        
                                        HTMLDivElement validationDiv = div().element();
                                        if (validationStatus.equalsIgnoreCase("FAILED")) {
                                            //validationDiv.style.backgroundColor = "#EC7063";
                                            validationDiv.append(div().style("background-color:#EC7063;").textContent("Es wurden Fehler gefunden.").element());
                                        } else {
                                            validationDiv.append(div().style("background-color:#58D68D;").textContent("Es wurden keine Fehler gefunden.").element());                                            
                                        }
                                        
                                        String logFileHref = ((JsString) resultJsonObj.get("logFileLocation")).normalize();
                                        String xtfLogFileHref = ((JsString) resultJsonObj.get("xtfLogFileLocation")).normalize();
                                        String csvLogFileHref = ((JsString) resultJsonObj.get("csvLogFileLocation")).normalize();

                                        String txtLogIconLink = "<a href=\""+logFileHref+"\" target=\"_blank\" class=\"badge-link\">\n"
                                                + "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"24\" height=\"24\" fill=\"currentColor\" class=\"bi bi-file-earmark-text\" viewBox=\"0 0 16 16\">\n"
                                                + "  <path d=\"M5.5 7a.5.5 0 0 0 0 1h5a.5.5 0 0 0 0-1h-5zM5 9.5a.5.5 0 0 1 .5-.5h5a.5.5 0 0 1 0 1h-5a.5.5 0 0 1-.5-.5zm0 2a.5.5 0 0 1 .5-.5h2a.5.5 0 0 1 0 1h-2a.5.5 0 0 1-.5-.5z\"></path>\n"
                                                + "  <path d=\"M9.5 0H4a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2V4.5L9.5 0zm0 1v2A1.5 1.5 0 0 0 11 4.5h2V14a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1h5.5z\"></path>\n"
                                                + "</svg>\n"
                                                + "</a>";
                                        
                                        String xtfLogIconLink = "<a href=\""+xtfLogFileHref+"\" target=\"_blank\" class=\"badge-link\">\n"
                                                + "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"24\" height=\"24\" fill=\"currentColor\" class=\"bi bi-file-earmark-code\" viewBox=\"0 0 16 16\">\n"
                                                + "  <path d=\"M14 4.5V14a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V2a2 2 0 0 1 2-2h5.5L14 4.5zm-3 0A1.5 1.5 0 0 1 9.5 3V1H4a1 1 0 0 0-1 1v12a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1V4.5h-2z\"/>\n"
                                                + "  <path d=\"M8.646 6.646a.5.5 0 0 1 .708 0l2 2a.5.5 0 0 1 0 .708l-2 2a.5.5 0 0 1-.708-.708L10.293 9 8.646 7.354a.5.5 0 0 1 0-.708zm-1.292 0a.5.5 0 0 0-.708 0l-2 2a.5.5 0 0 0 0 .708l2 2a.5.5 0 0 0 .708-.708L5.707 9l1.647-1.646a.5.5 0 0 0 0-.708z\"/>\n"
                                                + "</svg>\n"
                                                + "</a>";
                                        
                                        String csvLogFileName = csvLogFileHref.substring(csvLogFileHref.lastIndexOf("/")+1);
                                        String csvLogIconLink = "<a href=\""+csvLogFileHref+"\" target=\"_blank\" class=\"badge-link\" download=\""+csvLogFileName+"\">\n"
                                                + "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"24\" height=\"24\" fill=\"currentColor\" class=\"bi bi-filetype-csv\" viewBox=\"0 0 16 16\">\n"
                                                + "  <path fill-rule=\"evenodd\" d=\"M14 4.5V14a2 2 0 0 1-2 2h-1v-1h1a1 1 0 0 0 1-1V4.5h-2A1.5 1.5 0 0 1 9.5 3V1H4a1 1 0 0 0-1 1v9H2V2a2 2 0 0 1 2-2h5.5zM3.517 14.841a1.13 1.13 0 0 0 .401.823q.195.162.478.252.284.091.665.091.507 0 .859-.158.354-.158.539-.44.187-.284.187-.656 0-.336-.134-.56a1 1 0 0 0-.375-.357 2 2 0 0 0-.566-.21l-.621-.144a1 1 0 0 1-.404-.176.37.37 0 0 1-.144-.299q0-.234.185-.384.188-.152.512-.152.214 0 .37.068a.6.6 0 0 1 .246.181.56.56 0 0 1 .12.258h.75a1.1 1.1 0 0 0-.2-.566 1.2 1.2 0 0 0-.5-.41 1.8 1.8 0 0 0-.78-.152q-.439 0-.776.15-.337.149-.527.421-.19.273-.19.639 0 .302.122.524.124.223.352.367.228.143.539.213l.618.144q.31.073.463.193a.39.39 0 0 1 .152.326.5.5 0 0 1-.085.29.56.56 0 0 1-.255.193q-.167.07-.413.07-.175 0-.32-.04a.8.8 0 0 1-.248-.115.58.58 0 0 1-.255-.384zM.806 13.693q0-.373.102-.633a.87.87 0 0 1 .302-.399.8.8 0 0 1 .475-.137q.225 0 .398.097a.7.7 0 0 1 .272.26.85.85 0 0 1 .12.381h.765v-.072a1.33 1.33 0 0 0-.466-.964 1.4 1.4 0 0 0-.489-.272 1.8 1.8 0 0 0-.606-.097q-.534 0-.911.223-.375.222-.572.632-.195.41-.196.979v.498q0 .568.193.976.197.407.572.626.375.217.914.217.439 0 .785-.164t.55-.454a1.27 1.27 0 0 0 .226-.674v-.076h-.764a.8.8 0 0 1-.118.363.7.7 0 0 1-.272.25.9.9 0 0 1-.401.087.85.85 0 0 1-.478-.132.83.83 0 0 1-.299-.392 1.7 1.7 0 0 1-.102-.627zm8.239 2.238h-.953l-1.338-3.999h.917l.896 3.138h.038l.888-3.138h.879z\"/>\n"
                                                + "</svg>";

                                        validationDiv.append(div().innerHtml(
                                                SafeHtmlUtils.fromTrustedString("Logdateien: " + txtLogIconLink + " &nbsp;" + xtfLogIconLink + " &nbsp;" + csvLogIconLink))
                                                .element());
                                        
                                        logMessage(validationDiv);
                                    }
                                } else if (httpRequest.status == 400) { // Bad Request
                                    // Dieser Statuscode wird vom Server zurückgeliefert,
                                    // falls Job-Status==FAILED ist.
                                    apiTimer.cancel();
                                    resetInputElements();

                                    JsPropertyMap<?> resultJsonObj = (JsPropertyMap<?>) Global.JSON.parse(httpRequest.responseText);
                                     if (resultJsonObj.get("message") != null) {
                                         String message = ((JsString) resultJsonObj.get("message")).normalize();
                                         DomGlobal.window.alert("Error running Job: " + jobUrl + "\n" + message);
                                     } else {
                                         DomGlobal.window.alert("Error running Job: " + jobUrl);                                    
                                     }                                    
                                } else {
                                    console.log("status code: " + httpRequest.status);
                                    apiTimer.cancel();
                                    resetInputElements();
                                    DomGlobal.window.alert("Error fetching: " + jobUrl);
                                }
                            };
                            
                            httpRequest.addEventListener("error", event -> {
                                console.log("status text: " + httpRequest.statusText);
                                apiTimer.cancel();
                                resetInputElements();
                                DomGlobal.window.alert("Error fetching: " + jobUrl + "\n Error: " + httpRequest.status + " " + httpRequest.statusText);
                            });
                            httpRequest.send();
                        }
                    };
                    apiTimer.scheduleRepeating(API_REQUEST_PERIOD_MILLIS);
                    return null;
                })
                .catch_(error -> {
                    console.log(error);
                    resetInputElements();
                    DomGlobal.window.alert(error);
                    return null;
                });
            } 
        });

        protocolContainer = (HTMLDivElement) getDocument().getElementById("protocol-container");
        protocolContainer.style.display = "none";
        
        // Profile aus der URL lesen und die Comobox entsprechend
        // setzen.
        URL url = new URL(DomGlobal.window.location.href);
        String profile = url.searchParams.get("p");

        if (profile != null) {
            for (int i=0; i<options.length; i++) {
                HTMLOptionElement option = options.getAt(i);
                if (option.text.equalsIgnoreCase(profile)) {
                    select.selectedIndex = i;
                }
            }            
        }
	}
	
	private void logMessage(String message) {
	    protocolContainer.append(div().textContent(message).element());
	}
	
	private void logMessage(HTMLElement element) {
	    protocolContainer.append(div().add(element).element());
	}
	
    private void resetInputElements() {
        input.value = "";
        input.disabled = false;
        button.disabled = false;
    }

    private HTMLDocument getDocument() {
        return DomGlobal.document;
    }
    
    private void updateUrlLocation(String theme) {
        URL url = new URL(DomGlobal.location.href);
        
        String newUrl = protocol + "//" + host + pathname;
        if (theme != null) {
            URLSearchParams params = url.searchParams;
            params.set("p", theme);
            newUrl += "?" + params.toString(); 
        } 
        updateUrlWithoutReloading(newUrl);
    }

    // Update the URL in the browser without reloading the page.
    private static native void updateUrlWithoutReloading(String newUrl) /*-{
        $wnd.history.pushState(newUrl, "", newUrl);
    }-*/;
    
//    public native static String uuid() /*-{
//    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g,
//            function(c) {
//                var r = Math.random() * 16 | 0, v = c == 'x' ? r
//                        : (r & 0x3 | 0x8);
//                return v.toString(16);
//            });
//    }-*/;
}
