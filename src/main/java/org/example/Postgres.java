package org.example;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Postgres {
    private static final Prompt PROMPT = new Prompt();

    void dumpScenario() throws Exception {
        Map<String, String> databaseInfo;
        String selectedUserDumpEnvironment = PROMPT.userSelectEnvironment("Dump");

        if (selectedUserDumpEnvironment.equals("CONFIG")) {
            IniConfigManager iniConfigManager = new IniConfigManager();
            databaseInfo = getDatabaseInfoFromConfig(iniConfigManager, "./config/dump.ini");
        } else {
            databaseInfo = PROMPT.userInputDatabaseInfo("Dump");
        }

        processDatabaseDump(databaseInfo);
    }

    private Map<String, String> getDatabaseInfoFromConfig(IniConfigManager iniConfigManager, String filePath) {
        Map<String, Map<String, String>> databaseSectionMap = iniConfigManager.convertIniToObjects(filePath);
        return PROMPT.promptUserForSection(databaseSectionMap);
    }

    private void processDatabaseDump(Map<String, String> databaseInfo) throws Exception {
        String[] databaseList = getDatabaseList(databaseInfo);
        String[] userSelectedDatabaseList = PROMPT.userSelectDatabaseList(databaseList);
        performDatabaseDump(userSelectedDatabaseList, databaseInfo);
    }

    public String[] getDatabaseList(Map<String, String> databaseInfo) throws Exception {
        String url = String.format("jdbc:postgresql://%s:%s/postgres", databaseInfo.get("host"), databaseInfo.get("port"));
        List<String> databaseList = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url, databaseInfo.get("user"), databaseInfo.get("password"));
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT datname FROM pg_database;")) {

            Class.forName("org.postgresql.Driver");

            while (rs.next()) {
                String dbName = rs.getString("datname");
                if (!dbName.matches(".*template.*") && !dbName.matches(".*postgres.*") && !dbName.matches(".*rdsadmin.*")) {
                    databaseList.add(dbName);
                }
            }
        }

        return databaseList.toArray(new String[0]);
    }

    private void performDatabaseDump(String[] dumpDatabases, Map<String, String> databaseInfo) throws IOException, InterruptedException {
        for (String databaseName : dumpDatabases) {
            boolean isSuccess = pgDump(databaseName, databaseInfo, generateDumpFileName(databaseName));
            if (isSuccess) {
                System.out.println(databaseName + " 데이터베이스 Dump에 성공하였습니다");
            } else {
                System.out.println(databaseName + " 데이터베이스 Dump에 실패하였습니다");
            }
        }
    }

    private String generateDumpFileName(String databaseName) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm");
        String currentTimestamp = LocalDateTime.now().format(formatter);
        return String.format("./temp/%s_%s.dump", databaseName, currentTimestamp);
    }

    private boolean pgDump(String dbName, Map<String, String> databaseInfo, String path) throws IOException, InterruptedException {
        String host = databaseInfo.get("host");
        String port = databaseInfo.get("port");
        String username = databaseInfo.get("user");
        String password = databaseInfo.get("password");

        String[] cmdArray = new String[]{
                "pg_dump",
                "-U", username,
                "-h", host,
                "-p", port,
                "-d", dbName,
                "-Fc",
                "-f", path
        };

        ProcessBuilder processBuilder = new ProcessBuilder(cmdArray);
        processBuilder.environment().put("PGPASSWORD", password);

        Process process = processBuilder.start();
        return process.waitFor() == 0;
    }


    void restoreScenario() throws SQLException, ClassNotFoundException, IOException, InterruptedException {
        Map<String, String> databaseInfo;

        String folderPath = "./temp";
        List<String> selectedFiles = PROMPT.selectDumpFiles(folderPath);

        String selectedUserRestoreEnvironment = PROMPT.userSelectEnvironment("Restore");

        if (selectedUserRestoreEnvironment.equals("CONFIG")) {
            IniConfigManager iniConfigManager = new IniConfigManager();
            databaseInfo = getDatabaseInfoFromConfig(iniConfigManager, "./config/restore.ini");
            System.out.println("11");
        } else {
            databaseInfo = PROMPT.userInputDatabaseInfo("Restore");
        }

        processDatabaseRestore(databaseInfo, selectedFiles);
    }

    private void processDatabaseRestore(Map<String, String> databaseInfo, List<String> selectedFiles) throws SQLException, ClassNotFoundException, IOException, InterruptedException {
        for (String filePath : selectedFiles) {
            File file = new File(filePath);
            String fileName = file.getName();
            String[] parts = fileName.split("_");

            String databaseName = parts[0];

            if (pgDatabaseChecking(databaseName, databaseInfo)) {
                String userSelectedAction = PROMPT.getUserActionInput(databaseName);

                if (userSelectedAction.equals("1")) {
                    if (pgDeleteDatabase(databaseName, databaseInfo)) {
                        System.out.println("\n" + databaseName + " 데이터베이스를 삭제하였습니다.");
                    } else {
                        System.out.println("\n" + databaseName + " 데이터베이스를 삭제하지 못하였습니다.");
                    }
                }
            }

            if (pgCreateDatabase(databaseName, databaseInfo)) {
                System.out.println(databaseName + " 데이터베이스를 생성하였습니다.");
            }

            pgRestore(databaseName, databaseInfo, filePath);
        }

    }

    public boolean pgDatabaseChecking(String dbName, Map<String, String> databaseInfo) {
        String host = databaseInfo.get("host");
        String port = databaseInfo.get("port");
        String username = databaseInfo.get("user");
        String password = databaseInfo.get("password");

        String url = "jdbc:postgresql://" + host + ":" + port + "/postgres";

        try {
            Class.forName("org.postgresql.Driver");

            try (Connection conn = DriverManager.getConnection(url, username, password);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(String.format("SELECT EXISTS(SELECT 1 FROM pg_database WHERE datname = '%s') AS result;", dbName))) {

                if (rs.next()) {
                    return rs.getBoolean("result");
                }
            }

        } catch (ClassNotFoundException e) {
            System.out.println("PostgreSQL 드라이버를 찾을 수 없습니다.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("데이터베이스 서버 연결에 실패하였습니다.\n외부접속 허용 여부를 확인해주세요.");
            e.printStackTrace();
        }

        return false;
    }

    public boolean pgCreateDatabase(String dbName, Map<String, String> databaseInfo) throws IOException, InterruptedException {
        String host = databaseInfo.get("host");
        String port = databaseInfo.get("port");
        String username = databaseInfo.get("user");
        String password = databaseInfo.get("password");
        String[] createDbCmdArray = new String[]{
                "createdb",
                "-U", username,
                "-h", host,
                "-p", port,
                dbName
        };

        ProcessBuilder createDbProcessBuilder = new ProcessBuilder(createDbCmdArray);
        createDbProcessBuilder.environment().put("PGPASSWORD", password);

        Process createDbProcess = createDbProcessBuilder.start();

        return createDbProcess.waitFor() == 0;
    }

    public boolean pgDeleteDatabase(String dbName, Map<String, String> databaseInfo) throws IOException, InterruptedException {
        String host = databaseInfo.get("host");
        String port = databaseInfo.get("port");
        String username = databaseInfo.get("user");
        String password = databaseInfo.get("password");

        String[] deleteDbCmdArray = new String[]{
                "dropdb",
                "-U", username,
                "-h", host,
                "-p", port,
                dbName
        };

        ProcessBuilder deleteDbProcessBuilder = new ProcessBuilder(deleteDbCmdArray);
        deleteDbProcessBuilder.environment().put("PGPASSWORD", password);

        Process deleteDbProcess = deleteDbProcessBuilder.start();

        return deleteDbProcess.waitFor() == 0;
    }


    public void pgRestore(String dbName, Map<String, String> databaseInfo, String path) throws IOException, InterruptedException {
        String host = databaseInfo.get("host");
        String port = databaseInfo.get("port");
        String username = databaseInfo.get("user");
        String password = databaseInfo.get("password");

        String[] cmdArray = new String[]{
                "pg_restore",
                "-U", username,
                "-h", host,
                "-p", port,
                "-d", dbName,
                "--no-owner",
                path
        };

        ProcessBuilder processBuilder = new ProcessBuilder(cmdArray);
        processBuilder.environment().put("PGPASSWORD", password);

        Process process = processBuilder.start();

        process.waitFor();
    }
}
