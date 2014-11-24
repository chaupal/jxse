package net.jxta.peergroup.core;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Element;
import net.jxta.document.StructuredDocument;
import net.jxta.document.XMLElement;
import net.jxta.impl.loader.RefJxtaLoader;
import net.jxta.impl.peergroup.CompatibilityEquater;
import net.jxta.impl.peergroup.CompatibilityUtils;
import net.jxta.impl.protocol.PeerGroupConfigAdv;
import net.jxta.impl.protocol.PeerGroupConfigFlag;
import net.jxta.logging.Logger;
import net.jxta.logging.Logging;
import net.jxta.module.IJxtaModuleFactory;
import net.jxta.module.IModuleFactory;
import net.jxta.module.IModuleManager;
import net.jxta.module.ModuleException;
import net.jxta.peergroup.IModuleDefinitions;
import net.jxta.peergroup.PeerGroup;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.PeerGroupAdvertisement;

public class JxtaLoaderModuleManager<T extends Module> implements IModuleManager<T>{

    /**
     * Default compatibility equater instance.
     */
    private static final CompatibilityEquater COMP_EQ =
    	new CompatibilityEquater() {
        public boolean compatible(Element<?> test) {
            return CompatibilityUtils.isCompatible(test);
        }
    };

    /**
     * Statically scoped JxtaLoader which is used as the root of the
     * JXTA class loader hierarchy.  The world peer group defers to the
     * static loader as it's parent class loader.
     * <p/>
     * This class loader is a concession.  Use the group-scoped loader
     * instead.
     * <p/>
     */
    private static IJxtaLoader staticLoader;
    private static JxtaLoaderModuleManager<Module> root;

    private final static transient Logger LOG = Logging.getLogger( JxtaLoaderModuleManager.class.getName());

    private IJxtaLoader loader;
    
    private static Map<PeerGroup, IModuleManager<? extends Module>> managers;
    
    private Collection<IJxtaModuleFactory<T>> factories;

	protected JxtaLoaderModuleManager( ClassLoader loader) {
		this.loader = new RefJxtaLoader(new URL[0], loader, COMP_EQ );
		managers = new HashMap<PeerGroup, IModuleManager<? extends Module>>();
		factories = new ArrayList<IJxtaModuleFactory<T>>();
	}

	private JxtaLoaderModuleManager( IJxtaLoader loader) {
		this.loader = loader;
		managers = new HashMap<PeerGroup, IModuleManager<? extends Module>>();
		factories = new ArrayList<IJxtaModuleFactory<T>>();
	}

	/**
	 * initialize the manager
	 */
	public void init(){
		
	}

	public IJxtaLoader getLoader() {
		return loader;
	}

	public void registerFactory(IModuleFactory<T> factory) {
		factories.add( (IJxtaModuleFactory<T>) factory );
	}
	
	/**
	 * Get the module manager for the given peergroup
	 * @param peergroup
	 * @return
	 */
	public IModuleManager<? extends Module> getModuleManagerforPeerGroup( PeerGroup peergroup ){
		return managers.get( peergroup );
	}
	
    /**
     *  Finds the ModuleImplAdvertisement for the associated class in the 
     *  context of this ClassLoader.
     *
     *  @param msid The module spec id who's ModuleImplAdvertisement is desired.
     *  @return The matching {@code ModuleImplAdvertisement} otherwise
     *  {@code null} if there is no known association.
     */
    public ModuleImplAdvertisement findModuleImplAdvertisement(ModuleSpecID msid){
    	for( IJxtaModuleFactory<T> factory: factories ){
    		if( factory.getModuleSpecID().equals( msid ))
    			return factory.getModuleImplAdvertisement();
    	}
    	return loader.findModuleImplAdvertisement(msid);
    }

	@SuppressWarnings("unchecked")
	public T getModule(ModuleImplAdvertisement adv) throws ModuleException {
		return (T) loadModule( loader, adv );
	}

	public T[] getModules(ModuleSpecID id) {
		// TODO Auto-generated method stub
		return null;
	}

	public Class<?> defineClass( ModuleImplAdvertisement impl ){
    	for( IJxtaModuleFactory<T> factory: factories ){
    		if( factory.getModuleSpecID().equals( impl.getModuleSpecID() ))
				return factory.createModule().getClass();
    	}
		return loader.defineClass(impl);
	}

	/**
	 * Find the module impl advertisement that is represented by the given class
	 * @param clss
	 * @return
	 */
	public ModuleImplAdvertisement findModuleImplAdvertisement( Class<? extends Module> clss ){
    	for( IJxtaModuleFactory<T> factory: factories ){
    		if( factory.getRepresentedClassName().equals( clss.getCanonicalName() ))
				return factory.getModuleImplAdvertisement();
    	}
		return loader.findModuleImplAdvertisement( clss);
	}

	/**
	 * Add a url to the loader
	 * @param url
	 */
	public void addURL( URL url ){
		loader.addURL(url);
	}
	
