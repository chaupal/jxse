This is by no means finished, however has been commited to aid collaboration.

How to run.
Install all dependancies (or configure for valid MVN repos)
Install a version of JXTA that will be used for testing in the maven repository.

"mvn clean install"  -- This will build and run tests

Noteworthy:
The main test of interest is the JUNIT test that uses PAXEXAM to run the JXTA components in OSGI bundles. net.jxta.osgi.TestCollocatedInstances
Three maven modules:
	osgi - This is the interfaces for JXTA in the OSGi environment. This is not intended to be a full interface. The purpose is to enable this to be shared with multiple OSGi impls, therefore can reference mupltiple instances in the same OSGi module, while maintaining complete separation through classloading.
	osgiimpl - This is the implementation of the osgi interface and wraps the JXTA classes.
	test - This has the test cases for JXTA.

Multiple instances of OSGi is achieved by copying and renaming the bundles and installing, since OSGi can not have packages with same name. This is done by bundle utils.


TODO: Presently equinox dependancies are installed locally. Need to be upgraded to the latest in MVN (these versions where not published in a mvn repo)
TODO: There are a few hacks to get BiDi Pipe working non direct when connected to a relay. This issue needs to be looked at in more detail.
