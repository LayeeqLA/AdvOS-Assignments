package code;

public class AliveProbe {

    // FALSE -> PASSIVE
    // TRUE -> ACTIVE
    private boolean status;
    private int maxNumber; // of messages that can be sent
    private int currentSent;

    public synchronized boolean isAlive() {
        return status;
    }

    public synchronized void setActive() {
        if (currentSent < maxNumber) {
            status = true;
        }
    }

    public synchronized void setPassive() {
        status = false;
    }

    public synchronized void increment() {
        currentSent++;
    }

    public synchronized int getMessageCount() {
        return currentSent;
    }

    public synchronized boolean isTerminated() {
        // PASSIVE AND MAX NUMBER OF MESSAGES SENT
        return !status && currentSent >= maxNumber;
    }

    public AliveProbe(int maxNumber, boolean status) {
        this.currentSent = 0;
        this.maxNumber = maxNumber;
        this.status = status;
        System.out.println("\nSTARTING AS ACTIVE: " + status);
    }

}
