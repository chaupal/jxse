package net.jxta.peergroup;

import net.jxta.impl.cm.CacheManager;

public interface ICachedPeerGrroup extends PeerGroup{
    /**
     * @return the cache manager associated with this group.
     */
    public abstract CacheManager getCacheManager();
}