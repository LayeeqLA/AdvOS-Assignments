package fileops;

import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class test {
    public static void main(String[] args) throws IOException {
        int[] a1 = { 1, 2, 3, 4, 5, 6, 7 };
        int[] a2 = { 11, 2, 2, 2, 2, 2, 3 };
        AtomicIntegerArray ar1 = new AtomicIntegerArray(a1);
        AtomicIntegerArray ar2 = new AtomicIntegerArray(a2);
        String ars1 = toFileString(ar1);
        String ars2 = toFileString(ar2);

        FileWriter fw = new FileWriter("file" + 1 + ".out");
        fw.write(ars1 + System.lineSeparator());
        fw.flush();
        fw.write(ars2 + System.lineSeparator());
        fw.flush();
        fw.close();
    }

    public static String toFileString(AtomicIntegerArray vc) {
        int iMax = vc.length() - 1;
        if (iMax == -1)
            return "";

        StringBuilder b = new StringBuilder();
        for (int i = 0;; i++) {
            b.append(vc.get(i));
            if (i == iMax)
                return b.toString();
            b.append(' ');
        }
    }
}