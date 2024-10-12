package code;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConsistencyChecker {
    // private Map<Integer, StateRecord> localStates = new ConcurrentHashMap<>();

    // public boolean isConsistent() {
    //     for (int i = 0; i < localStates.size(); i++) {
    //         for (int j = localStates.size() - 1; j > i; j--) {
    //             if (!localStates.get(i).getClock().concurrent(localStates.get(j).getClock())) {
    //                 System.out.println("Not Consistent at " + i + " and " + j);
    //                 return false;
    //             }
    //         }
    //     }
    //     return true;
    // }

    // public void addStateEntry(Integer pid, StateRecord state) {
    //     localStates.put(pid, state);
    // }
}
