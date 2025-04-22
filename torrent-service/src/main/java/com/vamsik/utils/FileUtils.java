package com.vamsik.utils;

import java.io.File;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class FileUtils {

    private final static Logger logger = Logger.getLogger(FileUtils.class.getName());

    @SuppressWarnings({"CallToPrintStackTrace"})
    public static void createFolder(String dirName) {
        try {
            // TODO : add the user home to the directory

            Path dirPath = Path.of(dirName);
            Path newPath = Files.createDirectory(dirPath);

            logger.info("Folder created in path " + newPath.toAbsolutePath());
        } catch (FileAlreadyExistsException e) {
            logger.info("Folder already exists with the name " + dirName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static long getFileSize(String dirName, String fileName) {
        try {
            Path dirPath = Path.of(dirName, fileName).toAbsolutePath();
            File file = new File(dirPath.toString());
            return file.length();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static void createFile(String dirName, String fileName) {
        try {
            Path dirPath = Path.of(dirName, fileName).toAbsolutePath();
            File file = new File(dirPath.toString());

            boolean created = file.createNewFile();

            if (!created) {
                logger.info(file.getName() + " not created");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getFullPath(String dirName, String fileName) {
        Path dirPath = Path.of(dirName, fileName).toAbsolutePath();
        return dirPath.toString();
    }

    public static String[] walkDirectory(String dirName) {
        try {
            Path dirPath = Path.of(dirName).toAbsolutePath();
            List<String> fileList = new ArrayList<>();

            // walk the directory
            for (String fileName : Objects.requireNonNull(dirPath.toFile().list())) {
                // check if the file is a file
                if (new File(dirPath + "/" + fileName).isFile()) {
                    fileList.add(fileName);
                }
            }

            return fileList.toArray(new String[0]);
        } catch (Exception e) {
            e.printStackTrace();
            return new String[0];
        }
    }


    public static boolean checkFile(String dirName, String fileName) {
        try {
            Path dirPath = Path.of(dirName, fileName).toAbsolutePath();
            File file = new File(dirPath.toString());
            return file.exists();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
