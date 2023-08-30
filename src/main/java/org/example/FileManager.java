package org.example;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileManager {

    void createDefaultDirectory() {
        ensureDirectoryExists("temp");
        ensureDirectoryExists("config");
    }

    private void ensureDirectoryExists(String directory) {
        Path folderPath = Paths.get(".", directory);

        if (!Files.exists(folderPath)) {
            try {
                Files.createDirectory(folderPath);
            } catch (IOException e) {
                System.out.println("Failed to create the ./" + directory + " folder due to an error: " + e.getMessage());
            }
        }

        createDefaultConfigFile(directory, "/dump.ini");
        createDefaultConfigFile(directory, "/restore.ini");
    }

    private void createDefaultConfigFile(String directory, String fileName) {
        if (directory.equals("config")) {
            String dirPath = "./config";
            String filePath = dirPath + fileName;

            File directoryPath = new File(dirPath);
            File file = new File(filePath);

            if (!directoryPath.exists()) directoryPath.mkdir();

            if (!file.exists()) {
                try (FileWriter writer = new FileWriter(file)) {
                    String content = "[docker]\n";
                    content += "host=localhost\n";
                    content += "port=5432\n";
                    content += "user=postgres\n";
                    content += "password=postgrespw\n";

                    writer.write(content);
                } catch (IOException e) {
                    System.out.println("An error occurred while creating the file: " + e.getMessage());
                }
            }
        }
    }
}
