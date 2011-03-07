package net.jxta.impl.util;

import org.junit.Test;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

/**
 * Created by IntelliJ IDEA.
 * User: boylejohnr
 * Date: 07/03/2011
 * Time: 21:46
 * Check that the bounds behaviour is correct.
 */
public class LimitedSizeMapTest
{
    @Test
    public void testOlderItemsDiscardedAndNewerRetained()
    {
        final LimitedSizeMap<Integer, Integer> limitedSizeMap = new LimitedSizeMap<Integer, Integer>(10);
        for (int i = 0 ; i < 10 ; i++)
        {
            limitedSizeMap.put(i,i);
        }
        for(int i = 0 ; i < 10 ; i++)
        {
            assertNotNull(limitedSizeMap.put(i, i));
        }
        assertNull(limitedSizeMap.put(11,11));
        assertNull(limitedSizeMap.get(0));
        assertNotNull(limitedSizeMap.get(1));
    }
}
