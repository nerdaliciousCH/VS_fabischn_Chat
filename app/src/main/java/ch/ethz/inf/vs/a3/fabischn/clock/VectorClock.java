package ch.ethz.inf.vs.a3.fabischn.clock;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


/**
 * Created by Olivier on 27.10.2016.
 */

public class VectorClock implements Clock {


    private Map<Integer,Integer> vector = new HashMap<>(0);

    public Map<Integer, Integer> getVector() {
        return vector;
    }

    @Override
    public void update(Clock other) {
        Map<Integer,Integer> otherVector = ((VectorClock) other).getVector();
        for (int pid : otherVector.keySet()) {
            Integer clockVal = this.vector.get(pid);
            if (clockVal == null) {
                this.vector.put(pid, otherVector.get(pid));
            }
            else {
                this.vector.put(pid, Math.max(clockVal, otherVector.get(pid)));
            }

        }
        
    }

    @Override
    public void setClock(Clock other) {
        Map<Integer,Integer> otherVector = ((VectorClock) other).getVector();
        this.vector = otherVector;
    }

    @Override
    public void tick(Integer pid) {
        this.vector.put(pid, this.vector.get(pid) + 1);
    }

    @Override
    public boolean happenedBefore(Clock other) {
        Map<Integer,Integer> otherVector = ((VectorClock) other).getVector();
        boolean result = true;
        boolean equal = true;
        Set<Integer> s1 = this.vector.keySet();
        Set<Integer> s2 = ((VectorClock) other).getVector().keySet();
        /*
        In case that this vector contains only a subset of pids of the other processes vector
        we only have to compare the common pids. The pids that are exclusively in 'other' must
        have a non-zero time (see method 'addProcess()') and thus must be bigger than the implicit
        (because non-listed) pid in 'this' clock.
         */
        if (s2.containsAll(s1)) {
            for (int pid : s1) {
                result &= this.vector.get(pid) <= otherVector.get(pid);
            }
            /*
            If s1 is a proper subset of s2 it suffices to check the '<=' condition,
            since there exists an element in s2 that is not in s1 and therefore has a
            strictly bigger timestamp.
             */
            if (s1.size() < s2.size()) {
                return result;
            }
            /*
            If the keysets are indeed equal, we have to check whether all timestamps are equal.
            If that's the case we have to return false, because according to 'Definition 3.5' of
            Landes' paper the 'happened-before' relation is not reflexive.
             */
            else {
                for (int pid : s1) {
                    equal &= this.vector.get(pid) == otherVector.get(pid);
                }
                return result && !equal;
            }

        }
        /*
        Otherwise there must be a pid in 'this' that is not in 'other' and therefore this pids timestamp
        is bigger than the implicit-0-timestamp in 'other'. Therefore 'this' will never happen before
        'other' in that case.
         */
        else {
            return false;
        }

    }

    private static <T> Set<T> intersection(Set<T> setA, Set<T> setB) {
        Set<T> tmp = new TreeSet<T>();
        for (T x : setA)
            if (setB.contains(x))
                tmp.add(x);
        return tmp;
    }

    @Override
    public void setClockFromString(String clock) {
        // Check whether input-string is enclosed by curly brackets
        if (clock.matches("\\{.*\\}")) {
            // remove "{" and "}"
            String data = clock.substring(1, clock.length() - 1);
            // If there are no key-value pairs in the string, make the vector empty.
            if (data.isEmpty())
                this.vector = new HashMap<>();

            //Otherwise the String must have the following pattern:
            //(the second capturing grout is only to handle the special case, that
            // the last element isn't comma-separated.

            else if  (data.matches("(\"(\\d)+\":(\\d)+,)*(\"(\\d)+\":(\\d)+)")) {
                String[] kvStr = data.split(",");
                VectorClock newClock = new VectorClock();
                for (String kv : kvStr) {
                    String[] kvSep = kv.split(":");
                    String key = kvSep[0].substring(1, 2);
                    Integer pid = Integer.parseInt(key);
                    Integer clk = Integer.parseInt(kvSep[1]);
                    newClock.getVector().put(pid, clk);
                }
                setClock(newClock);
            }


        }

    }

    @Override
    public String toString() {
        String result = "{";
        for (int pid : this.vector.keySet()) {
            result += "\"" + pid + "\"" + ":" + this.vector.get(pid) + ",";
        }
        if (this.vector.size() > 0)
            result = result.substring(0, result.length() - 1);
        return  result + "}";
    }

    public void addProcess(int i, int testTime) {
            this.vector.put(i,testTime);
    }

    public int getTime(Integer pid) {
        return this.vector.get(pid);
    }
}