	protected static IModuleManager<? extends Module> getModuleManager( PeerGroup peergroup){
		if( managers.isEmpty())
			return root;
		IModuleManager<? extends Module> manager = managers.get( peergroup );
		return ( manager == null )? root: manager;
	}

	public static ClassLoader getClassLoader( PeerGroup peergroup){
		JxtaLoaderModuleManager<? extends Module> manager = (JxtaLoaderModuleManager<? extends Module>) managers.get( peergroup );
		return (ClassLoader) manager.getLoader();
	}

    @Override
	public int hashCode() {
		return loader.hashCode();
	}

	/**
     * Load a Module from a ModuleImplAdv.
     * <p/>
     * Compatibility is checked and load is attempted. If compatible and
     * loaded successfully, the resulting Module is initialized and returned.
     * In most cases the other loadModule() method should be preferred, since
     * unlike this one, it will seek many compatible implementation
     * advertisements and try them all until one works. The home group of the new
     * module (it's parent group if the new Module is a group) will be this group.
     *
     * @param assigned   Id to be assigned to that module (usually its ClassID).
     * @param implAdv    An implementation advertisement for that module.
     * @param privileged If {@code true} then the module is provided the true
     *                   group object instead of just an interface to the group object. This is
     *                   normally used only for the group's defined services and applications.
     * @return Module the module loaded and initialized.
     * @throws ModuleException 
     */
	protected static Module loadModule( IJxtaLoader loader, ModuleImplAdvertisement implAdv) throws ModuleException {

		Module loadedModule = null; 
		if ((null != implAdv.getCode()) && (null != implAdv.getUri())) {
			try{
			// Good one. Try it.
			Class<? extends Module> loadedModuleClass;
			try {
				loadedModuleClass = loader.loadClass(implAdv.getModuleSpecID());
			} catch (ClassNotFoundException exception) {
				loadedModuleClass = loader.defineClass(implAdv);
			}
			if (null == loadedModuleClass) {
				throw new ClassNotFoundException("Cannot load class (" + implAdv.getCode() + ") : ");
			}
            loadedModule = loadedModuleClass.newInstance();
			}
			catch( Exception ex ){
                Logging.logCheckedError(LOG,ex);
				
			}
		} else {
			String error;

			if (null == implAdv.getCode()) {
				error = "ModuleImpAdvertisement missing Code element";
			} else if (null == implAdv.getUri()) {
				error = "ModuleImpAdvertisement missing URI element";
			} else {
				error = "ModuleImpAdvertisement missing both Code and URI elements";
			}
			throw new ModuleException("Can not load module : " + error);
		}
		return loadedModule;
	}    

	/**
	 * Create a new module manager from the given parent group
	 * @param parentGroup
	 * @param peerGroupAdvertisement
	 * @return
	 */
	public static IModuleManager<Module> createModuleManager( PeerGroup peergroup, PeerGroupAdvertisement peerGroupAdvertisement ){
        IModuleManager<Module> manager = null;
        PeerGroup parentGroup = peergroup.getParentGroup();
        if (null == parentGroup) {

            Logging.logCheckedDebug(LOG, "Setting up group loader -> static loader");
            manager = root;

        } else {
            JxtaLoaderModuleManager<? extends Module> pm = (JxtaLoaderModuleManager<? extends Module>) managers.get( parentGroup ); 
            if( pm == null )
            	pm = root;
            IJxtaLoader upLoader = pm.getLoader();
            StructuredDocument<?> cfgDoc =
                    peerGroupAdvertisement.getServiceParam(
                    		IModuleDefinitions.peerGroupClassID);
            PeerGroupConfigAdv pgca;
            if (cfgDoc != null) {
                pgca = (PeerGroupConfigAdv)
                        AdvertisementFactory.newAdvertisement((XMLElement<?>)
                        peerGroupAdvertisement.getServiceParam(IModuleDefinitions.peerGroupClassID));
                if (pgca.isFlagSet(PeerGroupConfigFlag.SHUNT_PARENT_CLASSLOADER)) {
                    // We'll shunt to the same class loader that loaded this
                    // class, but not the JXTA form (to prevent Module
                    // definitions).
                    upLoader = (IJxtaLoader)JxtaLoaderModuleManager.class.getClassLoader();
                }
            }

            Logging.logCheckedDebug(LOG, "Setting up group loader -> ", upLoader);
            manager = new JxtaLoaderModuleManager<Module>(upLoader );
            managers.put( peergroup, manager);
        }
        return manager;
		
	}

	/**
	 * Get a root manager for the given class
	 * @param clzz
	 * @return
	 */
	public static JxtaLoaderModuleManager<Module> getRoot(){
		return getRoot(RefJxtaLoader.class);
	}

	/**
	 * Get a root manager for the given class
	 * @param clzz
	 * @return
	 */
	public static JxtaLoaderModuleManager<Module> getRoot( Class<?> clzz ){
		if( root == null ){
			staticLoader = new RefJxtaLoader( new URL[0], clzz.getClassLoader(), COMP_EQ);
			root = new JxtaLoaderModuleManager<Module>( staticLoader );
			root.init();
		}
		return root;
	}
}
