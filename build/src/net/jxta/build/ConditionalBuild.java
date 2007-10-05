/*
 *
 * $Id: ConditionalBuild.java,v 1.8 2006/03/01 19:53:48 bondolo Exp $
 *
 * Copyright (c) 2002 Sun Microsystems, Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *       Sun Microsystems, Inc. for Project JXTA."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact Project JXTA at http://www.jxta.org.
 *
 * 5. Products derived from this software may not be called "JXTA",
 *    nor may "JXTA" appear in their name, without prior written
 *    permission of Sun.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL SUN MICROSYSTEMS OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of Project JXTA.  For more
 * information on Project JXTA, please see
 * <http://www.jxta.org/>.
 *
 * This license is based on the BSD license adopted by the Apache Foundation.
 */

package net.jxta.build;

import java.util.*;
import java.io.*;

public class ConditionalBuild {

    /**
     *  Conditional build states
     */
    enum BuildConfig {

        OFF("off"), ON("on"), RUNTIME("runtime");
        /**
         * The properties file value which matches this state.
         */
        private final String config;

        /**
         *  Construct a new BuildConfig value.
         *
         *  @param config The configuration value as it would appear in a
         *  properties file.
         */
        private BuildConfig(String config) {
            this.config = config;
        }

        /**
         * Convert a properties file String value to an enum value.
         *
         * @param key String
         * @return A ConditionaBuildState object.
         * @throws IllegalArgumentException For illegal protocol codes.
         */
        public static BuildConfig toBuildConfig(String config) {
            if (OFF.config.equals(config)) {
                return OFF;
            } else if (ON.config.equals(config)) {
                return ON;
            } else if (RUNTIME.config.equals(config)) {
                return RUNTIME;
            } else {
                throw new IllegalArgumentException("Unknown Build Type: " + config + " found in property file (valid Types: on, off, runtime)");
            }
        }

        /**
         * Return the value that would appear in a properties file for this State.
         *
         * @return The
         */
        public String toPropertiesKey() {
            return config;
        }
    }

    private class StaticField {

        final String packageName;
        final String className;
        final String fieldName;
        final BuildConfig config;
        final String attr;

        StaticField(String packageName, String className, String fieldName, BuildConfig config, String attr) {
            this.packageName = packageName;
            this.className = className;
            this.fieldName = fieldName;
            this.config = config;
            this.attr = attr;
        }
    }
    private final File baseDir;
    private final Map<String, List<StaticField>> sourceFiles = new HashMap<String, List<StaticField>>();

    public ConditionalBuild(String baseDirName) throws IOException {
        baseDir = new File(baseDirName);
    }

    public void createFiles(File configProperties) throws IOException {
        System.err.println("Creating Conditional Build files in " + baseDir);

        Properties properties = new Properties();

        properties.load(new FileInputStream(configProperties));

        for (Enumeration e = properties.propertyNames(); e.hasMoreElements();) {
            String propertyName = (String) e.nextElement();

            parseProperty(propertyName, properties.getProperty(propertyName));
        }

        generateFiles();
    }

    private void generateFiles() throws IOException {
        for (String filename : sourceFiles.keySet()) {
            List<StaticField> staticFields = sourceFiles.get(filename);

            generateFile(filename, staticFields);
        }
    }

    private void generateFile(String filename, List<StaticField> staticFields) throws IOException {
        StaticField firstStaticField = staticFields.get(0);

        String srcDirName = firstStaticField.packageName.replace('.', File.separatorChar);

        File srcDir = new File(baseDir, srcDirName);

        srcDir.mkdirs();

        File file = new File(srcDir, filename + ".java");

        System.err.println("\tCreate conditional build file : " + srcDirName + File.separatorChar + file.getName());

        PrintWriter writer = new PrintWriter(new FileWriter(file));

        insertCopyright(writer);

        writer.println();
        writer.println("/*  ****  THIS IS A GENERATED FILE. DO NOT EDIT.  ****  */");
        writer.println();
        writer.println("package " + firstStaticField.packageName + ";");
        writer.println();
        writer.println("import net.jxta.impl.meter.*;");
        writer.println();
        writer.print("public interface " + firstStaticField.className);

        if (!firstStaticField.className.equals("MeterBuildSettings")) {
            writer.print(" extends MeterBuildSettings");
        }

        writer.println(" {");

        for (StaticField staticField : staticFields) {
            writer.print("\tpublic static final boolean " + staticField.fieldName + " = ");

            switch (staticField.config) {
                case OFF:
                case ON:
                    writer.print(Boolean.toString(BuildConfig.ON == staticField.config));
                    break;
                case RUNTIME:
                    String conditionalClassName = "Conditional" + staticField.className;
                    File conditionalFile = new File(srcDir, "Conditional" + staticField.className + ".java");

                    writer.print(conditionalClassName + ".isRuntimeMetering()");
                    makeConditionalFile(conditionalFile, staticField.packageName, conditionalClassName, staticField.attr);
            }
            
            writer.println(";");
        }

        writer.println("}");

        writer.close();
    }

