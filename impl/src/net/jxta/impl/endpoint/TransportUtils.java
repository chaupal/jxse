package net.jxta.impl.endpoint;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.Messenger;
import net.jxta.endpoint.OutgoingMessageEvent;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.logging.Logging;

/**
 * Provides some simple utility methods that are useful for transport implementations, as a complement
 * to those provided by IPUtils.
 * 
 * @author Iain McGinniss (iain.mcginniss@onedrum.com)
 */
public class TransportUtils {

    private static final Logger LOG = Logger.getLogger(TransportUtils.class.getName());

    /**
     * Will attempt to resolve the physical adapter's InetAddress for the given interface
     * address string (typically, a host name or string representation of an IP address).
     * If this cannot be resolved, {@link net.jxta.impl.endpoint.IPUtils#ANYADDRESS IPUtils.ANYADDRESS}
     * will be returned.
     */
    public static InetAddress resolveInterfaceAddress(String interfaceAddressStr) {
        InetAddress interfaceAddress;
        if (interfaceAddressStr != null) {
            try {
                interfaceAddress = InetAddress.getByName(interfaceAddressStr);
            } catch (UnknownHostException failed) {
                if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                    LOG.log(Level.WARNING, "Invalid address for local interface address, using {0} instead", IPUtils.ANYADDRESS);
                }
                interfaceAddress = IPUtils.ANYADDRESS;
            }
        } else {
            interfaceAddress = IPUtils.ANYADDRESS;
        }
        
        return interfaceAddress;
    }
    
    /**
     * Takes an integer port range and creates a list containing all the integers in
     * that range, randomly ordered. If the start or end values are outside the
     * legal range for a TCP or UDP port (1-65535) then an IllegalArgumentException
     * will be thrown.
     */
    public static List<Integer> rangeCheckShuffle(int start, int end) throws IllegalArgumentException {
        if ((start < 1) || (start > 65535)) {
            throw new IllegalArgumentException("Invalid start port");
        }

        if ((end < 1) || (end > 65535) || (end < start)) {
            throw new IllegalArgumentException("Invalid end port");
        }

        // fill the inRange array.
        List<Integer> inRange = new ArrayList<Integer>();

        for (int eachInRange = start; eachInRange < end; eachInRange++) {
            inRange.add(eachInRange);
        }
        Collections.shuffle(inRange);
        return inRange;
    }
    
    public static Message retargetMessage(Message message, String service, String param, EndpointAddress localAddress, EndpointAddress defaultDestAddress) {
        MessageElement srcAddrElem = new StringMessageElement(EndpointServiceImpl.MESSAGE_SOURCE_NAME, localAddress.toString(), null);
        message.replaceMessageElement(EndpointServiceImpl.MESSAGE_SOURCE_NS, srcAddrElem);
        EndpointAddress destAddressToUse;
        destAddressToUse = getDestAddressToUse(service, param, defaultDestAddress);

        MessageElement dstAddressElement = new StringMessageElement(EndpointServiceImpl.MESSAGE_DESTINATION_NAME, destAddressToUse.toString(), null);
        message.replaceMessageElement(EndpointServiceImpl.MESSAGE_DESTINATION_NS, dstAddressElement);
        
        return message;
    }
    
    /**
     * Assemble a destination address for a message based upon the messenger
     * default destination address and the optional provided overrides.
     *
     * @param service The destination service or {@code null} to use default.
     * @param serviceParam The destination service parameter or {@code null} to 
     * use default.
     */
    public static EndpointAddress getDestAddressToUse(final String service, final String serviceParam, final EndpointAddress defaultAddress) {
        EndpointAddress result;
        
        if(null == service) {
            if(null == serviceParam) {
                // Use default service name and service params
                result = defaultAddress;
            } else {
                // use default service name, override service params
                result = new EndpointAddress(defaultAddress, defaultAddress.getServiceName(), serviceParam);
            }
        } else {
            if(null == serviceParam) {
                // override service name, use default service params (this one is pretty weird and probably not useful)
                result = new EndpointAddress(defaultAddress, service, defaultAddress.getServiceParameter());
            } else {
                // override service name, override service params
                result = new EndpointAddress(defaultAddress, service, serviceParam);
            }
        }
        
        return result;
    }
    
    /**
     * Sets a message property on the provided message indicating that it could not be sent
     * due to messenger saturation.
     */
    public static void markMessageWithOverflowFailure(Message message) {
        message.setMessageProperty(Messenger.class, OutgoingMessageEvent.OVERFLOW);
    }
    
    /**
     * Sets a message property on the provided message indicating that it could not be sent
     * due to the provided cause.
     */
    public static void markMessageWithSendFailure(Message message, Throwable cause) {
        message.setMessageProperty(Messenger.class, new OutgoingMessageEvent(message, cause));
    }
    
    /**
     * Sets a message property on the provided message indicating that it was successfully
     * sent. This does not, however, mean that the message has yet been successfully received
     * by the intended recipient.
     */
    public static void markMessageWithSendSuccess(Message message) {
        message.setMessageProperty(Messenger.class, OutgoingMessageEvent.SUCCESS);
    }

    /**
     * @return whether or not the provided message has failed to send for whatever
     * reason (overflow or some other cause).
     */
    public static boolean isMarkedWithFailure(Message msg) {
        Object property = msg.getMessageProperty(Messenger.class);
        if(property instanceof OutgoingMessageEvent) {
            OutgoingMessageEvent event = (OutgoingMessageEvent) property;
            return event == OutgoingMessageEvent.OVERFLOW || event.getFailure() != null;
        }
        
        return false;
    }

    /**
     * @return the cause of the failure in sending this message. Note that if the message
     * failed due to messenger saturation, this method will return null.
     */
    public static Throwable getFailureCause(Message msg) {
        Object property = msg.getMessageProperty(Messenger.class);
        if(property instanceof OutgoingMessageEvent) {
            OutgoingMessageEvent event = (OutgoingMessageEvent) property;
            return event.getFailure();
        }
        
        return null;
    }

    /**
     * @return whether or not this message has been marked, stating that it has been
     * successfully sent.
     */
    public static boolean isMarkedWithSuccess(Message msg) {
        return msg.getMessageProperty(Messenger.class) == OutgoingMessageEvent.SUCCESS;
    }
    
}
