package net.jxta.impl.modulemanager;

import java.util.Collection;
import java.util.logging.Logger;

import net.jxta.module.IModuleBuilder;
import net.jxta.module.IModuleDescriptor;
import net.jxta.peergroup.core.Module;

public class ModuleVerifier<T extends Module> {

	private static String S_WRN_COULD_NOT_LOAD_MODULE = "Could not load module: ";
	
	private Collection<IModuleBuilder<T>> builders;
	
	private Logger logger = Logger.getLogger( this.getClass().getName() );
	
	public ModuleVerifier( Collection<IModuleBuilder<T>> builders) {
		super();
		this.builders = builders;
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
		
		//First check to see if the builder can resolve the dependencies
		for( IModuleDescriptor dependency: descriptor.dependencies() ){
			if( !acceptDescriptor( builder, dependency ))
				return false;
		}
				
		//All the dependencies are resolved. Now try to see which builder can build the descriptor
		for( IModuleBuilder<T> blder: builders ){
			if( blder.equals( builder ))
				continue;
			if(  this.acceptDescriptor(blder, descriptor))
				return true;
		}
		logger.warning( S_WRN_COULD_NOT_LOAD_MODULE + descriptor );
		return true;
	}
}
