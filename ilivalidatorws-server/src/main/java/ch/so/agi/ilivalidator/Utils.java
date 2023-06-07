package ch.so.agi.ilivalidator;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Utils {
    /**
     * Returns the (partial) path as part of an url at which the log file is available.
     * @param logFilename
     * @return The unique (partial) path of the log file url.
     */
//    public static String getLogFileUrlPathElement(String logFileName) {
//        return new File(new File(logFileName).getParent()).getName() + "/" + new File(logFileName).getName();
//    }
    
//    /**
//     * Returns the log file's absolute path of the input file.
//     * @param inputFileName
//     * @return The absolute file path.
//     */
//    public static String getLogFileName(String inputFileName) {
//        return new File(inputFileName).getAbsolutePath() + ".log";
//    }

    /**
     * Returns the log file's path of the input files by using the first file's path.
     * @param inputFiles
     * @return The log file path.
     */
//    public static String getLogFileName(Path[] inputFiles) {
//        String logFileName = new ArrayList<>(Arrays.asList(inputFiles)).stream().map(p -> {
//            return p.getFileName().toString().toLowerCase();
//        }).collect(Collectors.joining("_"));
//
//        return logFileName + ".log";
//    }
    
    /**
    * Returns the log file's path of the input files by using the first file's path.
    * @param inputFiles
    * @return The log file path.
    */
//   public static Path getLogFile(String[] inputFileNames) {
//       Path parentPath = Paths.get(inputFileNames[0]).getParent();
//       
//       String logFileName = new ArrayList<>(Arrays.asList(inputFileNames)).stream().map(f -> {
//           return Paths.get(f).getFileName().toString().toLowerCase();
//       }).collect(Collectors.joining("_"));
//
//       return parentPath.resolve(parentPath.getFileSystem().getPath(logFileName + ".log"));
//   }

      
    /**
     * Returns the log file's absolute path of the input file.
     * @param inputFile
     * @return The absolute file path.
     */
//    public static String getLogFileName(File inputFile) {
//        return getLogFileName(inputFile.getAbsolutePath());
//    }
    
    /**
     * Returns the log file's absolute path of the input file.
     * @param inputFile
     * @return The absolute file path.
     */
//    public static String getLogFileName(Path inputFile) {
//        return getLogFileName(inputFile.toFile());
//    }

    /**
     * Returns the xtf log file's absolute path of the input file.
     * @param inputFileName
     * @return The absolute file path.
     */
//    public static String getXtfLogFileName(String inputFileName) {
//        return getLogFileName(inputFileName) + ".xtf";
//    }

    /**
     * Fixes the url, e.g. finds multiple slashes in url preserving ones after protocol regardless of it. 
     * @param url
     * @return fixed url
     */
    public static String fixUrl(String url) {
        return url.replaceAll("(?<=[^:\\s])(\\/+\\/)", "/");
    }
}