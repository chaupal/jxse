#
# $Id: Makefile,v 1.27 2005/11/19 20:36:58 hamada Exp $
#

# if need to use a particular JDK set JAVA_HOME in your env.
# if you refrence additional libraries they need to be set in the
# CLASSPATH


# Under Windows, make sure we use the forward-slash form, not the back-slash form.
ifeq (cygwin,$(OSTYPE))
 override JAVA_HOME     := $(shell cygpath --path --unix "$(JAVA_HOME)")
 override CLASSPATH     := $(shell cygpath --path --unix "$(CLASSPATH)")
endif

ifneq ($(JAVA_HOME),)
 JAVAHOMEBIN      = $(JAVA_HOME)/bin/
else
 JAVAHOMEBIN      =
endif

ifeq ($(JXTAXMLLIB),)
JXTAXMLLIB = ""
endif

ifeq ($(JUNITLIB),)
 JUNITLIB = "../tools/junit.jar"
endif

 ifeq ($(LOG4JLIB),)
  LOG4JLIB = ../lib/log4j.jar
 endif
 ifeq ($(JXTAEXTRALIB),)
  JXTAEXTRALIB = ../dist/jxtaext.jar
 endif



ifneq ($(CLASSPATH),)
 JXTACLASSPATH      = $(CLASSPATH):$(CLASSDIR):$(shell find ../lib -type f -name '*.jar' -exec printf ':%s' {} \;):$(JXTAEXTRALIB)
else
 JXTACLASSPATH      = $(CLASSDIR):$(CLASSDIR):$(shell find ../lib -type f -name '*.jar' -exec printf ':%s' {} \;):$(JXTAEXTRALIB)
endif

ifneq ($(CLASSPATH),)
 JXTATOOLSPATH      = $(CLASSPATH):$(TOOLSDIR)
else
 JXTATOOLSPATH      = $(TOOLSDIR):
endif

JAVA          = $(JAVAHOMEBIN)java
JAVAC         = $(JAVAHOMEBIN)javac
JAVADOC       = $(JAVAHOMEBIN)javadoc
JAR           = $(JAVAHOMEBIN)jar
CP	      = cp -f
HEADER	      = "Project JXTA"
JXTA	      = "Project JXTA (TM) Technology 0.1 API Documentation"
JAVAFILES  = $(shell find src  -name '*.java' -print | grep -v -w 'CVS')
JAVADOCFILES  = $(JAVAAPIFILES)
JAVADOCREFFILES=$(JAVAREFFILES)
TOP           = $(shell pwd)/..
PWD	      = $(shell pwd)
CLASSDIR      = $(TOP)/classes
TCLASSDIR     = $(PWD)/classes
DISTDIR       = $(shell pwd)/dist
TOOLSDIR      = $(TOP)/tools
PKGDIR        = $(TOP)/pkg
DOCDIR	      = doc
REFDOCDIR     = refdoc
JXTASECJAR    = $(TOP)/../../../security/binding/java/dist/jxtasecurity.jar
EXTLIB        = $(JXTAXMLLIB):$(JUNITLIB):$(LOG4JLIB):$(JXTASECJAR)

# The classpath for things that require the EXTLIB files
JXTAEXTPATH   = "$(JXTACLASSPATH):$(EXTLIB):$(DISTDIR)/jxtatest.jar:$(TCLASSDIR)"

ifeq (true,$(OPTIMIZE))
 JAVACOPT=-O  -deprecation -g:none
else
 JAVACOPT= -deprecation
endif

#
# Cygwin uses the MS-Windows style classpath, so "fix" it for them
# The x versions must be used with non-cygwin utilities.  This
# includes:
#
#  javac, javadoc and jar
#
# Note: The external, or x, forms are used for arguments only,
#       not the path to the executable itself.
#
ifeq (cygwin,$(OSTYPE))
 JXTACLASSPATHx := "$(shell cygpath --path --windows "$(JXTACLASSPATH)")"
 JXTAEXTPATHx := "$(shell cygpath --path --windows "$(JXTAEXTPATH)")"
 JXTATOOLSPATHx := "$(shell cygpath --path --windows "$(JXTATOOLSPATH)")"
 CLASSDIRx := "$(shell cygpath --path --windows "$(CLASSDIR)")"
 TCLASSDIRx := "$(shell cygpath --path --windows "$(TCLASSDIR)")"
 TOOLSDIRx := "$(shell cygpath --path --windows "$(TOOLSDIR)")"
 MKBUILDTAGFILEx := "$(shell cygpath --path --windows "$(MKBUILDTAGFILE)")"
 BUILDTAGDIRx := "$(shell cygpath --path --windows "$(BUILDTAGDIR)")"
