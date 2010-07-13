#
# $Id: Makefile,v 1.99 2007/02/01 18:03:49 hamada Exp $
#

# if need to use a particular JDK set JAVA_HOME in your env.
#
# if you refrence additional libraries they need to be set in the
# CLASSPATH


ifneq ($(JXTA_HOME),)
 JHOME      = $(JXTA_HOME)
else
 JHOME      = .jxta
endif

ifneq ($(JAVA_HOME),)
 JAVAHOMEBIN      = $(JAVA_HOME)/bin/
else
 JAVAHOMEBIN      =
endif

JAVA          = $(JAVAHOMEBIN)java
JAVAC         = $(JAVAHOMEBIN)javac
JAR           = $(JAVAHOMEBIN)jar
CP	      = cp -f

TOP           = $(shell pwd)
METERSETDIR   = $(TOP)/build/class
CLASSDIR      = $(TOP)/classes
DISTDIR	      = $(TOP)/dist

JAVABUILDFILES= $(shell find build -name '*.java' -print)
JAVAAPIFILES  = $(shell find api -name '*.java' -print)
JAVAREFFILES  = $(shell find impl -name '*.java' -print)


JXTASHELLJAR  = $(TOP)/../jxse-shell/dist/jxtashell.jar
MKMETERSETFILE=$(TOP)/build/src/net/jxta/build/ConditionalBuild.java
METERPROPFILE = build/meterRuntimeBuild.properties
METEROUTDIR   = build_generated

ifeq ($(JXTAEXTRALIB),)
 JXTAEXTRALIB = ""
endif

ifneq ($(CLASSPATH),)
 JXTACLASSPATH      = $(CLASSPATH):$(CLASSDIR)$(shell find lib -type f -name '*.jar' -exec printf ':%s' {} \;):$(JXTAEXTRALIB)
else
 JXTACLASSPATH      = $(CLASSDIR)$(shell find lib -type f -name '*.jar' -exec printf ':%s' {} \;):$(JXTAEXTRALIB)
endif

ifneq ($(CLASSPATH),)
 JXTATOOLSPATH      = $(CLASSPATH):$(TOOLSDIR)
else
 JXTATOOLSPATH      = $(TOOLSDIR)
endif

ifneq ($(CLASSPATH),)
 JXTAMETERSETPATH      = $(CLASSPATH):$(METERSETDIR)
else
 JXTAMETERSETPATH      = $(METERSETDIR)
endif

ifeq (true,$(OPTIMIZE))
 JAVACOPT=-O -g:none -source 1.5 -target 1.5
else
 JAVACOPT=-g -source 1.5 -target 1.5
endif

ifneq ($(METER),)
  ifeq (true,$(METER))
    METERPROPFILE = build/meterOnBuild.properties
  else
    METERPROPFILE = build/meterOffBuild.properties
  endif
endif

CONFIGFILE=impl/src/net/jxta/impl/config.properties

MONITORCFGFILE=impl/src/net/jxta/impl/monitor.properties

USERFILE=api/src/net/jxta/user.properties

METAINFPROPERTYFILES=impl/src/META-INF/services/net.jxta.*           

JXTACLASSPATHx := "$(JXTACLASSPATH)"
CLASSDIRx := "$(CLASSDIR)"
JXTATOOLSPATHx := "$(JXTATOOLSPATH)"
TOOLSDIRx := "$(TOOLSDIR)"
METERSETDIRx := "$(METERSETDIR)"
MKMETERSETFILEx := "$(MKMETERSETFILE)"
METERPROPFILEx	:= "$(METERPROPFILE)"
METEROUTDIRx := "$(METEROUTDIR)"
JXTAMETERSETPATHx := "$(JXTAMETERSETPATH)"

PHONIES = all clean clobber help compile meterSet compileSrc jar run runawt runc runs runcs tags etags ctags

.PHONY: $(PHONIES)

all: clobber compile

compile: jar 

meterSet:
	@if [ '!' -d $(METERSETDIRx) ]; then mkdir $(METERSETDIRx); fi;
	@$(JAVAC) -d $(METERSETDIRx)  $(MKMETERSETFILEx)
	@echo Metering : $(METERPROPFILEx)
	@$(JAVA) -cp $(JXTAMETERSETPATHx) net.jxta.build.ConditionalBuild $(METERPROPFILEx) $(METEROUTDIRx)

