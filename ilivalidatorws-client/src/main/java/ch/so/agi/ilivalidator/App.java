package ch.so.agi.ilivalidator;

import static elemental2.dom.DomGlobal.console;
import static elemental2.dom.DomGlobal.fetch;
import static org.jboss.elemento.Elements.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import com.google.gwt.core.client.GWT;

import org.dominokit.domino.ui.button.Button;
import org.dominokit.domino.ui.grid.Column;
import org.dominokit.domino.ui.grid.Row;
import org.dominokit.domino.ui.modals.ModalDialog;
import org.dominokit.domino.ui.style.Color;
import org.dominokit.domino.ui.utils.TextNode;
import org.gwtproject.safehtml.shared.SafeHtmlUtils;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Timer;

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
import elemental2.dom.Location;
import elemental2.dom.RequestInit;
import elemental2.dom.URL;
import elemental2.dom.URLSearchParams;
import elemental2.dom.XMLHttpRequest;
import elemental2.promise.Promise;
import jsinterop.base.Any;
import jsinterop.base.Js;
import jsinterop.base.JsPropertyMap;

public class App implements EntryPoint {
    // Internationalization
    private MyMessages messages = GWT.create(MyMessages.class);

    // Application settings
    private String myVar;

    // Format settings
    private NumberFormat fmtDefault = NumberFormat.getDecimalFormat();
    private NumberFormat fmtPercent = NumberFormat.getFormat("#0.0");

    // Kann auch via Settings vom Server kommen.
    // Wäre momentan aber einziges Setting vom Server. Momentan sein lassen.
    private static int MAX_FILES_SIZE_MB = 300; 
    
    private static final String API_JOBS_ENDPOINT = "/api/jobs";
    private static final String API_PROFILES_ENDPOINT = "/api/profiles";
    private static final String HEADER_OPERATION_LOCATION = "Operation-Location";
    
    private Timer apiTimer;
    private static final int API_REQUEST_PERIOD_MILLIS = 5000;
    
    private List<String> validationProfiles = new ArrayList<>();
    
    private HTMLFormElement form;
    private HTMLSelectElement select;
    private HTMLInputElement input;
    private HTMLButtonElement button;
    private HTMLDivElement protocolContainer;
    
    private String host; 
    private String protocol;
    private String pathname;
    
    public void onModuleLoad() {
        URL url = new URL(DomGlobal.location.href);
        host = url.host;
        protocol = url.protocol;
        pathname = url.pathname;

        if (pathname.length() == 1) {
            pathname = "";
            console.log("length 1");
        } else {
            pathname = pathname.substring(1);
            console.log("length not 1");
        }

        String requestUrl = protocol + "//" + host + pathname + API_PROFILES_ENDPOINT;
        console.log("requestUrl: " + requestUrl);
        
        DomGlobal.fetch(requestUrl).then(response -> {
            if (!response.ok) {
                DomGlobal.window.alert(response.statusText + ": " + response.body);
                return null;
            }
            return response.text();
        }).then(json -> {            
            JsPropertyMap<Object> profilesMap = Js.asPropertyMap(Global.JSON.parse(json));
            Any[] profilesArray = profilesMap.getAsAny("profiles").asArray();
            for (int i=0; i<profilesArray.length; i++) {
                //console.log(profilesArray[i]);
                validationProfiles.add(profilesArray[i].asString());
            }
            init();
            return null;
        }).catch_(error -> {
            console.log(error);
            DomGlobal.window.alert(error.toString());
            return null;
        });
    }
    
