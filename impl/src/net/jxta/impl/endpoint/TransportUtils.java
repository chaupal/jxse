
package net.jxta.impl.endpoint;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import net.jxta.logging.Logging;

/**
 * Provides some simple utility methods that are useful for transport implementations, as a complement
 * to those provided by IPUtils.
 * 
 * @author Iain McGinniss (iain.mcginniss@onedrum.com)
 */
public final class TransportUtils {

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

                Logging.logCheckedWarning(LOG, "Invalid address for local interface address, using ", IPUtils.ANYADDRESS, " instead" );
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

    /**
     * Default constructor
     */
    private TransportUtils() {
    }
    
}
