package net.jxta.util;

import java.net.SocketException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jxta.logging.Logging;

/**
 * A server pipe listener which stores a queue of incoming connections, to be
 * synchronously pulled off the queue at a pace determined by the client. If more
 * connections are received than the specified backlog, new connections beyond that
 * point will be discarded until connections are accepted from the head of the queue
 */
public class QueuingServerPipeAcceptor implements ServerPipeAcceptListener {

    private BlockingQueue<JxtaBiDiPipe> pendingAcceptance;
    private long defaultTimeout;
    private static final Logger LOG = Logger.getLogger(QueuingServerPipeAcceptor.class.getName());

    /**
     * @param backlog the maximum number of connections to queue for acceptance.
     * @param defaultTimeout the default timeout of the {@link #accept()} method, in
     * milliseconds. If this value is 0, then the timeout will be effectively indefinite.
     * If the value is less than zero, then no timeout will be applied, i.e. accept will
     * return immediately).
     */
    public QueuingServerPipeAcceptor(int backlog, long defaultTimeout) {
        pendingAcceptance = new ArrayBlockingQueue<JxtaBiDiPipe>(backlog);
        setTimeout(defaultTimeout);
    }

    public void pipeAccepted(JxtaBiDiPipe pipe) {
        if(!pendingAcceptance.offer(pipe) && Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
            LOG.log(Level.WARNING, "pending queue full, discarding incoming pipe {0} from {1}", 
                    new Object[] { pipe.getPipeAdvertisement().getPipeID(),
                                   pipe.getRemotePeerAdvertisement().getPeerID()
                                 });
        }
    }

    /**
     * Accept a queued incoming pipe, or wait up to the specified timeout for a pipe
     * to be established. timeout <= 0 is equivalent to saying "do not wait".
     * 
     * @return the accepted pipe, or null if no new pipe arrived before the timeout.
     */
    public JxtaBiDiPipe accept(long timeout, TimeUnit timeoutUnit) throws InterruptedException {
        return pendingAcceptance.poll(timeout, TimeUnit.MILLISECONDS);
    }

    /**
     * @return the accepted pipe, or null if no new pipe arrived before the timeout.
     */
    public JxtaBiDiPipe accept() throws InterruptedException {
        return accept(defaultTimeout, TimeUnit.MILLISECONDS);
    }

    /**
     * Variant of accept which throws a SocketException if no pipe is received within the timeout.
     * This exists to be compatible with the original interface of {@link JxtaServerPipe#accept()}.
     */
    public JxtaBiDiPipe acceptBackwardsCompatible() throws SocketException {
        try {
            JxtaBiDiPipe pipe = accept();
            if(pipe == null) {
                throw new SocketException("No pipe received within timeout");
            }

            return pipe;
        } catch (InterruptedException e) {
            SocketException s = new SocketException("Interrupted while waiting for new incoming pipe");
            s.initCause(e);
            throw s;
        }
    }

    public void serverPipeClosed() {
        pendingAcceptance.clear();
    }

    /**
     * Sets the default timeout value (in milliseconds) that is used for the
     * {@link #accept()} method.
     * 
     * @param timeout
     */
    public void setTimeout(long timeout) {
        if(timeout == 0) {
            this.defaultTimeout = Long.MAX_VALUE;
        }

        this.defaultTimeout = timeout;
    }

    public void setTimeoutBackwardsCompatible(int timeout) {
    	if (timeout < 0) {
            throw new IllegalArgumentException("Negative timeout values are not allowed.");
    	} else if(timeout == 0) {
            this.defaultTimeout = Long.MAX_VALUE;
        } else {
        	this.defaultTimeout = timeout;
        }
    }

    /**
     * Returns the default timeout value (in milliseconds) that is used for the
     * {@link #accept()} method.
     */
    public long getTimeout() {
        return defaultTimeout;
    }

    /**
     * Variant of getTimeout which matches the original behaviour of {@link JxtaServerPipe#getPipeTimeout()}.
     * If the timeout value is greater than {@link Integer#MAX_VALUE} then 0 is returned, indicating an
     * infinite timeout.
     */
    public int getTimeoutBackwardsCompatible() {
       if(defaultTimeout > Integer.MAX_VALUE) {
           return 0;
       }

       return (int)defaultTimeout;
    }
}