compileSrc: cleanclassdir meterSet
	@echo building ALL using $(JAVAC)
	@echo CLASSPATH = $(JXTACLASSPATHx)
	@if [ '!' -d $(CLASSDIR) ]; then mkdir $(CLASSDIR); fi;
	@$(JAVAC) $(JAVACOPT) -d $(CLASSDIRx) -classpath $(JXTACLASSPATHx) $(JAVAAPIFILES) $(JAVAREFFILES) $(shell find $(METEROUTDIRx) -name '*.java' -print)
	@if [ '!' -d $(CLASSDIR)/META-INF/services/ ]; then mkdir -p $(CLASSDIR)/META-INF/services/; fi;
	@$(CP) $(METAINFPROPERTYFILES) $(CLASSDIR)/META-INF/services/
	@$(CP) $(CONFIGFILE) $(CLASSDIR)/net/jxta/impl/config.properties
	@$(CP) $(MONITORCFGFILE) $(CLASSDIR)/net/jxta/impl/monitor.properties
	@$(CP) $(USERFILE) $(CLASSDIR)/net/jxta/
	@echo Done building all.

run:
	nohup $(JAVA) -server -Xms64m -Xmx192m -DJXTA_HOME=$(JHOME) -Dnet.jxta.tls.password="password" -classpath $(JXTACLASSPATH) contrib.rendezvous.Rendezvous &


runs:
	nohup $(JAVA) -server -Xms64m -Xmx192m -DJXTA_HOME=$(JHOME) -Dnet.jxta.tls.password="password" -classpath $(JXTACLASSPATH):$(JXTASHELLJAR) contrib.rendezvous.Rendezvous &

runawt:
	nohup $(JAVA) -server -Xms64m -Xmx192m -DJXTA_HOME=$(JHOME) -classpath $(JXTACLASSPATH) contrib.rendezvous.WRendezvous &

runc:
	$(JAVA) -DDEBUG -server -Xms64m -Xmx192m -DJXTA_HOME=$(JHOME) -Dnet.jxta.tls.password="password" -classpath $(JXTACLASSPATH) contrib.rendezvous.Rendezvous 

runcs:
	$(JAVA) -server -Xms64m -Xmx192m -Dnet.jxta.tls.password="password" -DJXTA_HOME=$(JHOME) -classpath $(JXTACLASSPATH):$(JXTASHELLJAR) contrib.rendezvous.Rendezvous 

srdichk:
	$(JAVA) -Xms64m -Xmx192m -classpath $(JXTACLASSPATH):$(JXTASHELLJAR) CheckSrdi $(JHOME)

cmchk:
	$(JAVA) -Xms64m -Xmx192m -classpath $(JXTACLASSPATH):$(JXTASHELLJAR) CheckCm  $(JHOME)

jar:  compileSrc
	@if [ '!' -d $(DISTDIR) ]; then mkdir $(DISTDIR); fi;
	@echo Creating $(DISTDIR)/jxta.jar
	@cd $(CLASSDIR); $(JAR) -cf $(DISTDIR)/jxta.jar net META-INF; unzip -l $(DISTDIR)/jxta.jar |grep files

cleanmetering:
	@echo cleaning $(METEROUTDIRx)
	@rm -rf $(METEROUTDIRx)
        # XXX Temporary hack to delete old generated metering files.
	@echo cleaning old meter settings.
	@find impl/src -type f -name '*MeterBuildSettings.java' -exec rm {} \;

cleanclassdir:
	@echo cleaning $(CLASSDIRx)
	@rm -rf $(CLASSDIR)

cleandist:
	@echo cleaning $(DISTDIR)
	@rm -rf $(DISTDIR)

clean: cleanclassdir cleanmetering
	@rm -f TAGS tags

clobber: clean cleandist

ctags:
	@echo Creating $@
	@ctags $(JAVAAPIFILES) $(JAVAREFFILES)

etags:
	@echo Creating $@
	@etags $(JAVAAPIFILES) $(JAVAREFFILES)

tags: ctags etags

help: 
	@echo -n "# Usage : gnumake "
	@for eachtarget in $(PHONIES) ; do echo -n " [$$eachtarget]" ; done
	@echo ""		
