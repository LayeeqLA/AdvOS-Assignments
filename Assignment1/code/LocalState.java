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
    private AtomicBoolean systemActive; // MAP
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

    public synchronized boolean isSystemActive() {
        return systemActive.get();
    }

    public synchronized void setSystemActive() {
        if (currentSent < maxNumber) {
            systemActive.set(true);
        }
    }

    public synchronized void setSystemPassive() {
        systemActive.set(false);
    }

    public synchronized void incrementMessageCount() {
        currentSent++;
    }

    public synchronized int getMessageCount() {
        return currentSent;
    }

    public synchronized boolean isMapTerminated() {
        // PASSIVE AND MAX NUMBER OF MESSAGES SENT
        return !systemActive.get() && currentSent >= maxNumber;
    }

    public synchronized boolean isSystemTerminated() {
        return systemTerminated.get();
    }

    public synchronized void terminateSystem() {
        systemTerminated.set(true);
        terminationLatch.countDown();
    }

    public boolean isSnapshotActive() {
        return snapshotActive.get();
    }

    public void setSnapshotActive(int currentPid, Integer markerPid, int[] neighborIds) {
        assert !snapshotActive.get();
        this.snapshotActive.set(true);
        if (markerPid != null) {
            this.markersReceived.add(markerPid);
        }
        this.stateRecord = new StateRecord(currentPid, systemActive.get());
        for (int neighborId : neighborIds) {
            if (markerPid == null || neighborId != markerPid) {
                this.channelState.put(neighborId, 0);
            }
        }
    }

    public void setSnapshotInactive() {
        assert snapshotActive.get();
        this.snapshotActive.set(false);
        this.stateRecord.setAreAllChannelsEmpty(channelState.values().stream().reduce(Integer::sum).get() == 0);
    }

    public synchronized void addChannelAppMessage(int senderId) {
        if (snapshotActive.get() && channelState.keySet().contains(senderId)
                && !markersReceived.contains(senderId)) {
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

    public synchronized void addMarkerReceived(int pid) {
        markersReceived.add(pid);
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
        this.systemActive = new AtomicBoolean(status);
        this.clock = new VectorClock(nodeCount);
        this.markersReceived = new HashSet<>();
        this.childRecords = new HashMap<>();
        this.channelState = new HashMap<>();
        this.stateRecord = null;
        System.out.println("\nSTARTING AS ACTIVE: " + status);
    }

}