    public void init() {                                
        // Get url from browser (client) to find out the correct location of resources.
        // And read the theme from query params.
        // TODO: Ich benötige sie bereits vorher (onModuleLoad). Duplicate -> Bereinigen.
        Location location = DomGlobal.window.location;

        // Get document element which will be used to create other elements.
        HTMLDocument document = DomGlobal.document;
        
        // Add our "root" container
        HTMLDivElement container = div().id("container").element();
        body().add(container);

        // Add logo
        HTMLElement logoDiv = div().css("logo")
                .add(div().add(
                        img().attr("src", location.protocol + "//" + location.host + location.pathname + "Logo.png")
                                .attr("alt", "Logo Kanton"))
                        .element())
                .element();
        container.appendChild(logoDiv);

        // Title
        container.appendChild(div().css("ilivalidatorws-title").textContent("ilivalidator web service").element());

        // Info
        String infoString = "Der <i>ilivalidator web service</i> prüft, ob deine INTERLIS-Transferdateien (.xtf, .xml, .itf) modellkonform sind. "
        + "Für einige Datenmodelle gibt es ein zusätzliches Prüfprofil, bei welchem die Prüfung erweitert ist.";
        
        container.appendChild(div().css("info").innerHtml(SafeHtmlUtils.fromTrustedString(infoString)).element());
        
        // Protocol
        protocolContainer = div().id("protocol-container").element();
        
        // Form incl business logic
        form = (HTMLFormElement) document.createElement("form");
        form.id = "upload-form";
        form.enctype = "multipart/form-data";
        form.action = "";        
        container.appendChild(form);
        
        select = (HTMLSelectElement) document.createElement("select");        
        HTMLOptionsCollection options = select.options;

        {
            HTMLOptionElement option = (HTMLOptionElement) document.createElement("option");        
            option.text = messages.validationProfileSelectDefault();
            option.value = messages.validationProfileSelectDefault();
            options.add(option);
        }
        
        Collections.sort(validationProfiles);
        for (String validationProfile : validationProfiles) {
            HTMLOptionElement option = (HTMLOptionElement) document.createElement("option");        
            option.text = validationProfile;
            option.value = validationProfile.substring(0, validationProfile.length()-4);
            options.add(option);            
        }
        
        select.options = options;
        form.appendChild(select);
        
        select.addEventListener("change", new EventListener() {
            @Override
            public void handleEvent(Event evt) {
                String theme = select.selectedOptions.getAt(0).value;
                if (select.selectedIndex == 0) {
                    updateUrlLocation(null);
                } else {
                    updateUrlLocation(theme);
                }                
            }
        });
        
        form.appendChild(span().id("profile-label").textContent(messages.validationProfileSelect()).element());
        
        form.appendChild(p().element());
        
        input = (HTMLInputElement) document.createElement("input");
        input.setAttribute("type", "file");
        input.setAttribute("name", "files");
        input.multiple = true;
        input.accept = ".itf,.xtf,.xml,.ili,.ini,.csv";
        form.appendChild(input);

        button = (HTMLButtonElement) document.createElement("button");
        button.className = "submit-button";
        button.textContent = messages.submitButtonDefault();
        form.appendChild(button);
        
        form.addEventListener("submit", new EventListener() {
            @Override
            public void handleEvent(Event evt) {
                evt.preventDefault();
                
                cleanProtocol();
                
                if (input.files.length < 1) {
                    return;
                }
                
                button.textContent = messages.submitButtonValidate();

                input.disabled = true;
                button.disabled = true;
                
                FormData formData = new FormData();
                
                URL url = new URL(DomGlobal.window.location.href);
                String theme = url.searchParams.get("t");
                if (theme != null) {
                    formData.append("theme", theme);
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
                        logToProtocol(messages.errorTooLargeFile(String.valueOf(MAX_FILES_SIZE_MB)));
                        resetInputElements();
                        return;
                    }
                }

                for (String fileName : fileNames) {
                    logToProtocol(messages.uploadFile(fileName));                    
                }
                
                RequestInit init = RequestInit.create();
                init.setMethod("POST");
                init.setBody(formData);
                
                String requestUrl = protocol + "//" + host + pathname + API_JOBS_ENDPOINT;
                console.log("requestUrl jobs: " + requestUrl);

                DomGlobal.fetch(requestUrl, init)
                .then(response -> {
                    if (!response.ok) {
                        resetInputElements();
                        
                        Promise<String> foo  = response.text();
                        foo.then(r -> { 
                            logToProtocol(r);
                            return null;
                        });
                        
                        return null;
                    }
                    String jobUrl = response.headers.get(HEADER_OPERATION_LOCATION);
                    
                    if (apiTimer != null) {
                        apiTimer.cancel();
                    }
                    
                    logToProtocol(messages.validateFile());

                    apiTimer = new Timer() {
                        public void run() {                          
                            // Statt "fetch" wird XMLHttpRequest verwendet. 
                            // Diese Klasse erlaubt den Request NICHT asyncron
                            // zu machen.
                            XMLHttpRequest httpRequest = new XMLHttpRequest();
                            httpRequest.open("GET", jobUrl, false);
                            httpRequest.onload = event -> {
                                if (httpRequest.status == 200) {
                                    JsPropertyMap<?> resultJsonObj = (JsPropertyMap<?>) Global.JSON.parse(httpRequest.responseText);
                                    String jobStatus = ((JsString) resultJsonObj.get("status")).normalize();
                                    
                                    if (jobStatus.equalsIgnoreCase("SUCCEEDED")) {
                                        apiTimer.cancel();
                                        resetInputElements();

                                        String validationStatus = ((JsString) resultJsonObj.get("validationStatus")).normalize();
                                        String validationStatusTxt = messages.validationStatusTxtSuccess();
                                        if (validationStatus.equalsIgnoreCase("FAILED")) {
                                            validationStatusTxt = messages.validationStatusTxtFail();
                                        } 
                                        
                                        String logFileHref = ((JsString) resultJsonObj.get("logFileLocation")).normalize();
                                        String xtfLogFileHref = ((JsString) resultJsonObj.get("xtfLogFileLocation")).normalize();

                                        String txtLogIconLink = "<a href=\""+logFileHref+"\" target=\"blank\" class=\"badge-link\">\n"
                                                + "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"24\" height=\"24\" fill=\"currentColor\" class=\"bi bi-file-earmark-text\" viewBox=\"0 0 16 16\">\n"
                                                + "  <path d=\"M5.5 7a.5.5 0 0 0 0 1h5a.5.5 0 0 0 0-1h-5zM5 9.5a.5.5 0 0 1 .5-.5h5a.5.5 0 0 1 0 1h-5a.5.5 0 0 1-.5-.5zm0 2a.5.5 0 0 1 .5-.5h2a.5.5 0 0 1 0 1h-2a.5.5 0 0 1-.5-.5z\"></path>\n"
                                                + "  <path d=\"M9.5 0H4a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2V4.5L9.5 0zm0 1v2A1.5 1.5 0 0 0 11 4.5h2V14a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1h5.5z\"></path>\n"
                                                + "</svg>\n"
                                                + "</a>";
                                        
                                        String xtfLogIconLink = "<a href=\""+xtfLogFileHref+"\" target=\"blank\" class=\"badge-link\">\n"
                                                + "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"24\" height=\"24\" fill=\"currentColor\" class=\"bi bi-file-earmark-code\" viewBox=\"0 0 16 16\">\n"
                                                + "  <path d=\"M14 4.5V14a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V2a2 2 0 0 1 2-2h5.5L14 4.5zm-3 0A1.5 1.5 0 0 1 9.5 3V1H4a1 1 0 0 0-1 1v12a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1V4.5h-2z\"/>\n"
                                                + "  <path d=\"M8.646 6.646a.5.5 0 0 1 .708 0l2 2a.5.5 0 0 1 0 .708l-2 2a.5.5 0 0 1-.708-.708L10.293 9 8.646 7.354a.5.5 0 0 1 0-.708zm-1.292 0a.5.5 0 0 0-.708 0l-2 2a.5.5 0 0 0 0 .708l2 2a.5.5 0 0 0 .708-.708L5.707 9l1.647-1.646a.5.5 0 0 0 0-.708z\"/>\n"
                                                + "</svg>\n"
                                                + "</a>";

                                        logToProtocol(messages.validationDone() + ". " + validationStatusTxt +  ": " + txtLogIconLink + "&nbsp;" + xtfLogIconLink);
                                    }
                                } else if (httpRequest.status == 400) {
                                    apiTimer.cancel();
                                    resetInputElements();

                                    JsPropertyMap<?> resultJsonObj = (JsPropertyMap<?>) Global.JSON.parse(httpRequest.responseText);
                                     if (resultJsonObj.get("message") != null) {
                                         String message = ((JsString) resultJsonObj.get("message")).normalize();
                                         logToProtocol(messages.validationProcessingError() +": " + message);                                    
                                     } else {
                                         logToProtocol(messages.validationProcessingError() +".");                                    
                                     }                                    
                                } else {
                                    console.log("status code: " + httpRequest.status);
                                    logToProtocol(httpRequest.responseText);
                                    apiTimer.cancel();
                                    resetInputElements();
                                    DomGlobal.window.alert("Error fetching: " + jobUrl);
                                }
                            };
                            
                            httpRequest.addEventListener("error", event -> {
                                console.log("status text: " + httpRequest.statusText);
                                logToProtocol(httpRequest.responseText);
                                apiTimer.cancel();
                                resetInputElements();
                                DomGlobal.window.alert("Error fetching: " + jobUrl + "\n Error: " + httpRequest.status + " " + httpRequest.statusText);
                            });
                            
                            httpRequest.send();

                            // Wird am Ende geresettet, darum muss es händisch nachgeführt werden.
                            // TODO: Grund mir nicht klar.
                            updateComboBox(options);
                        }
                        
                    };
                    
                    apiTimer.scheduleRepeating(API_REQUEST_PERIOD_MILLIS);

                    return null;
                })
                .catch_(error -> {
                    console.log(error);
                    logToProtocol(error.toString());
                    resetInputElements();
                    return null;
                });
            }
        });
        