    private void makeConditionalFile(File conditionalFile, String packageName, String className, String propertyName) throws IOException {
        PrintWriter writer = new PrintWriter(new FileWriter(conditionalFile));

        System.err.println("\tCreate runtime conditional build file : " + packageName.replace('.', File.separatorChar) + File.separatorChar + conditionalFile.getName());

        insertCopyright(writer);

        writer.println();
        writer.println("/*  ****  THIS IS A GENERATED FILE. DO NOT EDIT.  ****  */");
        writer.println();
        writer.println("package " + packageName + ";");
        writer.println();
        writer.println("import java.util.ResourceBundle;");
        writer.println("import net.jxta.impl.meter.*;");
        writer.println();
        writer.println("public class " + className + " {");
        writer.println("\tpublic static boolean isRuntimeMetering() {");
        writer.println("\t\tboolean runtimeMetering = false; ");
        writer.println();
        writer.println("\t\ttry { ");
        writer.println("\t\t\tResourceBundle userResourceBundle = ResourceBundle.getBundle( \"net.jxta.user\" ); ");
        writer.println("\t\t\tString meteringProperty = \"" + propertyName + "\"; ");
        writer.println("\t\t\tString meteringValue = userResourceBundle.getString( meteringProperty ); ");
        writer.println("\t\t\truntimeMetering = \"on\".equalsIgnoreCase( meteringValue ); ");
        writer.println("\t\t} catch (Exception ignored) { ");
        writer.println("\t\t}");
        writer.println();
        writer.println("\t\treturn runtimeMetering;");
        writer.println("\t}");
        writer.print("}");
        writer.close();
    }

    private void parseProperty(String propertyName, String value) {
        int pos = propertyName.lastIndexOf('.');
        String fullClassName = propertyName.substring(0, pos);
        String fieldName = propertyName.substring(pos + 1);

        int pos2 = fullClassName.lastIndexOf('.');
        String packageName = fullClassName.substring(0, pos2);
        String className = fullClassName.substring(pos2 + 1);

        StringTokenizer st = new StringTokenizer(value, ", \t");

        String buildConfig = st.nextToken();

        BuildConfig config = BuildConfig.toBuildConfig(buildConfig);

        String attr = st.nextToken();
        StaticField staticField = new StaticField(packageName, className, fieldName, config, attr);

        List<StaticField> fields = sourceFiles.get(className);

        if (fields == null) {
            fields = new ArrayList<StaticField>();
            sourceFiles.put(className, fields);
        }

        fields.add(staticField);
    }

    private void insertCopyright(PrintWriter writer) {
        writer.println("/*");
        writer.println(" *  The Sun Project JXTA(TM) Software License");
        writer.println(" *  ");
        writer.println(" *  Copyright (c) 2001-2006 Sun Microsystems, Inc. All rights reserved.");
        writer.println(" *  ");
        writer.println(" *  Redistribution and use in source and binary forms, with or without ");
        writer.println(" *  modification, are permitted provided that the following conditions are met:");
        writer.println(" *  ");
        writer.println(" *  1. Redistributions of source code must retain the above copyright notice,");
        writer.println(" *     this list of conditions and the following disclaimer.");
        writer.println(" *  ");
        writer.println(" *  2. Redistributions in binary form must reproduce the above copyright notice, ");
        writer.println(" *     this list of conditions and the following disclaimer in the documentation ");
        writer.println(" *     and/or other materials provided with the distribution.");
        writer.println(" *  ");
        writer.println(" *  3. The end-user documentation included with the redistribution, if any, must ");
        writer.println(" *     include the following acknowledgment: \"This product includes software ");
        writer.println(" *     developed by Sun Microsystems, Inc. for JXTA(TM) technology.\" ");
        writer.println(" *     Alternately, this acknowledgment may appear in the software itself, if ");
        writer.println(" *     and wherever such third-party acknowledgments normally appear.");
        writer.println(" *  ");
        writer.println(" *  4. The names \"Sun\", \"Sun Microsystems, Inc.\", \"JXTA\" and \"Project JXTA\" must ");
        writer.println(" *     not be used to endorse or promote products derived from this software ");
        writer.println(" *     without prior written permission. For written permission, please contact ");
        writer.println(" *     Project JXTA at https://jxta.dev.java.net.");
        writer.println(" *  ");
        writer.println(" *  5. Products derived from this software may not be called \"JXTA\", nor may ");
        writer.println(" *     \"JXTA\" appear in their name, without prior written permission of Sun.");
        writer.println(" *  ");
        writer.println(" *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,");
        writer.println(" *  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND ");
        writer.println(" *  FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL SUN ");
        writer.println(" *  MICROSYSTEMS OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, ");
        writer.println(" *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT ");
        writer.println(" *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, ");
        writer.println(" *  OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF ");
        writer.println(" *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING ");
        writer.println(" *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, ");
        writer.println(" *  EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.");
        writer.println(" *  ");
        writer.println(" *  JXTA is a registered trademark of Sun Microsystems, Inc. in the United ");
        writer.println(" *  States and other countries.");
        writer.println(" *  ");
        writer.println(" *  Please see the license information page at :");
        writer.println(" *  <https://jxta.dev.java.net/license.html> for instructions on use of ");
        writer.println(" *  the license in source files.");
        writer.println(" *  ");
        writer.println(" *  ====================================================================");
        writer.println("");
        writer.println(" *  This software consists of voluntary contributions made by many individuals ");
        writer.println(" *  on behalf of Project JXTA. For more information on Project JXTA, please see ");
        writer.println(" *  https://jxta.dev.java.net/");
        writer.println(" *  ");
        writer.println(" *  This license is based on the BSD license adopted by the Apache Foundation. ");
        writer.println(" *  ");
        writer.println(" */");
        writer.println();
    }

    public static void main(String[] args) {
        try {
            if (args.length != 2) {
                System.err.println("# ConditionalBuild <buildConfig.properties> <rootDir>");
                System.err.println("#     <buildConfig.properties>    The configuration properties file.");
                System.err.println("#     <rootDir>                   The root directory which will contain the generated configuration files.");
                System.exit(1);
            }

            final String propertyFile = args[0];
            final String targetDir = args[1];

            ConditionalBuild conditionalBuild = new ConditionalBuild(targetDir);

            final File configProperties = new File(propertyFile);

            conditionalBuild.createFiles(configProperties);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
}
