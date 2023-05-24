package ch.so.agi.ilivalidator;

import static elemental2.dom.DomGlobal.console;
import static elemental2.dom.DomGlobal.fetch;
import static org.jboss.elemento.Elements.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.dominokit.domino.ui.badges.Badge;
import org.dominokit.domino.ui.breadcrumbs.Breadcrumb;
import org.dominokit.domino.ui.button.Button;
import org.dominokit.domino.ui.datatable.ColumnConfig;
import org.dominokit.domino.ui.datatable.DataTable;
import org.dominokit.domino.ui.datatable.TableConfig;
import org.dominokit.domino.ui.datatable.store.LocalListDataStore;
import org.dominokit.domino.ui.forms.TextBox;
import org.dominokit.domino.ui.grid.Column;
import org.dominokit.domino.ui.grid.Row;
import org.dominokit.domino.ui.icons.Icons;
import org.dominokit.domino.ui.infoboxes.InfoBox;
import org.dominokit.domino.ui.modals.ModalDialog;
import org.dominokit.domino.ui.preloaders.Preloader;
import org.dominokit.domino.ui.style.Color;
import org.dominokit.domino.ui.style.ColorScheme;
import org.dominokit.domino.ui.tabs.Tab;
import org.dominokit.domino.ui.tabs.TabsPanel;
import org.dominokit.domino.ui.themes.Theme;
import org.dominokit.domino.ui.utils.TextNode;

import com.google.gwt.core.client.GWT;
import org.gwtproject.safehtml.shared.SafeHtmlUtils;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.i18n.client.DateTimeFormat;

import elemental2.dom.CSSProperties;
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
import elemental2.dom.Location;
import elemental2.dom.RequestInit;
import elemental2.dom.XMLHttpRequest;

public class App implements EntryPoint {

    // Application settings
    private String myVar;

    // Format settings
    private NumberFormat fmtDefault = NumberFormat.getDecimalFormat();
    private NumberFormat fmtPercent = NumberFormat.getFormat("#0.0");

    private static int MAX_FILES_SIZE_MB = 300; 
    
//    private static final String API_PATH_UPLOAD = "rest/jobs";
    private static final String API_PATH_UPLOAD = "api/jobs";
    private static final String HEADER_OPERATION_LOCATION = "Operation-Location";
    
    private Timer apiTimer;
    private static final int API_REQUEST_PERIOD_MILLIS = 2000;
    
    private HTMLFormElement form;
    private HTMLInputElement input;
    private HTMLButtonElement button;
    private HTMLDivElement protocolContainer;
    
    public void onModuleLoad() {
        init();
    }
    
    public void init() {        
        // Change Domino UI color scheme.
        Theme theme = new Theme(ColorScheme.RED);
        theme.apply();
        
        console.log("Hallo Stefan.");
                
        // Get url from browser (client) to find out the correct location of resources.
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
        String infoString = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr.";
        container.appendChild(div().css("info").innerHtml(SafeHtmlUtils.fromTrustedString(infoString)).element());
        
        
        protocolContainer = div().id("protocol-container").element();
        protocolContainer.innerHTML = "adsf <br> adf";
        
        form = (HTMLFormElement) document.createElement("form");
        form.id = "upload-form";
        form.enctype = "multipart/form-data";
        form.action = "";        
        container.appendChild(form);
        
        input = (HTMLInputElement) document.createElement("input");
        input.setAttribute("type", "file");
        input.setAttribute("name", "files");
        input.multiple = true;
        input.accept = ".itf,.xtf,.xml";
        form.appendChild(input);

        button = (HTMLButtonElement) document.createElement("button");
        button.className = "submit-button";
        button.textContent = "Submit"; // TODO i18n
        form.appendChild(button);
        
        form.addEventListener("submit", new EventListener() {
            @Override
            public void handleEvent(Event evt) {
                evt.preventDefault();
                
                if (input.files.length < 1) {
                    return;
                }
                
                button.textContent = "Validating..."; // TODO i18n

                input.disabled = true;
                button.disabled = true;
                //form.appendChild(Preloader.create().element());
                
                FormData formData = new FormData();

                int filesSize = 0;
                for (int i=0; i<input.files.length; i++) {
                    File file = input.files.getAt(i);
                    // Falls Safari Probleme macht:
                    // https://github.com/hal/console/blob/d435e24a837adedbd6aefe06495eafaa97c08a65/dmr/src/main/java/org/jboss/hal/dmr/dispatch/Dispatcher.java#L263
                    formData.append("files", AppendValueUnionType.of(file), file.name);
                    filesSize += file.size;
                    console.log(filesSize);
                    int filesSizeMb = filesSize / 1024 / 1024;
                    if (filesSizeMb > MAX_FILES_SIZE_MB) {
                        // TODO log to protocol
                        protocolContainer.appendChild(div().textContent("zu gross").element());
                        resetInputElements();
                        return;
                    }
                }

                RequestInit init = RequestInit.create();
                init.setMethod("POST");
                init.setBody(formData);
                
                console.log("form submit...");
                
                DomGlobal.fetch(API_PATH_UPLOAD, init)
                .then(response -> {
                    if (!response.ok) {
                        resetInputElements();
                        
                        // TODO log to protocol..
                        
                        return null;
                    }
                    String jobUrl = response.headers.get(HEADER_OPERATION_LOCATION);
                    console.log(jobUrl);
                    
                    if (apiTimer != null) {
                        apiTimer.cancel();
                    }
                    
                    apiTimer = new Timer() {
                        public void run() {
                            // showElapsed();
                            console.log("fubar: " + jobUrl);
                            
                            // Statt "fetch" wird XMLHttpRequest verwendet. 
                            // Diese Klasse erlaubt den Request NICHT asyncron
                            // zu machen.
                            XMLHttpRequest httpRequest = new XMLHttpRequest();
                            httpRequest.open("GET", jobUrl, false);
                            httpRequest.onload = event -> {
                                if (httpRequest.status == 200) {
                                    console.log(httpRequest.responseText);
                                    
                                    
                                    
                                    if (httpRequest.responseText.equalsIgnoreCase("SUCCEEDED")) {
                                        console.log("cancel timer");
                                        apiTimer.cancel();
                                        
                                        resetInputElements();

                                        protocolContainer.innerHTML = "downloade mich";
                                    }
                                    
                                    
                                
                                } else {
                                    console.log("status code: " + httpRequest.status);
                                    DomGlobal.window.alert("Error fetching: " + jobUrl);
                                }
                            };
                            
                            httpRequest.addEventListener("error", event -> {
                                console.log("status text: " + httpRequest.statusText);
                                DomGlobal.window
                                        .alert("Error fetching: " + jobUrl + "\n Error: " + httpRequest.status + " " + httpRequest.statusText);
                            });
                            
                            httpRequest.send();
                        }
                    };
                    
                    apiTimer.scheduleRepeating(API_REQUEST_PERIOD_MILLIS);
                    
                    //return response.json();
                    return null;
                })
//                .then(json -> {
//                    console.log(json);
//                    
//                    return null;
//                })
                .catch_(error -> {
                    console.log(error);
                    // TODO log to protocol
                    resetInputElements();
                    return null;
                });
            }
        });
        
        container.appendChild(protocolContainer);

    }    
    
    private void resetInputElements() {
        form.reset();
        input.disabled = false;
        button.disabled = false;
        button.textContent = "Submit"; // TODO i18n
    }
}