else
 JXTACLASSPATHx := "$(JXTACLASSPATH)"
 JXTAEXTPATHx := "$(JXTAEXTPATH)"
 CLASSDIRx := "$(CLASSDIR)"
 JXTATOOLSPATHx := "$(JXTATOOLSPATH)"
 TOOLSDIRx := "$(TOOLSDIR)"
 MKBUILDTAGFILEx := "$(MKBUILDTAGFILE)"
 BUILDTAGDIRx := "$(BUILDTAGDIR)"
endif


.PHONY: all compile javadoc refdoc clean clobber help

all: compile


compile:
	@echo building API Tests using $(JAVAC)
	@echo CLASSPATH = $(JXTACLASSPATHx)
	@if [ '!' -d $(TCLASSDIR) ]; then mkdir $(TCLASSDIR); fi;
	@if [ '!' -d $(DISTDIR) ]; then mkdir $(DISTDIR); fi;
	@$(JAVAC) $(JAVACOPT) -d $(TCLASSDIR) -classpath $(JXTAEXTPATHx) $(JAVAFILES)
	@cd $(TCLASSDIR);$(JAR) -cf $(DISTDIR)/jxtatest.jar *
	@echo Done building test suites.

runxpconf:
	$(JAVA) -classpath $(JXTAEXTPATH) net.jxta.impl.endpoint.XportConfTest

runid:
	$(JAVA) -classpath $(JXTAEXTPATH) net.jxta.id.IDTest
run_sink:
	$(JAVA) -classpath $(JXTAEXTPATH) net.jxta.test.util.JXTASink

runendpointapi:
	$(JAVA) -classpath $(JXTAEXTPATH) -Xmx64M net.jxta.impl.endpoint.EndpointApiTest

runrdvadv:
	$(JAVA) -classpath $(JXTAEXTPATH) net.jxta.protocol.RdvAdvertisementTest

runrouteadv:
	$(JAVA) -classpath $(JXTAEXTPATH) net.jxta.protocol.TestRouteAdv
runcm:
	$(JAVA) -classpath $(JXTAEXTPATH) net.jxta.impl.cm.CmTest
runsrdi:
	$(JAVA) -classpath $(JXTAEXTPATH) net.jxta.impl.cm.SrdiIndexTest
rdvload:
	$(JAVA) -classpath $(JXTAEXTPATH) net.jxta.rendezvous.RendezvousLoad
relayload:
	$(JAVA) -classpath $(JXTAEXTPATH) net.jxta.relay.RelayLoad
rundoctest:
	$(JAVA) -classpath $(JXTAEXTPATH) DocTest

run-reliable-sender:
	@if [ '!' -d ../send ]; then mkdir ../send; fi;
	cd ../send && $(JAVA) -classpath $(JXTAEXTPATH) net.jxta.impl.util.pipe.reliable.ReliableTest -sender -iterations 4000 -minload 32768 -maxload 32768 -quiet

run-reliable-receiver:
	@if [ '!' -d ../recv ]; then mkdir ../recv; fi;
	cd ../recv && $(JAVA) -classpath $(JXTAEXTPATH) net.jxta.impl.util.pipe.reliable.ReliableTest -server -iterations 4000 -quiet

run-reliable-receiver-d50:
	@if [ '!' -d ../recv ]; then mkdir ../recv; fi;
	cd ../recv && $(JAVA) -classpath $(JXTAEXTPATH) net.jxta.impl.util.pipe.reliable.ReliableTest -receiver -drop 2

run-reliable-receiver-d30:
	@if [ '!' -d ../recv ]; then mkdir ../recv; fi;
	cd ../recv && $(JAVA) -classpath $(JXTAEXTPATH) net.jxta.impl.util.pipe.reliable.ReliableTest -receiver -drop 3

run-reliable-receiver-d10:
	@if [ '!' -d ../recv ]; then mkdir ../recv; fi;
	cd ../recv && $(JAVA) -classpath $(JXTAEXTPATH) net.jxta.impl.util.pipe.reliable.ReliableTest -receiver -drop 10

run-reliable-help:
	@if [ '!' -d ../recv ]; then mkdir ../recv; fi;
	cd ../recv && $(JAVA) -classpath $(JXTAEXTPATH) net.jxta.impl.util.pipe.reliable.ReliableTest -help

run:
	$(JAVA) -classpath $(JXTAEXTPATH) junit.swingui.TestRunner

cleanclassdir:
	@echo cleaning $(TCLASSDIRx)
	@rm -rf $(TCLASSDIR)/net/jxta/test
cleanpkg:
	@echo cleaning pkg
	@rm -rf $(PKGDIR)
clean:
	@rm -rf $(TCLASSDIR)
	@rm -rf $(DOCDIR)
	@rm -fr $(REFDOCDIR)
	@rm -f TAGS tags

clobber: clean

help:
	@echo "# Usage : gnumake [all] [compile] [jar] [doc] [javadoc] [refdoc] [clean] [clobber] [ctags] [etags] [tags] [help]"

