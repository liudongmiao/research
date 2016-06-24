package me.piebridge.research;

/**
 * Created by Liu DongMiao &lt;liudongmiao@gmail.com&gt; on 2016/06/24.
 *
 * @author thom
 */
public class Boxing {

    Integer box(int i) {
        return i;
    }

    int unbox(Integer i) {
        return i;
    }

    boolean equals(Integer i, int j) {
        return i == j;
    }

    void checkNan() {
        Object o = 42;
        double d = Double.NaN;
        Double da = Double.NaN;
        Double db = Double.NaN;
        boolean ba = d == d;
        boolean bb = da == da; // true, idea says "false"
        boolean bc = da == db;
        boolean bd = d == da;
    }

    Integer sum(Integer max) {
        if (max == null) {
            return null;
        }
        Integer s = 0;
        for (Integer i = 0; i <= max; ++i) {
            s += i;
        }
        return s;
    }

}
