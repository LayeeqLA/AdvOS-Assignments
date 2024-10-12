package code;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class LocalState {

    // FALSE -> PASSIVE
    // TRUE -> ACTIVE
    private AtomicBoolean status;
    private AtomicBoolean snapshotActive;
    private AtomicBoolean systemTerminated;
    private int maxNumber; // of messages that can be sent
    private int currentSent;
    private VectorClock clock;
    private Set<Integer> markersReceived;
    private StateRecord stateRecord;
    private CountDownLatch terminationLatch;
    private Map<Integer, Integer> channelState;
    private Map<Integer, List<StateRecord>> childRecords;

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

    public synchronized boolean isMapTerminated() {
        // PASSIVE AND MAX NUMBER OF MESSAGES SENT
        return !status.get() && currentSent >= maxNumber;
        // TODO: CHANGE
    }

    public synchronized boolean isSystemTerminated() {
        return systemTerminated.get();
    }

    public boolean isSnapshotActive() {
        return snapshotActive.get();
    }

    public void setSnapshotActive(int currentPid, int markerPid, int[] neighborIds) {
        this.snapshotActive.set(true);
        this.markersReceived.add(markerPid);
        this.stateRecord = new StateRecord(currentPid, clock, false);
        for (int neighborId : neighborIds) {
            if (neighborId != markerPid) {
                this.channelState.put(neighborId, 0);
            }
        }
        // TODO: Write to file
    }

    public void setSnapshotInactive() {
        this.snapshotActive.set(false);
        this.stateRecord.setAreAllChannelsEmpty(channelState.values().stream().reduce(Integer::sum).get() == 0);
    }

    public void addChannelMessage(int senderId) {
        if (snapshotActive.get() && channelState.keySet().contains(senderId)) {
            channelState.merge(senderId, 1, Integer::sum);
        }
    }

    public void clearSnapshotData() {
        this.markersReceived = new HashSet<>();
        this.childRecords = new HashMap<>();
        this.channelState = new HashMap<>();
        this.stateRecord = null;
    }

    public VectorClock getClock() {
        return clock;
    }

    // returns how many markers are received
    public synchronized int addMarkerReceivedAndGet(int pid) {
        markersReceived.add(pid);
        return markersReceived.size();
    }

    public synchronized int getMarkerCount() {
        return markersReceived.size();
    }

    // return how many children have responded
    public synchronized int addChildRecordAndGet(int pid, List<StateRecord> childRecord) {
        childRecords.put(pid, childRecord);
        return childRecords.size();
    }

    public synchronized int getChildRecordsLength() {
        return childRecords.size();
    }

    public Map<Integer, List<StateRecord>> getChildRecords() {
        return childRecords;
    }

    public synchronized void setStateRecord(StateRecord sr) {
        this.stateRecord = sr;
    }

    public StateRecord getStateRecord() {
        return stateRecord;
    }

    public CountDownLatch getTerminationLatch() {
        return terminationLatch;
    }

    public LocalState(int maxNumber, boolean status, int nodeCount) {
        this.currentSent = 0;
        this.maxNumber = maxNumber;
        this.terminationLatch = new CountDownLatch(1);
        this.systemTerminated = new AtomicBoolean(false);
        this.snapshotActive = new AtomicBoolean(false);
        this.status = new AtomicBoolean(status);
        this.clock = new VectorClock(nodeCount);
        this.markersReceived = new HashSet<>();
        this.childRecords = new HashMap<>();
        this.channelState = new HashMap<>();
        this.stateRecord = null;
        System.out.println("\nSTARTING AS ACTIVE: " + status);
    }

}
