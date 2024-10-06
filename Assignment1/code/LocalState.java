package code;

import java.util.concurrent.atomic.AtomicBoolean;

public class LocalState {

    // FALSE -> PASSIVE
    // TRUE -> ACTIVE
    private AtomicBoolean status;
    private AtomicBoolean snapshotActive;
    private int maxNumber; // of messages that can be sent
    private int currentSent;
    private VectorClock clock;

    public synchronized boolean isAlive() {
        return status.get();
    }

    public synchronized void setActive() {
        if (currentSent < maxNumber) {
            status.set(true);
        }
    }

    public synchronized void setPassive() {
        status.set(false);
    }

    public synchronized void incrementMessageCount() {
        currentSent++;
    }

    public synchronized int getMessageCount() {
        return currentSent;
    }

    public synchronized boolean isTerminated() {
        // PASSIVE AND MAX NUMBER OF MESSAGES SENT
        return !status.get() && currentSent >= maxNumber;
    }

    public boolean isSnapshotActive() {
        return snapshotActive.get();
    }

    // public void setSnapshotActive(boolean snapshotActive) {
    // this.snapshotActive = snapshotActive;
    // }

    public VectorClock getClock() {
        return clock;
    }

    public LocalState(int maxNumber, boolean status, int nodeCount) {
        this.currentSent = 0;
        this.maxNumber = maxNumber;
        this.status = new AtomicBoolean(status);
        this.clock = new VectorClock(nodeCount);
        System.out.println("\nSTARTING AS ACTIVE: " + status);
    }

}
