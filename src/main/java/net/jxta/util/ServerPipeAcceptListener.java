package net.jxta.util;

public interface ServerPipeAcceptListener {

    /**
     * Intended as a signal to clean up the listener, if necessary.
     */
    public void serverPipeClosed();

    /**
     * A new incoming pipe has been established, and should be processed by the listener.
     * <em>NOTE</em>: Processing of the pipe should be fast and non-blocking. If you
     * intend to do a lot of complex processing or blocking behaviour in the handling
     * of a new pipe, please delegate the handling to another thread.
     * 
     * @param pipe the new pipe
     */
    public void pipeAccepted(JxtaBiDiPipe pipe);
}
