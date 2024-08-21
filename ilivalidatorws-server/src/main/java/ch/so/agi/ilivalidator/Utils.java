package ch.so.agi.ilivalidator;

import java.util.HashMap;
import java.util.Map;

public class Utils {
    /**
     * Fixes the url, e.g. finds multiple slashes in url preserving ones after protocol regardless of it. 
     * @param url
     * @return fixed url
     */
    public static String fixUrl(String url) {
        return url.replaceAll("(?<=[^:\\s])(\\/+\\/)", "/");
    }
    
    public static Map<String,String> themes() {
        Map<String,String> themes = new HashMap<String,String>();
        themes.put("dmav", "ilidata:DMAV_V1_0_Validierung-meta");
        themes.put("ipw_2020", "ilidata:VSADSSMINI_2020_LV95_IPW_20230605-meta");
        themes.put("ipw_2020_1", "ilidata:VSADSSMINI_2020_1_LV95_IPW_20230605-meta");
        themes.put("drainagen", "ilidata:VSADSSMINI_2020_LV95_Drainage_20230731-meta");
        themes.put("nutzungsplanung", "ilidata:SO_Nutzungsplanung_20171118_20231101-meta");
        themes.put("naturgefahren", "ilidata:SO_AFU_Naturgefahren_20240515-web-meta");
        themes.put("gb2av", "gb2av.ini");
        themes.put("mopublic", "mopublic.ini");
//        themes.put("hba_grundstuecke", "ilidata:)
        return themes;
    }
}