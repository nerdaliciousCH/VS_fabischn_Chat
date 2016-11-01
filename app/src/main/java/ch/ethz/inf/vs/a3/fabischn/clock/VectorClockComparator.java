package ch.ethz.inf.vs.a3.fabischn.clock;

import java.util.Comparator;

public class VectorClockComparator implements Comparator<VectorClock> {

    @Override
    public int compare(VectorClock lhs, VectorClock rhs) {
        boolean lhbr = lhs.happenedBefore(rhs);
        boolean rhbl = rhs.happenedBefore(lhs);
        if (lhbr == rhbl){
            return 0;
        } else if(lhbr){
            return -1;
        } else{
            return 1;
        }
    }
}
