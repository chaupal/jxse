package net.jxta.impl.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: boylejohnr
 * Date: 07/03/2011
 * Time: 21:32
 * Provide Simple way to limit size of map, this class at time of writing was to maintain a
 * cached of processed messages ID's to avoid circular messages.
 */
public class LimitedSizeMap<K, V> extends LinkedHashMap<K,V>
{
    private final int maxSize;

    public LimitedSizeMap(int maxSize)
    {
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> kvEntry)
    {
        return size() > maxSize;

    }
}
