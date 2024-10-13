package code;

import java.io.IOException;
public class Test {
    public static void main(String[] args) throws IOException {
        String configPath = "/mnt/c/Users/layqa/Desktop/2024_Fall/CS6378-AOS/AdvOS-Assignments/Assignment1/config-local.txt";
        // int totalNodes = 5;
        int i = 0;
        // for (int i = 0; i < totalNodes; i++) {
            System.out.println("\n\n\n==========> TESTING FOR NODE " + i + " <=======");
            Runner.processConfig(configPath, i);
        // }
    }

}
