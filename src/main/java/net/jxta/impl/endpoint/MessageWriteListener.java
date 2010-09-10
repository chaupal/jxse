package net.jxta.impl.endpoint;

/**
 * Simple callback interface to be asynchronously notified whether a message
 * has been successfully sent via the messenger or not. Note that this does
 * not mean that the message has necessarily been successfully received by
 * the intended recipient, simply that it was sent without error on the first
 * hop.
 * 
 * @author Iain McGinniss (iain.mcginniss@onedrum.com)
 */
public interface MessageWriteListener {

    public void writeSuccess();
    public void writeFailure(Throwable cause);
}
