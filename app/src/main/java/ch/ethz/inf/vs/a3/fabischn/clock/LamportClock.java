package ch.ethz.inf.vs.a3.fabischn.clock;

/**
 * Created by Olivier on 27.10.2016.
 */

public class LamportClock implements Clock {

    private int time;

    @Override
    public void update(Clock other) {
        this.time = Math.max(((LamportClock) other).getTime(), this.time);
    }

    @Override
    public void setClock(Clock other) {
        this.time = ((LamportClock) other).getTime();
    }

    @Override
    public void tick(Integer pid) {
        this.time++;
    }

    @Override
    public boolean happenedBefore(Clock other) {
        return this.time < ((LamportClock) other).getTime();
    }

    @Override
    public void setClockFromString(String clock) {
        try {
            this.time = Integer.parseInt(clock);
        }
        catch (NumberFormatException ne) {
            return;
        }


    }


    public void setTime(int time) {
        this.time = time;
    }

    public int getTime() {
        return this.time;
    }

    @Override
    public String toString() {
        return this.time + "";
    }
}
