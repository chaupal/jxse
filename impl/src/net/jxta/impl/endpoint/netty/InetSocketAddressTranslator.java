package net.jxta.impl.endpoint.netty;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import net.jxta.endpoint.EndpointAddress;
import net.jxta.impl.endpoint.IPUtils;

/**
 * Socket address translator for traditional java InetSocketAddress instances, both IPv6 and IPv4.
 * 
 * @author iain.mcginniss@onedrum.com
 */
public class InetSocketAddressTranslator implements AddressTranslator {

    private String protocol;

    public InetSocketAddressTranslator(String protocol) {
        this.protocol = protocol;
    }
    
    public EndpointAddress toEndpointAddress(SocketAddress addr) {
        InetSocketAddress socketAddr = (InetSocketAddress)addr;
        String hostAddr;
        if(socketAddr.isUnresolved()) {
            hostAddr = socketAddr.getHostName();
        } else {
            hostAddr = IPUtils.getHostAddress(socketAddr.getAddress());
        }
        return new EndpointAddress(protocol, hostAddr + ":" + socketAddr.getPort(), null, null);
    }
    
    public EndpointAddress toEndpointAddress(SocketAddress clientAddr, SocketAddress serverAddr) {
        InetSocketAddress serverInetAddr = (InetSocketAddress)serverAddr;
        InetSocketAddress clientInetAddr = (InetSocketAddress)clientAddr;
        
        return toEndpointAddress(new InetSocketAddress(clientInetAddr.getAddress(), serverInetAddr.getPort()));
    }

    public SocketAddress toSocketAddress(EndpointAddress addr) {
        String protoAddr = addr.getProtocolAddress();
        if(protoAddr.indexOf('[') != -1 || protoAddr.indexOf(']') != -1) {
            return parseIPv6(protoAddr);
        } else {
            return parseIPv4(protoAddr);
        }
    }
    
    private InetSocketAddress parseIPv6(String protoAddr) {
        int indexOfCloseBracket = protoAddr.indexOf(']');
        if(protoAddr.charAt(0) != '[' || indexOfCloseBracket == -1) {
            throw new IllegalArgumentException("Address is not a valid IPv6 address <" + protoAddr + ">");
        }
        
        String hostPart = protoAddr.substring(1, indexOfCloseBracket);
        if(indexOfCloseBracket == protoAddr.length()-1 || indexOfCloseBracket == protoAddr.length()-2) {
            throw new IllegalArgumentException("No port specified in IPv6 address <" + protoAddr + ">");
        }
        
        if(protoAddr.charAt(indexOfCloseBracket+1) != ':') {
            throw new IllegalArgumentException("No port specified in IPv6 address <" + protoAddr + ">");
        }
        
        String portPart = protoAddr.substring(indexOfCloseBracket+2);
        int port = parsePort(portPart, protoAddr);
        
        return new InetSocketAddress(hostPart, port);
    }
    
    private InetSocketAddress parseIPv4(String protoAddr) {
        int indexOfColon = protoAddr.indexOf(":");
        if(indexOfColon == -1 || indexOfColon == protoAddr.length()-1) {
            throw new IllegalArgumentException("No port specified in address <" + protoAddr + ">");
        }
        
        if(indexOfColon == 0) {
            throw new IllegalArgumentException("No host specified in address <" + protoAddr + ">");
        }
        
        String hostName = protoAddr.substring(0, indexOfColon);
        String portStr = protoAddr.substring(indexOfColon+1);
        int port = parsePort(portStr, protoAddr);
        
        return new InetSocketAddress(hostName, port);
    }

    private int parsePort(String portStr, String fullAddr) {
        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch(NumberFormatException e) {
            throw new IllegalArgumentException("Port specified is not a valid number <" + fullAddr + ">");
        }
        
        if(port < 0 || port > 65535) {
            throw new IllegalArgumentException("Port specified is not in the legal range <" + fullAddr + ">");
        }
        return port;
    }

    public List<EndpointAddress> translateToExternalAddresses(SocketAddress bindpoint) {
        if(bindpoint == null) {
            return Collections.emptyList();
        }
        InetSocketAddress bpInet = (InetSocketAddress) bindpoint;
        
        List<EndpointAddress> externalAddresses = new LinkedList<EndpointAddress>();
        
        if(IPUtils.ANYADDRESS.equals(bpInet.getAddress())) {
            for(InetAddress localAddress : IPUtils.getAllLocalAddresses()) {
                
                // ignore loopback and link local addresses - they are useless for routing
                // in most scenarios.
                if (localAddress.isLoopbackAddress() || localAddress.isLinkLocalAddress()) {
                    continue;
                }

                SocketAddress externalAddress = new InetSocketAddress(localAddress, bpInet.getPort());
                externalAddresses.add(toEndpointAddress(externalAddress));
            }

            // we sort them so that later equals() will be deterministic.
            // the result of IPUtils.getAllLocalAddresses() is not known to 
            // be sorted.
            sortAddresses(externalAddresses);
        } else {
            externalAddresses.add(toEndpointAddress(bindpoint));
        }
        
        return externalAddresses;
    }

    private void sortAddresses(List<EndpointAddress> externalAddresses) {
        Collections.sort(externalAddresses, new Comparator<EndpointAddress>() {
            public int compare(EndpointAddress one, EndpointAddress two) {
                return one.toString().compareTo(two.toString());
            }

            public boolean equals(Object that) {
                return (this == that);
            }
        });
    }
    
    public String getProtocolName() {
        return protocol;
    }
}
