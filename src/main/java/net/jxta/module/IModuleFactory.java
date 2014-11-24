package net.jxta.module;

public interface IModuleFactory<T extends Object> {

	public String getIdentifier();

	public String getDescription();

	/**
	 * Create the module
	 * @return
	 */
	public T createModule();
}
