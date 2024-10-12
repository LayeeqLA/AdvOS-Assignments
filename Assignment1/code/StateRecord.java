package code;

import java.io.Serializable;

public class StateRecord implements Serializable {
    private int pid;
    private boolean isNodeMapActive;
    private boolean areAllChannelsEmpty;

    public StateRecord(int pid, boolean isNodeMapActive) {
        this.pid = pid;
        this.isNodeMapActive = isNodeMapActive;
    }

    public boolean isNodeMapActive() {
        return isNodeMapActive;
    }

    public void setIsNodeActive(boolean isNodeMapActive) {
        this.isNodeMapActive = isNodeMapActive;
    }

    public boolean areAllChannelsEmpty() {
        return areAllChannelsEmpty;
    }

    public void setAreAllChannelsEmpty(boolean areAllChannelsEmpty) {
        this.areAllChannelsEmpty = areAllChannelsEmpty;
    }

    @Override
    public String toString() {
        return "StateRecord [pid=" + pid + ", isNodeMapActive=" + isNodeMapActive
                + ", areAllChannelsEmpty=" + areAllChannelsEmpty + "] ";
    }

}
