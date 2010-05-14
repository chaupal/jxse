package net.jxta.impl.endpoint;

import net.jxta.endpoint.Message;

/**
 * Simple data structure to associate a message with it's write listener,
 * used by asynchronous messengers.
 * @author Iain McGinniss (iain.mcginniss@onedrum.com)
 */
class QueuedMessage {

    private Message message;
    private MessageWriteListener writeListener;
    
    public QueuedMessage(Message msg, MessageWriteListener writeListener) {
        this.message = msg;
        this.writeListener = writeListener;
    }
    
    public Message getMessage() {
        return message;
    }
    
    public MessageWriteListener getWriteListener() {
        return writeListener;
    }
    
}
