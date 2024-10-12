package code;

import java.util.concurrent.atomic.AtomicIntegerArray;

public class VectorClock extends AtomicIntegerArray {

    // AtomicIntegerArray clock;

    public VectorClock(int numberOfProcesses) {
        // default 0s
        // clock = new AtomicIntegerArray(numberOfProcesses);
        super(numberOfProcesses);
    }

    public VectorClock(int[] arr) {
        // static testing
        super(arr);
    }

    public static VectorClock copy(VectorClock vc) {
        // deep copy
        VectorClock clockCopy = new VectorClock(vc.length());
        for(int i=0; i<vc.length(); i++) {
            clockCopy.set(i, vc.get(i));
        }
        return clockCopy;
    }

    public boolean concurrent(VectorClock v2) {
        return !this.lessThan(v2) && !v2.lessThan(this);
    }

    public boolean lessThan(VectorClock v2) {
        assert this.length() == v2.length();
        for (int i = 0; i < this.length(); i++) {
            // for all i; v1[i] <= v2[i]
            if (this.get(i) > v2.get(i)) {
                return false;
            }
        }
        for (int j = 0; j < this.length(); j++) {
            // for some j; v1[j] < v2[j]
            if (this.get(j) < v2.get(j)) {
                return true;
            }
        }
        // at this point v1==v2
        return false;
    }

    public void mergeMessageClockAndIncrement(VectorClock messageClock, int nodeId) {
        assert this.length() == messageClock.length();
        for (int i = 0; i < this.length(); i++) {
            if(this.get(i) < messageClock.get(i)) {
                this.set(i, messageClock.get(i));
            }
        }
        this.getAndIncrement(nodeId);
    }

    public void print() {
        System.out.println(this.toString());
    }

    public void print(String prefix) {
        System.out.println(prefix + " " + this.toString());
    }

    public static void test(VectorClock v1, VectorClock v2) {
        v1.print("v1");
        v2.print("v2");
        System.out.println("v1<v2: " + v1.lessThan(v2));
        System.out.println("v2<v1: " + v2.lessThan(v1));
        System.out.println("v1||v2: " + v1.concurrent(v2));
    }

    public static void main(String[] args) {
        int n = 7;
        VectorClock v1, v2;

        System.out.println("\nCASE 0");
        v1 = new VectorClock(n);
        v2 = new VectorClock(n);
        test(v1, v2);

        System.out.println("\nCASE 1");
        v1 = new VectorClock(new int[] { 1, 2, 3, 4, 5, 6, 7 });
        v2 = new VectorClock(new int[] { 0, 1, 1, 2, 2, 2, 1 });
        test(v1, v2);

        System.out.println("\nCASE 2");
        v1 = new VectorClock(new int[] { 0, 1, 1, 2, 2, 2, 1 });
        v2 = new VectorClock(new int[] { 7, 6, 5, 4, 3, 2, 1 });
        test(v1, v2);

        System.out.println("\nCASE 3");
        v1 = new VectorClock(new int[] { 1, 2, 3, 4, 5, 6, 7 });
        v2 = new VectorClock(new int[] { 7, 6, 5, 4, 3, 2, 1 });
        test(v1, v2);

        v1.print("before: ");
        v1.incrementAndGet(3);
        v1.print("after:  ");
    }
}