        container.appendChild(protocolContainer);
        
        HTMLDivElement helpElement = div().id("button-help").element();
        helpElement.appendChild(TextNode.of(messages.helpButtonText()));
        helpElement.addEventListener("click", new EventListener() {

            @Override
            public void handleEvent(Event evt) {
                ModalDialog modal = ModalDialog.create(messages.helpButtonText()).setAutoClose(true);
                modal.css("modal-object");

                modal.appendChild(
                        Row.create()
                        .appendChild(Column.span6().appendChild(TextNode.of(messages.helpGeneralLink())))
                        .appendChild(Column.span6().appendChild(a().attr("href", "https://github.com/sogis/ilivalidatorws/blob/main/docs/user-manual-de.md")
                                .attr("target", "_blank").attr("class", "default-link").add(TextNode.of("user-manual-de.md")))).element()
                        );

                modal.appendChild(
                        Row.create()
                        .appendChild(Column.span6().appendChild(TextNode.of(messages.helpLandUsePlanLink())))
                        .appendChild(Column.span6().appendChild(a().attr("href", "https://github.com/sogis/ilivalidatorws/blob/main/docs/user-manual-de-nplso.md")
                                .attr("target", "_blank").attr("class", "default-link").add(TextNode.of("user-manual-de-nplso.md")))).element()
                        );

                modal.appendChild(
                        Row.create()
                        .appendChild(Column.span6().appendChild(TextNode.of(messages.helpRestApiLink())))
                        .appendChild(Column.span6().appendChild(a().attr("href", "https://github.com/sogis/ilivalidatorws/blob/main/docs/rest-api-de.md")
                                .attr("target", "_blank").attr("class", "default-link").add(TextNode.of("rest-api-de.md")))).element()
                        );

                Button closeButton = Button.create(messages.closeButton().toUpperCase()).linkify();
                closeButton.removeWaves();
                closeButton.setBackground(Color.RED_DARKEN_3);
                EventListener closeModalListener = (evt2) -> modal.close();
                closeButton.addClickListener(closeModalListener);
                modal.appendFooterChild(closeButton);
                
                modal.open();
            }
        });
        
