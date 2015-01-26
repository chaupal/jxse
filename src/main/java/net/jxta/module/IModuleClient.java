package net.jxta.module;

public interface IModuleClient {

	/**
	 * Returns true if the client can build the module with the given descriptor
	 * @param descriptor
	 * @return
	 */
	public boolean canBuild( IModuleDescriptor descriptor );
}
