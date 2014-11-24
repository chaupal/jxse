package net.jxta.module;

import net.jxta.peergroup.core.Module;
import net.jxta.peergroup.core.ModuleSpecID;
import net.jxta.protocol.ModuleImplAdvertisement;

public interface IModuleManager<T extends Module> {

	/**
	 * Register a factory with the manager
	 * @param factory
	 */
	public void registerFactory( IModuleFactory<T> factory );

    /**
     *  Finds the ModuleImplAdvertisement for the associated class in the 
     *  context of this ClassLoader.
     *
     *  @param msid The module spec id who's ModuleImplAdvertisement is desired.
     *  @return The matching {@code ModuleImplAdvertisement} otherwise
     *  {@code null} if there is no known association.
     */
    public abstract ModuleImplAdvertisement findModuleImplAdvertisement(ModuleSpecID msid);

	/**
	 * Get the module with the given module implementation advertisement, or null if none were found
	 * @param adv
	 * @return
	 * @throws ModuleException 
	 */
	public T getModule( ModuleImplAdvertisement adv ) throws ModuleException;
	
	/**
	 * Get all the modules conforming to the given module spec id.
	 * @param id
	 * @return
	 */
	public T[] getModules( ModuleSpecID id );
}
