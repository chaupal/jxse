package net.jxse.module;

public interface IModuleService<T extends Object> {

	public String getIdentifier();

	public String getDescription();

	public String getVersion();

	/**
	 * Get the module, if it was created
	 * @return
	 */
	public T getModule();
}
