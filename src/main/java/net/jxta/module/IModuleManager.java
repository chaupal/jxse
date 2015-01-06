package net.jxta.module;

import net.jxta.impl.modulemanager.ModuleException;
import net.jxta.peergroup.core.ModuleSpecID;
import net.jxta.protocol.ModuleImplAdvertisement;

public interface IModuleManager<T extends Object> {

	/**
	 * Register a builder with the manager
	 * @param factory
	 */
	public void registerBuilder( IModuleBuilder<T> builder );

	/**
	 * Register a builder with the manager
	 * @param factory
	 */
	public void unregisterBuilder( IModuleBuilder<T> builder );

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
}
