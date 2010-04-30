package net.jxta.impl.endpoint.netty;

import static org.junit.Assert.*;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import net.jxta.endpoint.EndpointAddress;

import org.junit.Before;
import org.junit.Test;

public class InetSocketAddressTranslatorTest {

    public static final String TEST_PROTO_NAME = "test";

    private static final byte[] TEST_IPv6_ADDR_BYTES = new byte[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 };
    private static final byte[] TEST_IPV4_ADDR_BYTES = new byte[] { 1, 1, 1, 1};
    
    public static final Inet6Address TEST_INET6_ADDR = createAddress(TEST_IPv6_ADDR_BYTES, Inet6Address.class);
    public static final Inet4Address TEST_INET4_ADDR = createAddress(TEST_IPV4_ADDR_BYTES, Inet4Address.class);
    
    private static <T extends InetAddress> T createAddress(byte[] addrBytes, Class<T> type) {
        InetAddress addr;
        try {
            addr = InetAddress.getByAddress(addrBytes);
            if(type.equals(addr.getClass())) {
                return type.cast(addr);
            }
            throw new Error("address was not of the expected type");
        } catch(UnknownHostException e) {
            throw new Error("host resolution failed");
        }
    }
    
    private InetSocketAddressTranslator translator;
    
    @Before
    public void setUp() throws Exception {
        translator = new InetSocketAddressTranslator(TEST_PROTO_NAME);
    }

    @Test
    public void testToEndpointAddress_withIPv6Address() {
        InetSocketAddress socketAddr = new InetSocketAddress(TEST_INET6_ADDR, 1234);
        EndpointAddress translated = translator.toEndpointAddress(socketAddr);
        assertEquals("[101:101:101:101:101:101:101:101]:1234", translated.getProtocolAddress());
    }
    
    @Test
    public void testToEndpointAddress_withIPv4Address() {
        InetSocketAddress socketAddr = new InetSocketAddress(TEST_INET4_ADDR, 12345);
        EndpointAddress translated = translator.toEndpointAddress(socketAddr);
        assertEquals("1.1.1.1:12345", translated.getProtocolAddress());
    }
    
    @Test
    public void testToEndpointAddress_withUnresolvedHostName() {
        InetSocketAddress socketAddr = new InetSocketAddress(TEST_INET4_ADDR, 12345);
        EndpointAddress translated = translator.toEndpointAddress(socketAddr);
        
        assertEquals(TEST_PROTO_NAME, translated.getProtocolName());
        assertEquals("1.1.1.1:12345", translated.getProtocolAddress());
        assertNull(translated.getServiceName());
        assertNull(translated.getServiceParameter());
    }

    @Test
    public void testToSocketAddress() {
        EndpointAddress addr = new EndpointAddress(TEST_PROTO_NAME, "1.1.1.1:12345", null, null);
        SocketAddress translated = translator.toSocketAddress(addr);
        assertTrue(translated instanceof InetSocketAddress);
        InetSocketAddress socketAddr = (InetSocketAddress)translated;
        assertEquals(TEST_INET4_ADDR, socketAddr.getAddress());
        assertEquals(12345, socketAddr.getPort());
    }
    
    @Test
    public void testToSocketAddress_withIPv6Address() {
        EndpointAddress addr = new EndpointAddress(TEST_PROTO_NAME, "[101:101:101:101:101:101:101:101]:50000", null, null);
        SocketAddress translated = translator.toSocketAddress(addr);
        assertTrue(translated instanceof InetSocketAddress);
        InetSocketAddress socketAddr = (InetSocketAddress)translated;
        assertEquals(TEST_INET6_ADDR, socketAddr.getAddress());
        assertEquals(50000, socketAddr.getPort());
    }
    
    @Test
    public void testToSocketAddress_withNoPortSpecified() {
        checkParseThrowsIllegalArgumentException("1.1.1.1", "No port specified in address <1.1.1.1>");
    }
    
    @Test
    public void testToSocketAddress_withNoPortSpecifiedAfterColon() {
        checkParseThrowsIllegalArgumentException("1.1.1.1:", "No port specified in address <1.1.1.1:>");
    }
    
    @Test
    public void testToSocketAddress_withIllegalPortSpecified() {
        checkParseThrowsIllegalArgumentException("1.1.1.1:70000", "Port specified is not in the legal range <1.1.1.1:70000>");
    }
    
    @Test
    public void testToSocketAddress_withNegativePortSpecified() {
        checkParseThrowsIllegalArgumentException("1.1.1.1:-5", "Port specified is not in the legal range <1.1.1.1:-5>");
    }
    
    @Test
    public void testToSocketAddress_withNonNumericPortSpecified() {
        checkParseThrowsIllegalArgumentException("1.1.1.1:http", "Port specified is not a valid number <1.1.1.1:http>");
    }
    
    @Test
    public void testToSocketAddress_withNoHostSpecified() {
        checkParseThrowsIllegalArgumentException(":5000", "No host specified in address <:5000>");
    }
    
    @Test
    public void testToSocketAddress_withNoPortSpecified_IPv6() {
        checkParseThrowsIllegalArgumentException("[101:101:101:101:101:101:101:101]", "No port specified in IPv6 address <[101:101:101:101:101:101:101:101]>");
    }
    
    @Test
    public void testToSocketAddress_withNoPortSpecifiedAfterColon_IPv6() {
        checkParseThrowsIllegalArgumentException("[101:101:101:101:101:101:101:101]:", "No port specified in IPv6 address <[101:101:101:101:101:101:101:101]:>");
    }
    
    @Test
    public void testToSocketAddress_withIllegalPortSpecified_IPv6() {
        checkParseThrowsIllegalArgumentException("[101:101:101:101:101:101:101:101]:70000", "Port specified is not in the legal range <[101:101:101:101:101:101:101:101]:70000>");
    }
    
    @Test
    public void testToSocketAddress_withNegativePortSpecified_IPv6() {
        checkParseThrowsIllegalArgumentException("[101:101:101:101:101:101:101:101]:-5", "Port specified is not in the legal range <[101:101:101:101:101:101:101:101]:-5>");
    }
    
    @Test
    public void testToSocketAddress_withNonNumericPortSpecified_IPv6() {
        checkParseThrowsIllegalArgumentException("[101:101:101:101:101:101:101:101]:http", "Port specified is not a valid number <[101:101:101:101:101:101:101:101]:http>");
    }
    
    @Test
    public void testToSocketAddress_withIncorrectlyEscapedIPv6Address_missingCloseBracket() {
        checkParseThrowsIllegalArgumentException("[101:101:101:101:101:101:101:101:12345", "Address is not a valid IPv6 address <[101:101:101:101:101:101:101:101:12345>");
    }
    
    @Test
    public void testToSocketAddress_withIncorrectlyEscapedIPv6Address_missingOpenBracket() {
        checkParseThrowsIllegalArgumentException("101:101:101:101:101:101:101:101]:12345", "Address is not a valid IPv6 address <101:101:101:101:101:101:101:101]:12345>");
    }
    
    private void checkParseThrowsIllegalArgumentException(String addrStr, String exceptionMessage) {
        EndpointAddress addr = new EndpointAddress(TEST_PROTO_NAME, addrStr, null, null);
        try {
            translator.toSocketAddress(addr);
            fail("Expected IllegalArgumentException");
        } catch(IllegalArgumentException e) {
            assertEquals(exceptionMessage, e.getMessage());
        }
    }

}
