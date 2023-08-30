package org.example;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class Prompt {
    private static final Scanner SCANNER = new Scanner(System.in);

    String userSelectAutomationMode() {
        final List<String> validOptions = Arrays.asList("1", "2");

        while (true) {
            System.out.println("Automation Mode를 선택해주세요.");
            System.out.println("1. Dump");
            System.out.println("2. Restore");
            System.out.print("mode : ");

            String userInput = SCANNER.nextLine();

            if (validOptions.contains(userInput)) return userInput;
            System.out.println("\n모드를 다시 선택해주세요.\n");
        }
    }

    String userSelectEnvironment(String type) {
        String selectedEnvironment = null;

        while (true) {
            System.out.println("\n" + type + " 할 환경을 선택해주세요.");
            System.out.println("1. CONFIG");
            System.out.println("2. CUSTOM");

            System.out.print(type + " 환경 : ");

            String userInput = SCANNER.nextLine();

            if (Objects.equals(userInput, "1")) {
                selectedEnvironment = "CONFIG";
            } else if (Objects.equals(userInput, "2")) {
                selectedEnvironment = "CUSTOM";
            }

            if (selectedEnvironment != null) {
                return selectedEnvironment;
            }

            System.out.println("\n" + type + " 환경을 다시 선택해주세요.");
        }
    }

    Map<String, String> userInputDatabaseInfo(String type) {
        String[] infoKeys = {"host", "port", "user", "password"};
        Map<String, String> databaseInfo = new HashMap<>();

        System.out.println("\n" + type + " 데이터베이스 서버 정보를 입력해주세요 :");

        for (String key : infoKeys) {
            System.out.print(type + " server " + key + ": ");
            databaseInfo.put(key, SCANNER.nextLine());
        }

        System.out.println("\n" + type + " 데이터베이스 서버 정보:");
        databaseInfo.forEach((key, value) -> System.out.println(key + ": " + value));

        return databaseInfo;
    }

    public String[] userSelectDatabaseList(String[] databaseList) {
        List<String> selectDatabaseList = new ArrayList<>(Arrays.asList(databaseList));
        selectDatabaseList.add("all");

        Set<Integer> selectedIndices;

        do {
            System.out.println("\n### 데이터베이스 선택 ###");
            for (int i = 0; i < selectDatabaseList.size(); i++) {
                System.out.println(i + ". " + selectDatabaseList.get(i));
            }
            System.out.print("선택할 데이터베이스 번호를 ','로 구분하여 입력해주세요 (예: 0,2,4) : ");

            String input = SCANNER.nextLine();
            String[] splitInputs = input.split(",");
            selectedIndices = new HashSet<>();

            for (String item : splitInputs) {
                int index = Integer.parseInt(item.trim());
                if (index >= 0 && index < selectDatabaseList.size()) {
                    selectedIndices.add(index);
                }
            }

            if (selectedIndices.isEmpty()) {
                System.out.println("\n올바른 번호를 입력해주세요.\n");
            }
        } while (selectedIndices.isEmpty());

        if (selectedIndices.contains(selectDatabaseList.size() - 1)) { // "all" option selected
            return databaseList;
        }

        return selectedIndices.stream()
                .map(selectDatabaseList::get)
                .toArray(String[]::new);
    }

    public Map<String, String> promptUserForSection(Map<String, Map<String, String>> sectionsMap) {
        Scanner scanner = new Scanner(System.in);
        int index = 1;
        Map<Integer, String> indexToSectionName = new HashMap<>();

        System.out.println("\n데이터베이스 환경을 선택해주세요.");

        for (String sectionName : sectionsMap.keySet()) {
            System.out.println(index + ". " + sectionName);
            indexToSectionName.put(index, sectionName);
            index++;
        }

        int selected = 0;
        do {
            System.out.print("환경: ");
            try {
                selected = scanner.nextInt();
                if (selected < 1 || selected > sectionsMap.size()) {
                    System.out.println("\n환경을 다시 선택해주세요.");
                    selected = 0;
                }
            } catch (Exception e) {
                System.out.println("Please enter a valid number.");
                scanner.next();
            }
        } while (selected == 0);

        String selectedSectionName = indexToSectionName.get(selected);
        return sectionsMap.get(selectedSectionName);
    }

    public List<String> selectDumpFiles(String folderPath) {
        File folder = new File(folderPath);
        File[] listOfFiles = folder.listFiles((dir, name) -> name.endsWith(".dump"));

        if (listOfFiles == null || listOfFiles.length == 0) {
            System.out.println("No .dump files found in the directory.");
            return Collections.emptyList();
        }

        Map<String, List<File>> groupedByDatabase = new HashMap<>();
        for (File file : listOfFiles) {
            String fileName = file.getName();
            String databaseName = fileName.split("_")[0];
            groupedByDatabase
                    .computeIfAbsent(databaseName, k -> new ArrayList<>())
                    .add(file);
        }

        List<String> selectedFiles = new ArrayList<>();
        List<String> databaseNames = new ArrayList<>(groupedByDatabase.keySet());

        Set<Integer> selectedIndices;
        do {
            System.out.println("\n### 데이터베이스 선택 ###");
            for (int i = 0; i < databaseNames.size(); i++) {
                System.out.println((i + 1) + ". " + databaseNames.get(i));
            }
            System.out.print("데이터베이스 번호를 ','로 구분하여 입력해주세요 (예: 1,2,3): ");

            String input = SCANNER.nextLine();
            String[] splitInputs = input.split(",");
            selectedIndices = new HashSet<>();

            for (String item : splitInputs) {
                int index = Integer.parseInt(item.trim()) - 1;
                if (index >= 0 && index < databaseNames.size()) {
                    selectedIndices.add(index);
                }
            }

            if (selectedIndices.isEmpty()) {
                System.out.println("올바른 번호를 입력해주세요.");
            }
        } while (selectedIndices.isEmpty());

        for (int selectedIndex : selectedIndices) {
            String selectedDatabase = databaseNames.get(selectedIndex);
            List<File> files = groupedByDatabase.get(selectedDatabase);
            List<String> filePaths = files.stream()
                    .map(File::getAbsolutePath)
                    .collect(Collectors.toList());

            System.out.println("\n### 파일 선택: " + selectedDatabase + " ###");
            for (int i = 0; i < filePaths.size(); i++) {
                System.out.println((i + 1) + ". " + filePaths.get(i));
            }

            int selectedFileIndex;
            do {
                System.out.print("파일을 선택해주세요: ");
                selectedFileIndex = SCANNER.nextInt();
                SCANNER.nextLine();
                if (selectedFileIndex < 1 || selectedFileIndex > filePaths.size()) {
                    System.out.println("올바른 번호를 입력해주세요.");
                    selectedFileIndex = 0;
                }
            } while (selectedFileIndex == 0);

            selectedFiles.add(filePaths.get(selectedFileIndex - 1));
        }

        return selectedFiles;
    }

    public String getUserActionInput(String databaseName) {
        String selectedOption = null;

        while (selectedOption == null) {
            System.out.printf("\nRestore 서버에 %s 데이터베이스가 이미 존재합니다.\n", databaseName);
            System.out.println("1. 삭제 후 Dump 실행하기");
            System.out.println("2. 건너띄기");
            System.out.print("액션: ");

            String userInput = SCANNER.nextLine();

            if (userInput.equals("1") || userInput.equals("2")) {
                selectedOption = userInput;
            } else {
                System.out.println("잘못 입력하셨습니다. 다시 입력해주세요.");
            }
        }

        return selectedOption;
    }
}
