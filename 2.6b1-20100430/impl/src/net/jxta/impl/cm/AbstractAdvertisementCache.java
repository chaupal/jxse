package net.jxta.impl.cm;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;

/**
 * Helper class for new AdvertisementCache implementations. Deals with the simple method overrides,
 * passing the defined standard parameters to their more precise siblings.
 */
public abstract class AbstractAdvertisementCache implements AdvertisementCache {

    public void save(String dn, String fn, Advertisement adv) throws IOException {
        save(dn, fn, adv, DiscoveryService.INFINITE_LIFETIME, DiscoveryService.NO_EXPIRATION);
    }

    public List<InputStream> getRecords(String dn, int threshold, List<Long> expirations) throws IOException {
        return getRecords(dn, threshold, expirations, false);
    }

}
