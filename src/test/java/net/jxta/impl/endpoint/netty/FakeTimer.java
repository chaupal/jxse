package net.jxta.impl.endpoint.netty;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.Timer;
import org.jboss.netty.util.TimerTask;

public class FakeTimer implements Timer {

    private List<FakeTimeout> timeouts = new LinkedList<FakeTimeout>();

    public Timeout newTimeout(TimerTask task, long delay, TimeUnit unit) {
        FakeTimeout t = new FakeTimeout(task, delay, unit);
        timeouts.add(t);
        return t;
    }

    public void expireAll() throws Exception {
        List<FakeTimeout> currentTimeouts = new LinkedList<FakeTimeout>(timeouts);
        for(FakeTimeout t : currentTimeouts) {
            t.expire();
        }

        timeouts.remove(currentTimeouts);
    }

    public Set<Timeout> stop() {
        return null;
    }

    private class FakeTimeout implements Timeout {

        private TimerTask task;
        private long delay;
        private TimeUnit unit;
        private boolean cancelled;
        private boolean expired;

        public FakeTimeout(TimerTask task, long delay, TimeUnit unit) {
            this.task = task;
            this.delay = delay;
            this.unit = unit;
            this.cancelled = false;
            this.expired = false;
        }

        public void cancel() {
            timeouts.remove(this);
            cancelled = true;
        }

        public TimerTask getTask() {
            return task;
        }

        public Timer getTimer() {
            return FakeTimer.this;
        }

        public boolean isCancelled() {
            return cancelled;
        }

        public boolean isExpired() {
            return expired;
        }

        public void expire() throws Exception {
            this.expired = true;
            task.run(this);
        }

    }

    public int numRegisteredTimeouts() {
        return timeouts.size();
    }

}
