package ch.so.agi.ilivalidator;

public class Utils {
    /**
     * Fixes the url, e.g. finds multiple slashes in url preserving ones after protocol regardless of it. 
     * @param url
     * @return fixed url
     */
    public static String fixUrl(String url) {
        return url.replaceAll("(?<=[^:\\s])(\\/+\\/)", "/");
    }
}