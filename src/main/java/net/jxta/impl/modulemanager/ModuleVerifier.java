package net.jxta.impl.modulemanager;

import java.util.Collection;
import java.util.logging.Logger;

import net.jxta.module.IModuleBuilder;
import net.jxta.module.IModuleClient;
import net.jxta.module.IModuleDescriptor;
import net.jxta.protocol.ModuleImplAdvertisement;

public class ModuleVerifier<T extends Object> implements IModuleClient{

	private static String S_WRN_COULD_NOT_LOAD_MODULE = "Could not load module: ";
	
	private Collection<IModuleBuilder<T>> builders;
	
	private ModuleConfig<T> config;
	
	private Logger logger = Logger.getLogger( this.getClass().getName() );

	//TODO temporary!!!
	public ModuleVerifier( Collection<IModuleBuilder<T>> builders) {
		this( new ModuleConfig<T>( null ), builders );
	}

	protected ModuleVerifier( ModuleConfig<T> config, Collection<IModuleBuilder<T>> builders) {
		super();
		this.config = config;
		this.builders = builders;
	}

	protected ModuleConfig<T> getConfig() {
		return config;
	}

	/**
	 * See if the given descriptor can be built by the builders
	 */
	public boolean canBuild(IModuleDescriptor descriptor) {
		for( IModuleBuilder<T> builder: builders ){
			if( this.acceptDescriptor(builder, descriptor))
				return true;
		}
		return false;
	}

	/**
	 * Verify the given descriptor by checking if the dependencies are resolved, and 
	 * @param builder
	 * @param descriptor
	 * @return
	 */
	public boolean acceptDescriptor( IModuleBuilder<T> builder, IModuleDescriptor descriptor ){
		
		if( builder.canBuild(descriptor))
			return true;
		
		if(! builder.supports( descriptor ))
			return false;
		
		//First check to see if the builder can resolve the dependencies
	    boolean result = false;
		for( IModuleDescriptor dependency: descriptor.getDependencies() ){
			if( acceptDescriptor( builder, dependency ))
				return true;
				
			//All the dependencies are resolved. Now try to see which builder can build the descriptor
			for( IModuleBuilder<T> blder: builders ){
				if( blder.equals( builder ))
					continue;
				if( this.acceptDescriptor( blder, dependency )){
					result = true;
					break;
				}
			}
			if( !result ){
				logger.warning( S_WRN_COULD_NOT_LOAD_MODULE + descriptor );
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Create a new verifier from the given descriptor
	 * @param descriptor
	 * @return
	 */
	public ModuleVerifier<T> createVerifier( IModuleDescriptor descriptor ){
		ModuleConfig<T> descendant = this.config.getModuleConfig(descriptor);
		if( descendant == null )
			return this;
		return new ModuleVerifier<T>( descendant, this.builders );
	}

	/**
	 * Create a new verifier from the given descriptor
	 * @param descriptor
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public ModuleVerifier<T> createVerifier( ModuleImplAdvertisement implAdv ){
		
		ModuleConfig<?> descendant = null;//ModuleConfig.getModuleConfig( this.config, implAdv);
		//if( descendant == null )
		//	return this;
		descendant = new ModuleConfig<T>( null );// TODO replace  
		return new ModuleVerifier<T>( (ModuleConfig<T>) descendant, this.builders );
	}
}
