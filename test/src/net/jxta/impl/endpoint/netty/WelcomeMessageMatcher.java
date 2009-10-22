package net.jxta.impl.endpoint.netty;

import java.io.IOException;
import java.nio.ByteBuffer;

import net.jxta.impl.endpoint.msgframing.WelcomeMessage;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.TypeSafeMatcher;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

public class WelcomeMessageMatcher extends TypeSafeMatcher<ChannelBuffer> {

    private WelcomeMessage expectedWelcomeMessage;

    public WelcomeMessageMatcher(WelcomeMessage expectedWelcomeMessage) {
        this.expectedWelcomeMessage = expectedWelcomeMessage;
    }
    
    @Override
    public boolean matchesSafely(ChannelBuffer item) {
        try {
            ByteBuffer expected = expectedWelcomeMessage.getByteBuffer();
            return NettyTestUtils.checkEquals(ChannelBuffers.copiedBuffer(expected), item);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void describeTo(Description description) {
        description.appendText("a welcome message");
    }
    
    @Factory
    public static WelcomeMessageMatcher aWelcomeMessage(WelcomeMessage expected) {
        return new WelcomeMessageMatcher(expected);
    }

}