        container.appendChild(helpElement);
        
        
        URL url = new URL(DomGlobal.window.location.href);
        String theme = url.searchParams.get("t");

        for (int i=0; i<options.length; i++) {
            HTMLOptionElement option = options.getAt(i);
            if (option.value.equalsIgnoreCase(theme)) {
                select.selectedIndex = i;
            }
        }        
    }    
    
    private void updateComboBox(HTMLOptionsCollection options) {
        URL url = new URL(DomGlobal.window.location.href);
        String theme = url.searchParams.get("t");
       
        for (int i=0; i<options.length; i++) {
            HTMLOptionElement option = options.getAt(i);
            if (option.value.equalsIgnoreCase(theme)) {
                select.selectedIndex = i;
            }
        }        
    }
    
    private void logToProtocol(String logText) {
        protocolContainer.appendChild(div().innerHtml(SafeHtmlUtils.fromTrustedString(logText)).element());
    }
    
    private void cleanProtocol() {
        protocolContainer.innerHTML = "";    
    }
    
    private void resetInputElements() {
        form.reset();
        input.disabled = false;
        button.disabled = false;
        button.textContent = messages.submitButtonDefault();
    }
    
    private void updateUrlLocation(String theme) {
        URL url = new URL(DomGlobal.location.href);
        String host = url.host;
        String protocol = url.protocol;
        String pathname = url.pathname;
        
        String newUrl = protocol + "//" + host + pathname;
        if (theme != null) {
            URLSearchParams params = url.searchParams;
            params.set("t", theme);
            newUrl += "?" + params.toString(); 
        } 
        updateUrlWithoutReloading(newUrl);
    }

    // Update the URL in the browser without reloading the page.
    private static native void updateUrlWithoutReloading(String newUrl) /*-{
        $wnd.history.pushState(newUrl, "", newUrl);
    }-*/;

}