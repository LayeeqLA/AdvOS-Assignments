package code;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConsistencyChecker {

    public static void checkGlobalStateConsistency(int nodeCount, String configPath) throws IOException {

        /*
         * ASSUMES NODE IDs ARE IN ORDER: 0 ... n-1
         */

        String baseOutputPath = configPath;
        if (baseOutputPath.endsWith(".txt")) {
            baseOutputPath = baseOutputPath.substring(0, baseOutputPath.length() - 4);
        }
        List<String> paths = new ArrayList<>();
        for (int i = 0; i < nodeCount; i++) {
            paths.add(baseOutputPath + "-" + i + ".out");
        }

        List<List<VectorClock>> globalState = new ArrayList<>();
        for (String path : paths) {
            List<VectorClock> localState = new ArrayList<>();
            List<String> allLines = Files.readAllLines(Paths.get(path));
            // FIX BOM encoding for UTF-16 and UTF-8 config files
            String firstLine = allLines.get(0);
            if (firstLine.codePointAt(0) == 0xfeff) {
                allLines.set(0, firstLine.substring(1, firstLine.length()));
            }
            if ("".equalsIgnoreCase(allLines.get(allLines.size() - 1).trim())) {
                // remove last line if blank
                allLines.remove(allLines.size() - 1);
            }
            for (String snapshotLine : allLines) {
                localState.add(
                        new VectorClock(Arrays.stream(snapshotLine.trim().split(" "))
                                .mapToInt(Integer::parseInt).toArray()));
            }
            globalState.add(localState);
        }

        // ensuring all have correct records
        for (int i = 0; i < globalState.size() - 1; i++) {
            if (globalState.get(i).size() != globalState.get(i + 1).size()) {
                System.out.println("Incorrect state counts between files");
                System.exit(1);
            }
        }

        boolean consistent = true;
        for (int i = 0; i < globalState.get(0).size(); i++) {
            VectorClock vcMax = new VectorClock(nodeCount);
            for (int j = 0; j < globalState.size(); j++) {
                for (int k = 0; k < globalState.size(); k++) {
                    int value = globalState.get(j).get(i).get(k);
                    if (vcMax.get(k) < value) {
                        vcMax.set(k, value);
                    }
                }
            }

            // vcMax.print(" VC MAX " + i + " : ");

            for (int p = 0; p < vcMax.length(); p++) {
                if (vcMax.get(p) != globalState.get(p).get(i).get(p)) {
                    System.out.println("GLOBAL STATE " + i + " inconsistent at node " + p);
                    consistent = false;
                }
            }
        }

        if (consistent) {
            System.out.println("Global state is consistent" + System.lineSeparator());
        }

    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Need exactly TWO args!");
            System.exit(-1);
        }
        int nodeCount = Integer.parseInt(args[0]);
        String configPath = args[1];

        checkGlobalStateConsistency(nodeCount, configPath);

    }

}
