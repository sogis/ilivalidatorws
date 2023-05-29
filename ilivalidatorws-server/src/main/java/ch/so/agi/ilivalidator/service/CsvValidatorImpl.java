package ch.so.agi.ilivalidator.service;

import java.io.File;

import ch.interlis.iox_j.PipelinePool;
import org.interlis2.validator.Validator;

import ch.ehi.basics.settings.Settings;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.iom_j.csv.CsvReader;
import ch.interlis.iox.IoxException;
import ch.interlis.iox.IoxReader;
import ch.interlis.iox_j.logging.LogEventFactory;

public class CsvValidatorImpl extends Validator {
//    @Override
//    protected IoxReader createReader(String filename, TransferDescription td, LogEventFactory errFactory,
//            Settings settings, PipelinePool pool) throws IoxException {
//        CsvReader reader = new CsvReader(new File(filename), settings);
//        reader.setModel(td);
//        boolean firstLineIsHeader = false;
//        {
//            String val = settings.getValue(CsvValidatorService.SETTING_FIRSTLINE_IS_HEADER);
//            if (Boolean.parseBoolean(val)) {
//                firstLineIsHeader = true;
//            }
//        }
//        reader.setFirstLineIsHeader(firstLineIsHeader);
//        char valueDelimiter = CsvValidatorService.SETTING_VALUEDELIMITER_DEFAULT;
//        {
//            String val = settings.getValue(IoxWkfConfig.SETTING_VALUEDELIMITER);
//            if (val != null) {
//                valueDelimiter = val.charAt(0);
//            }
//        }
//        reader.setValueDelimiter(valueDelimiter);
//        char valueSeparator = IoxWkfConfig.SETTING_VALUESEPARATOR_DEFAULT;
//        {
//            String val = settings.getValue(IoxWkfConfig.SETTING_VALUESEPARATOR);
//            if (val != null) {
//                valueSeparator = val.charAt(0);
//            }
//        }
//        reader.setValueSeparator(valueSeparator);
//        return reader;
//    }

}
