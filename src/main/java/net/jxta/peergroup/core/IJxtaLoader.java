package net.jxta.peergroup.core;

import java.net.URL;

import net.jxta.peergroup.core.Module;
import net.jxta.peergroup.core.ModuleSpecID;
import net.jxta.protocol.ModuleImplAdvertisement;

/**
 * An interface which provides additional JXTA functionality to a variety of loaders, including class loaders
 * You can load classes by ModuleSpecID. Classes are defined with ModuleImplAdvertisements
 * and class loading will determine suitability using the provided
 * compatibility statements.
 */

public interface IJxtaLoader {

	public void addURL( URL url );
	
	/**
	 * Get the class loader for this loader
	 * @return
	 */
	public ClassLoader getClassLoader();
	
    /**
     * Finds and loads the class with the specified spec ID from the URL search
     * path. Any URLs referring to JAR files are loaded and opened as needed
     * until the class is found.
     *
     *  @param spec the specid of the class to load.
     *  @throws ClassNotFoundException if the class could not be found.
     *  @return the resulting class.
     */
    public abstract Class<? extends Module> findClass(ModuleSpecID spec) throws ClassNotFoundException;

    /**
     *  Loads the class with the specified spec ID from the URL search
     *  path.
     *
     *  @param spec the specid of the class to load.
     *  @throws ClassNotFoundException if the class could not be found.
     *  @return the resulting class.
     */
    public abstract Class<? extends Module> loadClass(ModuleSpecID spec) throws ClassNotFoundException;

    /**
     *  Defines a new class from a Module Impl Advertisement.
     *
     *  @param impl The moduleImplAdvertisement containing the class 
     *  specification
     *  @return The Class object that was created from the specified class data.
     */
    public abstract Class<? extends Module> defineClass(ModuleImplAdvertisement impl);

    /**
     *  Finds the ModuleImplAdvertisement for the associated class in the
     *  context of this ClassLoader.
     *
     *  @param clazz The class who's ModuleImplAdvertisement is desired.
     *  @return The matching {@code ModuleImplAdvertisement} otherwise
     *  {@code null} if there is no known association.
     */
    public abstract ModuleImplAdvertisement findModuleImplAdvertisement(Class<? extends Module> clazz);

    /**
     *  Finds the ModuleImplAdvertisement for the associated class in the 
     *  context of this ClassLoader.
     *
     *  @param msid The module spec id who's ModuleImplAdvertisement is desired.
     *  @return The matching {@code ModuleImplAdvertisement} otherwise
     *  {@code null} if there is no known association.
     */
    public abstract ModuleImplAdvertisement findModuleImplAdvertisement(ModuleSpecID msid);
}
