package net.jxta.impl.cm.bdb;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jxta.logging.Logging;

public class CleanerTask extends TimerTask {

	private static final Logger LOG = Logger.getLogger(CleanerTask.class.getName());
	
	private WeakReference<BerkeleyDbAdvertisementCache> cacheRef;
	
	public CleanerTask(BerkeleyDbAdvertisementCache cache) {
		cacheRef = new WeakReference<BerkeleyDbAdvertisementCache>(cache);
	}
	
	@Override
	public void run() {
		BerkeleyDbAdvertisementCache cache = cacheRef.get();
		if(cache == null) {
			// the cache is dead, stop trying to clean it
			this.cancel();
			return;
		}
		
		try {
			cache.garbageCollect();
		} catch (IOException e) {
			if(Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
				LOG.log(Level.WARNING, "Error occurred while attempting to clean up cache", e);
			}
		}
	}

}
