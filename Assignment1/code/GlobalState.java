package code;

import java.util.List;

public class GlobalState {
    private List<VectorClock> localStates;

    public boolean isConsistent() {
        for (int i = 0; i < localStates.size(); i++) {
            for (int j = localStates.size() - 1; j > i; j--) {
                if (!localStates.get(i).concurrent(localStates.get(j))) {
                    System.out.println("Not Consistent at " + i + " and " + j);
                    return false;
                }
            }
        }
        return true;
    }
}
