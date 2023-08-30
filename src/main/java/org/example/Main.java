package org.example;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("### Automation ###\n");

        FileManager fileManager = new FileManager();
        fileManager.createDefaultDirectory();

        Prompt prompt = new Prompt();
        String userSelectedAutomationMode = prompt.userSelectAutomationMode();

        Postgres postgres = new Postgres();
        if (userSelectedAutomationMode.equals("1")) {
            postgres.dumpScenario();
        } else if (userSelectedAutomationMode.equals("2")) {
            postgres.restoreScenario();
        } else {
            System.out.println("구현 준비중");
        }
    }
}
