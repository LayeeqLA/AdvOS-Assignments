package code;

import java.io.Serializable;

public class StateRecord implements Serializable {
    private int pid;
    private VectorClock clock;
    private boolean areAllChannelsEmpty;

    public StateRecord(int pid, VectorClock clock, boolean areAllChannelsEmpty) {
        this.pid = pid;
        this.clock = VectorClock.copy(clock);
        this.areAllChannelsEmpty = areAllChannelsEmpty;
    }

    public VectorClock getClock() {
        return clock;
    }

    public boolean areAllChannelsEmpty() {
        return areAllChannelsEmpty;
    }

    public void setAreAllChannelsEmpty(boolean areAllChannelsEmpty) {
        this.areAllChannelsEmpty = areAllChannelsEmpty;
    }

    @Override
    public String toString() {
        return "StateRecord [pid=" + pid + ", clock=" + clock.toString() + ", areAllChannelsEmpty=" + areAllChannelsEmpty + "]";
    }

}
