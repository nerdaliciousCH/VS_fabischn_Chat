package ch.ethz.inf.vs.a3.fabischn.message;

import java.util.Comparator;

import ch.ethz.inf.vs.a3.fabischn.clock.VectorClock;
import ch.ethz.inf.vs.a3.fabischn.clock.VectorClockComparator;

/**
 * Message comparator class. Use with PriorityQueue.
 */
public class MessageComparator implements Comparator<Message> {

    @Override
    public int compare(Message lhs, Message rhs) {
        VectorClock lClock = new VectorClock();
        VectorClock rClock = new VectorClock();
        lClock.setClockFromString(lhs.getTimestamp());
        rClock.setClockFromString(rhs.getTimestamp());
        VectorClockComparator comp = new VectorClockComparator();
        return comp.compare(lClock,rClock);
    }

}
