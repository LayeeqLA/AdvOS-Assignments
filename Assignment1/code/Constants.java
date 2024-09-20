package code;

import java.util.Random;

public class Constants {
    public static final int MAX_MSG_SIZE = 4096;
    public static final int BASE_NODE = 0;
    // public static final int MAX_MESSAGES = 10;
    // public static final int MIN_WAIT = 2;
    // public static final int MAX_WAIT = 16;
    public static final int MIN_RANGE = 0;
    public static final int MAX_RANGE = 1000;
    public static final long CONNECT_WAIT = 5000L;
    public static final int CONNECT_MAX_ATTEMPTS = 5;

    public static int getRandomNumber(int min, int max) {
        Random random = new Random();
        return random.nextInt(max + 1 - min) + min; // min & max inclusive
    }

    // public static int getRandomWait() {
    //     return getRandomNumber(MIN_WAIT, MAX_WAIT) * 1000;
    // }

    public static int getRandomBroadcastInt() {
        return getRandomNumber(MIN_RANGE, MAX_RANGE);
    }

    public static boolean isConfigLineValid(String line) {
        if ("".equalsIgnoreCase(line)) {
            // ignore full comment line
            return false;
        }
        try {
            Integer.parseInt(line.split(" ")[0]);
        } catch (NumberFormatException e) {
            // line does not start with integer
            return false;
        }
        return true;
    }
}
