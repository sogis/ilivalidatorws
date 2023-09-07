package ch.so.agi.ilivalidator;

import com.google.gwt.i18n.client.Messages;

public interface MyMessages extends Messages {
    @DefaultMessage("Fubar")
    String fubar();
    
    @DefaultMessage("Füü {0} bar {1}")
    String yinyang(String yin, String yang);
    
    @DefaultMessage("Submit") 
    String submitButtonDefault();
    
    @DefaultMessage("Choose validation profile") 
    String validationProfileSelect();

    @DefaultMessage("default (no additional tests)") 
    String validationProfileSelectDefault();

    @DefaultMessage("Validating ...") 
    String submitButtonValidate();
    
    @DefaultMessage("File is too large (larger than {0} MB).") 
    String errorTooLargeFile(String maxFileSize);

    @DefaultMessage("Uploding {0} ...") 
    String uploadFile(String fileName);

    @DefaultMessage("The file is being validated ...") 
    String validateFile();

    @DefaultMessage("The file cannot be validated") 
    String validationProcessingError();

    @DefaultMessage("Validation done") 
    String validationDone();
    
    @DefaultMessage("No errors found") 
    String validationStatusTxtSuccess();

    @DefaultMessage("Errors found") 
    String validationStatusTxtFail();
    
    @DefaultMessage("Info & Help") 
    String helpButtonText();

    @DefaultMessage("General tutorial") 
    String helpGeneralLink();

    @DefaultMessage("Tutorial for land-use planning") 
    String helpLandUsePlanLink();

    @DefaultMessage("Tutorial REST-API") 
    String helpRestApiLink();
    
    @DefaultMessage("Schliessen") 
    String closeButton();
}
