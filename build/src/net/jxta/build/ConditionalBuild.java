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
    private static final int OFF = 0;
    private static final int ON = 1;
    private static final int RUNTIME = 2;
    private static final String types[] = { "off", "on", "runtime"}; // must be sorted !!

    private final File baseDir;
    private final Map<String, List<StaticField>> sourceFiles = new HashMap<String, List<StaticField>>();

    private class StaticField {
        final String packageName;
        final String className;
        final String fieldName;
        final int type;
        final String attr;

        StaticField(String packageName, String className, String fieldName, int type, String attr) {
            this.packageName = packageName;
            this.className = className;
            this.fieldName = fieldName;
            this.type = type;
            this.attr = attr;
        }
    }
	
    public ConditionalBuild(String baseDirName) throws IOException {
        baseDir = new File(baseDirName);
        System.err.println("Creating Conditional Build files in: " + baseDir);
    }

    public void createFiles(Properties properties) throws IOException {
        for (Enumeration e = properties.propertyNames(); e.hasMoreElements();) {
            String propertyName = (String) e.nextElement();

            parseProperty(propertyName, properties.getProperty(propertyName));
        }

        generateFiles();
    }

    private void generateFiles() throws IOException {
        for ( String filename : sourceFiles.keySet()) {          
            List<StaticField> staticFields = sourceFiles.get(filename);

            StaticField staticField = staticFields.get(0);

            StringBuffer srcDirName = new StringBuffer(staticField.packageName);

            for (;;) {
                int pos = -1;

                for (int i = 0; i < srcDirName.length(); i++) {
                    if (srcDirName.charAt(i) == '.') {
                        pos = i;
                        break;
                    }
                }
						
                if (pos < 0) {
                    break;
                }
                srcDirName.setCharAt(pos, File.separatorChar);
            }
			
            File srcDir = new File(baseDir, srcDirName.toString());

            srcDir.mkdirs();
            
            File file = new File(srcDir, filename + ".java");
            PrintWriter writer = new PrintWriter(new FileWriter(file));

            System.err.println("\tCreating Conditional Build file : " + file.getPath());

            insertCopyright(writer, staticField.className);

            writer.println("package " + staticField.packageName + ";");
            writer.println();			
            writer.println("import net.jxta.impl.meter.*;");
            writer.println();			
            writer.print("public interface " + staticField.className);

            if (!staticField.className.equals("MeterBuildSettings")) {
                writer.print(" extends MeterBuildSettings");
            }

            writer.println(" {");

            for (Iterator i = staticFields.iterator(); i.hasNext();) {
                staticField = (StaticField) i.next();

                String value = "false";

                if (staticField.type == ON) {
                    writer.println("    public static final boolean " + staticField.fieldName + " = true;");
                } else if (staticField.type == OFF) {
                    writer.println("    public static final boolean " + staticField.fieldName + " = false;");
                } else if (staticField.type == RUNTIME) {
                    String conditionalClassName = "Conditional" + staticField.className;

                    writer.println("    public static final boolean " + staticField.fieldName + " = " + conditionalClassName + ".isRuntimeMetering();");
                    makeConditionalFile(srcDir, staticField.packageName, conditionalClassName, staticField.attr);
                }
            }
			
            writer.println("}");

            writer.close();
        }
    }

    private void makeConditionalFile(File srcDir, String packageName, String className, String propertyName) throws IOException {

        File file = new File(srcDir, className + ".java");
        PrintWriter writer = new PrintWriter(new FileWriter(file));

        System.err.println("\tCreating Runtime Conditional Build file : " + file.getPath());

        insertCopyright(writer, className);

        writer.println("package " + packageName + ";");
        writer.println();			
        writer.println("import net.jxta.impl.meter.*;");
        writer.println("import java.util.ResourceBundle;");
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
        writer.println("\t\t} catch (Exception e) { }; ");
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

        String buildType = st.nextToken();
		
        int type = Arrays.binarySearch(types, buildType);
        String attr = st.nextToken();

        if (type < 0) { 
            System.out.println("Unknown Build Type: " + buildType + " found in property file (valid Types: on, off, runtime)");
        }

        StaticField staticField = new StaticField(packageName, className, fieldName, type, attr);

        List<StaticField> fields = sourceFiles.get(className);
		
        if (fields == null) {
            fields = new ArrayList<StaticField>();
            sourceFiles.put(className, fields);
        }

        fields.add(staticField);
    }
	
    public static void main(String args[]) {
        try {
            String propertyFile = (args.length < 1) ? "conditionalBuild.properties" : args[0];
            String targetDir = (args.length < 2) ? "d:/beam/tmp" : args[1];
				
            Properties properties = new Properties();

            properties.load(new FileInputStream(propertyFile));
            ConditionalBuild conditionalBuild = new ConditionalBuild(targetDir);

            conditionalBuild.createFiles(properties);
			
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void insertCopyright(PrintWriter writer, String className) {
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
        writer.println(" *     Project JXTA at http://www.jxta.org.");
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
        writer.println(" *  <http://www.jxta.org/project/www/license.html> for instructions on use of ");
        writer.println(" *  the license in source files.");
        writer.println(" *  ");
        writer.println(" *  ====================================================================");
        writer.println("");
        writer.println(" *  This software consists of voluntary contributions made by many individuals ");
        writer.println(" *  on behalf of Project JXTA. For more information on Project JXTA, please see ");
        writer.println(" *  http://www.jxta.org.");
        writer.println(" *  ");
        writer.println(" *  This license is based on the BSD license adopted by the Apache Foundation. ");
        writer.println(" *  ");
        writer.println(" */");
        writer.println();
    }
}
