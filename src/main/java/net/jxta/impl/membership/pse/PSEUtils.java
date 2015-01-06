/*
 * Copyright (c) 2001-2007 Sun Microsystems, Inc.  All rights reserved.
 *
 *  The Sun Project JXTA(TM) Software License
 *
 *  Redistribution and use in source and binary forms, with or without 
 *  modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice, 
 *     this list of conditions and the following disclaimer in the documentation 
 *     and/or other materials provided with the distribution.
 *
 *  3. The end-user documentation included with the redistribution, if any, must 
 *     include the following acknowledgment: "This product includes software 
 *     developed by Sun Microsystems, Inc. for JXTA(TM) technology." 
 *     Alternately, this acknowledgment may appear in the software itself, if 
 *     and wherever such third-party acknowledgments normally appear.
 *
 *  4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA" must 
 *     not be used to endorse or promote products derived from this software 
 *     without prior written permission. For written permission, please contact 
 *     Project JXTA at http://www.jxta.org.
 *
 *  5. Products derived from this software may not be called "JXTA", nor may 
 *     "JXTA" appear in their name, without prior written permission of Sun.
 *
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 *  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND 
 *  FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL SUN 
 *  MICROSYSTEMS OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 *  OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF 
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 *  EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *  JXTA is a registered trademark of Sun Microsystems, Inc. in the United 
 *  States and other countries.
 *
 *  Please see the license information page at :
 *  <http://www.jxta.org/project/www/license.html> for instructions on use of 
 *  the license in source files.
 *
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many individuals 
 *  on behalf of Project JXTA. For more information on Project JXTA, please see 
 *  http://www.jxta.org.
 *
 *  This license is based on the BSD license adopted by the Apache Foundation. 
 */

package net.jxta.impl.membership.pse;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.security.auth.x500.X500Principal;

import net.jxta.document.Attribute;
import net.jxta.document.XMLElement;
import net.jxta.impl.util.BASE64InputStream;
import net.jxta.impl.util.BASE64OutputStream;
import net.jxta.logging.Logger;
import net.jxta.logging.Logging;

import org.spongycastle.asn1.DERObjectIdentifier;
import org.spongycastle.asn1.x509.X509NameTokenizer;
import org.spongycastle.jce.X509Principal;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.x509.X509V3CertificateGenerator;

/**
 * Singleton class of static utility methods.
 */
public final class PSEUtils {

    private static final transient Logger LOG = Logging.getLogger(PSEUtils.class.getName());

    /**
     * Singleton instance.
     */
    private static final PSEUtils UTILS = new PSEUtils();

    /**
     * A SecureRandom for generating keys.
     */
    final transient SecureRandom srng = new SecureRandom();

    /**
    * The name of the symmetric cipher to use.
    */
    public final static String symmetricAlgorithm = "DESede";

    /**
     * Singleton utility class
     */
    private PSEUtils() {

        try {
            //<DC desc="JNLP does not work on this>
//            ClassLoader sysloader = ClassLoader.getSystemClassLoader();
//
//            Class<?> loaded = sysloader.loadClass(BouncyCastleProvider.class.getName());
//
//            Provider provider = (Provider) loaded.newInstance();
            //</DC>
            //<DC desc="Instantiate it directly so that the JNLP classloader will load it.">
            Provider provider = new BouncyCastleProvider();

            Security.addProvider(provider);

            Logging.logCheckedInfo(LOG, "Loaded Security Providers into system class loader");

        } catch (Exception disallowed) {

            Logging.logCheckedWarning(LOG, "Failed loading Security Providers into System Class Loader. Will try local class loader (which may not work)\n",
                disallowed);

            // Add the providers we use.
            Security.addProvider(new BouncyCastleProvider());

            Logging.logCheckedInfo(LOG, "Loaded Security Providers into local class loader");

        }

        // Provider [] providers = Security.getProviders();
        // Iterator eachProvider = Arrays.asList(providers).iterator();
        //
        // while (eachProvider.hasNext()) {
        // Provider aProvider = (Provider) eachProvider.next();
        //
        // System.out.println("\n\n" + aProvider.getName() + " - " + aProvider.getVersion() + " - " + aProvider.getInfo());
        //
        // Iterator allMappings = aProvider.entrySet().iterator();
        //
        // while (allMappings.hasNext()) {
        // Map.Entry aMapping = (Map.Entry) allMappings.next();
        //
        // Object key = aMapping.getKey();
        // System.out.println(key + " (" + key.getClass().getName() + ") --> " + aMapping.getValue() + " (" + key.getClass().getName() + ")");
        // }
        // }
    }

    /**
     * Issuer Information
     */
    public static class IssuerInfo {
        public X509Certificate cert; // subject Cert
        public PrivateKey subjectPkey; // subject private key
        public X509Certificate issuer; // issuer Cert
        public PrivateKey issuerPkey; // issuer private key
    }

    /**
     * Generate a Cert
     *
     * @param cn         subject cn for the certificate
     * @param issuerinfo the cert issuer or null if self-signed root cert.
     * @return the details of the generated cert.
     * @throws SecurityException if the cert could not be generated.
     */
    public static IssuerInfo genCert(String cn, IssuerInfo issuerinfo) throws SecurityException {

        try {

            String useCN;

            if (null == issuerinfo) {

                Logging.logCheckedDebug(LOG, "Generating Self Signed Cert ...");

                if (!cn.endsWith("-CA")) {
                    useCN = cn + "-CA";
                } else {
                    useCN = cn;
                }

            } else {

                Logging.logCheckedDebug(LOG, "Generating Client Cert ...");
                useCN = cn;

            }

            // set name attribute
            Hashtable<DERObjectIdentifier, String> attrs = new Hashtable<DERObjectIdentifier, String>();

            attrs.put(X509Principal.CN, useCN);
            attrs.put(X509Principal.O, "www.jxta.org");

            // XXX bondolo 20040405 wouldn't SN or UID be a better choice?
            // set ou to 20 random digits
            byte[] ou = new byte[10];

            UTILS.srng.nextBytes(ou);
            String ouStr = toHexDigits(ou);

            attrs.put(X509Principal.OU, ouStr);

            X509Principal subject = new X509Principal(attrs);
            X500Principal samesubject = new X500Principal(subject.getEncoded());
            KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");

            g.initialize(1024, UTILS.srng);

            KeyPair keypair = g.generateKeyPair();

            return genCert(samesubject, keypair, issuerinfo);

        } catch (NoSuchAlgorithmException e) {

            Logging.logCheckedError(LOG, "Could not generate certificate\n\n", e);

            SecurityException failure = new SecurityException("Could not generate certificate");
            failure.initCause(e);
            throw failure;

        }
    }

    /**
     * Generate a Cert given a keypair
     *
     * @param subject    subjectDN for the certificate
     * @param keypair    the keypair to use.
     * @param issuerinfo the cert issuer or null if self-signed root cert.
     * @return the details of the generated cert.
     * @throws SecurityException if the cert could not be generated.
     */
    public static IssuerInfo genCert(X500Principal subject, KeyPair keypair, IssuerInfo issuerinfo) throws SecurityException {
        try {
            // set up issuer
            PrivateKey signer;
            X509Principal issuer;

            if (null == issuerinfo) { // self-signed root cert
                signer = keypair.getPrivate();
                issuer = new X509Principal(subject.getEncoded());
            } else { // issuer signed service sert
                signer = issuerinfo.subjectPkey;
                X500Principal issuer_subject = issuerinfo.cert.getSubjectX500Principal();

                issuer = new X509Principal(issuer_subject.getEncoded());
            }

            // set validity 10 years from today
            Date today = new Date();
            Calendar cal = Calendar.getInstance();

            cal.setTime(today);
            cal.add(Calendar.YEAR, 10);
            Date until = cal.getTime();

            // generate cert
            X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();

            certGen.setIssuerDN(issuer);
            certGen.setSubjectDN(new X509Principal(subject.getEncoded()));
            certGen.setNotBefore(today);
            certGen.setNotAfter(until);
            certGen.setPublicKey(keypair.getPublic());
            // certGen.setSignatureAlgorithm("SHA1withDSA");
            certGen.setSignatureAlgorithm("SHA1WITHRSA");
            // FIXME bondolo 20040317 needs fixing.
            certGen.setSerialNumber(BigInteger.valueOf(1));

            // return issuer info for generating service cert
            IssuerInfo info = new IssuerInfo();

            // the cert
            info.cert = certGen.generateX509Certificate(signer, UTILS.srng);

            // For saving service cert private key
            info.subjectPkey = keypair.getPrivate();

            // for signing service cert
            info.issuer = (null == issuerinfo) ? info.cert : issuerinfo.cert;

            // for signing service cert
            info.issuerPkey = signer;

            // dump the certificate?
            if (null == info.issuer) {
                Logging.logCheckedDebug(LOG, "Root Cert : \n", info.cert);
            } else {
                Logging.logCheckedDebug(LOG, "Client Cert : \n", info.cert);
            }

            return info;

        } catch (SignatureException e) {

            Logging.logCheckedError(LOG, "Could not generate certificate\n\n", e);

            SecurityException failure = new SecurityException("Could not generate certificate");
            failure.initCause(e);
            throw failure;

        } catch (InvalidKeyException e) {

            Logging.logCheckedError(LOG, "Could not generate certificate\n\n", e);

            SecurityException failure = new SecurityException("Could not generate certificate");
            failure.initCause(e);
            throw failure;

        } catch (IOException e) {

            Logging.logCheckedError(LOG, "Could not generate certificate\n\n", e);

            SecurityException failure = new SecurityException("Could not generate certificate");
            failure.initCause(e);
            throw failure;

        }
    }

    /**
     * return the CN token from the provided cert's subjectDN
     *
     * @param cert the certificate to examine
     * @return the CN name or null if none could be found.
     */
    public static String getCertSubjectCName(X509Certificate cert) {

        // get the subject dname
        X500Principal subject = cert.getSubjectX500Principal();

        X509NameTokenizer tokens = new X509NameTokenizer(subject.getName());

        // iterate over the attributes of the dname
        while (tokens.hasMoreTokens()) {
            String aToken = tokens.nextToken();

            if (aToken.length() < 3) {
                continue;
            }

            String attribute = aToken.substring(0, 3);

            if ("CN=".equalsIgnoreCase(attribute)) {
                return aToken.substring(3);
            }
        }

        return null;
    }

    /**
     * return the CN token from the provided cert's issuerDN
     *
     * @param cert the certificate to examine
     * @return the CN name or null if none could be found.
     */
    public static String getCertIssuerCName(X509Certificate cert) {

        // get the subject dname
        X500Principal issuer = cert.getIssuerX500Principal();

        X509NameTokenizer tokens = new X509NameTokenizer(issuer.getName());

        // iterate over the attributes of the dname
        while (tokens.hasMoreTokens()) {
            String aToken = tokens.nextToken();

            if (aToken.length() < 3) {
                continue;
            }

            String attribute = aToken.substring(0, 3);

            if ("CN=".equalsIgnoreCase(attribute)) {
                return aToken.substring(3);
            }
        }

        return null;
    }

    /**
     * Compute the signature of a stream.
     *
     * @param key    the private key used to sign the stream
     * @param stream the stream to sign.
     * @return byte[] the signature
     */
    public static byte[] computeSignature(String algorithm, PrivateKey key, InputStream stream) throws InvalidKeyException, SignatureException, IOException {
        Signature sign;

        try {
            sign = Signature.getInstance(algorithm);
        } catch (NoSuchAlgorithmException badsigner) {
            throw new IOException("Could not initialize signer with algorithm " + algorithm);
        }
        sign.initSign(key, UTILS.srng);

        byte[] buffer = new byte[1024];

        while (true) {
            int read = stream.read(buffer);

            if (read < 0) {
                break;
            }

            sign.update(buffer, 0, read);
        }

        return sign.sign();
    }

    /**
     * Verify a signature of a stream.
     *
     * @param cert      The certificate containing the public key which will be used
     *                  to verify the signature.
     * @param signature The signature to verify.
     * @param stream    The stream to verify.
     * @return boolean true if the signature was valid otherwise false.
     */
    public static boolean verifySignature(String algorithm, Certificate cert, byte[] signature, InputStream stream) throws InvalidKeyException, SignatureException, IOException {
        Signature sign;

        try {
            sign = Signature.getInstance(algorithm);
        } catch (NoSuchAlgorithmException badsigner) {
            throw new IOException("Could not initialize signer with algorithm " + algorithm);
        }

        sign.initVerify(cert);

        byte[] buffer = new byte[1024];

        while (true) {
            int read = stream.read(buffer);

            if (read < 0) {
                break;
            }

            sign.update(buffer, 0, read);
        }

        return sign.verify(signature);
    }

    /**
     * returns a hash SHA-1 of the given byte array
     *
     * @param data the data to be hashed
     * @return byte[] the hash of the data
     */
    public static byte[] hash(String algorithm, byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);

            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    /**
     * We are trying to use : PBEWITHMD5ANDDES
     */
    static final String PKCS5_PBSE1_ALGO = "PBEWITHMD5ANDDES";

    /**
     * Given a private key and a password, encrypt the private key using the
     * PBESE1 algorithm.
     *
     * @param password   The password which will be used.
     * @param privkey    The private key to be encrypted.
     * @param iterations Number of iterations.
     * @return An encrypted private key info or null if the key could not be
     *         encrypted.
     */
    public static EncryptedPrivateKeyInfo pkcs5_Encrypt_pbePrivateKey(char[] password, PrivateKey privkey, int iterations) {

        PBEKeySpec pbeKeySpec = new PBEKeySpec(password);
        byte[] salt = new byte[8];

        UTILS.srng.nextBytes(salt);

        try {
            PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, iterations);

            // convert password into a SecretKey object, using a PBE key factory.
            SecretKeyFactory keyFac = SecretKeyFactory.getInstance(PKCS5_PBSE1_ALGO);
            SecretKey pbeKey = keyFac.generateSecret(pbeKeySpec);

            // Create PBE Cipher
            Cipher pbeCipher = Cipher.getInstance(PKCS5_PBSE1_ALGO);

            // Initialize PBE Cipher with key and parameters
            pbeCipher.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParamSpec);

            byte[] encryptedPrivKey = pbeCipher.doFinal(privkey.getEncoded());

            AlgorithmParameters algo = AlgorithmParameters.getInstance(PKCS5_PBSE1_ALGO);

            algo.init(pbeParamSpec);

            EncryptedPrivateKeyInfo result = new EncryptedPrivateKeyInfo(algo, encryptedPrivKey);

            return result;

        } catch (Exception failed) {

            Logging.logCheckedWarning(LOG, "Encrypt failed\n", failed);
            return null;

        }
    }

    /**
     * Given an encrypted private key and a password, decrypt the private key
     * using the PBESE1 algorithm.
     *
     * @param password         The password which will be used.
     * @param encryptedPrivKey The private key to be encrypted.
     * @return The decrypted private key or null if the key could not be decrpyted.
     */
    public static PrivateKey pkcs5_Decrypt_pbePrivateKey(char[] password, String algorithm, EncryptedPrivateKeyInfo encryptedPrivKey) {

        PBEKeySpec pbeKeySpec = new PBEKeySpec(password);

        try {

            AlgorithmParameters algo = encryptedPrivKey.getAlgParameters();

            if (null == algo) {

                Logging.logCheckedWarning(LOG, "Could not get algo parameters from ", encryptedPrivKey);
                throw new IllegalStateException("Could not get algo parameters from " + encryptedPrivKey);

            }

            PBEParameterSpec pbeParamSpec = algo.getParameterSpec(PBEParameterSpec.class);

            // convert password into a SecretKey object, using a PBE key factory.
            try {

                SecretKeyFactory keyFac = SecretKeyFactory.getInstance(PKCS5_PBSE1_ALGO);
                SecretKey pbeKey = keyFac.generateSecret(pbeKeySpec);

                // Create PBE Cipher
                Cipher pbeCipher = Cipher.getInstance(PKCS5_PBSE1_ALGO);

                // Initialize PBE Cipher with key and parameters
                pbeCipher.init(Cipher.DECRYPT_MODE, pbeKey, pbeParamSpec);

                KeySpec key_spec;

                key_spec = encryptedPrivKey.getKeySpec(pbeCipher);

                KeyFactory kf = KeyFactory.getInstance(algorithm);

                return kf.generatePrivate(key_spec);

            } catch (InvalidKeySpecException failed) {

                Logging.logCheckedWarning(LOG, "Incorrect key for ", encryptedPrivKey, " : \n", failed);
                return null;

            }
        } catch (Exception failed) {

            Logging.logCheckedWarning(LOG, "Decrypt failed\n", failed);
            return null;

        }
    }

    // Load a wrapped object in base64 format:
    // The following three methods were modified
    // from similar pureTLS methods.
    /**
     * WrappedObject.java
     * <p/>
     * Copyright (C) 1999, Claymore Systems, Inc.
     * All Rights Reserved.
     * <p/>
     * ekr@rtfm.com  Fri Jun  4 09:11:27 1999
     * <p/>
     * This package is a SSLv3/TLS implementation written by Eric Rescorla
     * <ekr@rtfm.com> and licensed by Claymore Systems, Inc.
     * <p/>
     * Redistribution and use in source and binary forms, with or without
     * modification, are permitted provided that the following conditions
     * are met:
     * 1. Redistributions of source code must retain the above copyright
     * notice, this list of conditions and the following disclaimer.
     * 2. Redistributions in binary form must reproduce the above copyright
     * notice, this list of conditions and the following disclaimer in the
     * documentation and/or other materials provided with the distribution.
     * 3. All advertising materials mentioning features or use of this software
     * must display the following acknowledgement:
     * This product includes software developed by Claymore Systems, Inc.
     * 4. Neither the name of Claymore Systems, Inc. nor the name of Eric
     * Rescorla may be used to endorse or promote products derived from this
     * software without specific prior written permission.
     * <p/>
     * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
     * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
     * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
     * ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
     * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
     * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
     * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
     * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
     * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
     * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
     * SUCH DAMAGE.
     */

    public static String loadBase64Object(BufferedReader rdr, String type) throws IOException {
        if (null != findObject(rdr, type)) {
            return readBase64Object(rdr, type);
        } else {
            return null;
        }
    }

    public static byte[] loadObject(BufferedReader rdr, String type) throws IOException {
        if (null != findObject(rdr, type)) {
            return readObject(rdr, type);
        } else {
            return null;
        }
    }

    public static String findObject(BufferedReader br, String type) throws IOException {
        String prefix = "-----BEGIN ";
        String suffix = (type == null) ? "-----" : type + "-----";

        while (true) {
            br.mark(1024);

            String line = br.readLine();

            if (null == line) {
                return null;
            }

            if (!line.startsWith(prefix)) {
                continue;
            }

            if (!line.endsWith(suffix)) {
                continue;
            }

            br.reset();

            return line.substring(prefix.length(), line.length() - 5);
        }
    }

    /**
     * We read a block of n-lines (\n terminated) and return a String of n-lines
     * concatenated together. This keeps the format consistent with the pureTLS
     * requirements.
     */
    public static String readBase64Object(BufferedReader br, String type) throws IOException {
        String line = br.readLine();

        String prefix = "-----BEGIN ";
        String suffix = (type == null) ? "-----" : type + "-----";

        if (!line.startsWith(prefix) || !line.endsWith(suffix)) {
            throw new IOException("Not at begining of object");
        }

        StringBuilder block = new StringBuilder();

        while (true) {
            line = br.readLine();

            if (null == line) {
                break;
            }

            if (line.startsWith("-----END ")) {
                break;
            }

            block.append(line);
            block.append('\n');
        }

        return block.toString();
    }

    /**
     * Read an object
     */
    public static byte[] readObject(BufferedReader br, String type) throws IOException {
        String base64 = readBase64Object(br, type);

        return base64Decode(new StringReader(base64));
    }

    /**
     *
     */

    /**
     * Write an object that is already base64 encoded.
     */
    public static void writeBase64Object(BufferedWriter bw, String type, String object) throws IOException {

        bw.write("-----BEGIN ");
        bw.write(type);
        bw.write("-----");
        bw.newLine();

        bw.write(object);

        char lastChar = object.charAt(object.length() - 1);

        if (('\n' != lastChar) && ('\r' != lastChar)) {
            bw.newLine();
        }

        bw.write("-----END ");
        bw.write(type);
        bw.write("-----");
        bw.newLine();

        bw.flush();
    }

    public static void writeObject(BufferedWriter out, String type, byte[] object) throws IOException {
        String base64 = base64Encode(object);

        writeBase64Object(out, type, base64);
    }

    /**
     * Convert a byte array into a BASE64 encoded String.
     *
     * @param in The bytes to be converted
     * @return the BASE64 encoded String.
     */
    public static String base64Encode(byte[] in) throws IOException {
        return base64Encode(in, true);
    }

    /**
     * Convert a byte array into a BASE64 encoded String.
     *
     * @param in the bytes to be converted
     * @return the BASE64 encoded String.
     */
    public static String base64Encode(byte[] in, boolean wrap) throws IOException {
        StringWriter base64 = new StringWriter();

        BASE64OutputStream b64os = null;
        try{
        	if (wrap) {
        		b64os = new BASE64OutputStream(base64, 72);
        	} else {
        		b64os = new BASE64OutputStream(base64);
        	}
        	b64os.write(in);
        }
        finally{
        	if( b64os != null )
        		b64os.close();
        }

        String encoded = base64.toString();

        // LOGGING: was Finer
        Logging.logCheckedDebug(LOG, "Encoded ", in.length, " bytes -> ", encoded.length(), " characters.");

        return encoded;
    }

    /**
     * Convert a BASE64 Encoded String into byte array.
     *
     * @param in BASE64 encoded String
     * @return the decoded bytes.
     */
    public static byte[] base64Decode(Reader in) throws IOException {
        BASE64InputStream b64is = new BASE64InputStream(in);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try{
        do {
            int c = b64is.read();

            if (c < 0) {
                break;
            }

            bos.write(c);
        } while (true);
        }
        finally{
        	b64is.close();
        }

        byte[] result = bos.toByteArray();

        // LOGGING: was Finer
        Logging.logCheckedDebug(LOG, "Decoded ", result.length, " bytes.");

        return result;
    }

    /**
     * Private replacement for toHexString since we need the leading 0 digits.
     * Returns a String containing byte value encoded as 2 hex characters.
     *
     * @param theByte a byte containing the value to be encoded.
     * @return String containing byte value encoded as 2 hex characters.
     */
    private static String toHexDigits(byte theByte) {
        final char[] HEXDIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        StringBuilder result = new StringBuilder(2);

        result.append(HEXDIGITS[(theByte >>> 4) & 15]);
        result.append(HEXDIGITS[theByte & 15]);

        return result.toString();
    }

    public static String toHexDigits(byte[] bytes) {
        StringBuilder encoded = new StringBuilder(bytes.length * 2);

        // build the string.
        for (byte aByte : bytes) {
            encoded.append(toHexDigits(aByte).toUpperCase());
        }
        return encoded.toString();
    }
    
    /**
     * Traverses a XmlElement and applies it to a MessageDigest
     *
     * @param xmlElement   The xmlElement to be traversed.
     * @param ignoreXmlElementName   Ignore the top level child with this name.
     * @param messageDigest   The messageDigest to which .
     * @return An encrypted private key info or null if the key could not be
     */
    public static void xmlElementDigest(XMLElement<?> xmlElement, List<String> ignoreXmlElementNames, MessageDigest messageDigest) {
        PSEUtils.writeStringToDigest(xmlElement.getName(), messageDigest);
        Enumeration<Attribute> attributes = xmlElement.getAttributes();
        while(attributes.hasMoreElements()) {
            Attribute attribute = (Attribute)attributes.nextElement();
            PSEUtils.writeStringToDigest(attribute.getName(), messageDigest);
            PSEUtils.writeStringToDigest(attribute.getValue(), messageDigest);
        }
        PSEUtils.writeStringToDigest(xmlElement.getValue(), messageDigest);
        Enumeration<?> children = xmlElement.getChildren();
        while(children.hasMoreElements()) {
            XMLElement<?> xmlElementChild = (XMLElement<?>)children.nextElement();
            if (ignoreXmlElementNames.contains(xmlElementChild.getName()))
                continue;
            PSEUtils.xmlElementDigest(xmlElementChild, messageDigest);
        }
    }
    /**
     * Traverses a XmlElement and applies it to a MessageDigest
     *
     * @param xmlElement   The xmlElement to be traversed.
     * @param messageDigest   The messageDigest to which .
     * @return An encrypted private key info or null if the key could not be
     */
    public static void xmlElementDigest(XMLElement<?> xmlElement, MessageDigest messageDigest) {
        PSEUtils.writeStringToDigest(xmlElement.getName(), messageDigest);
        Enumeration<Attribute> attributes = xmlElement.getAttributes();
        while(attributes.hasMoreElements()) {
            Attribute attribute = (Attribute)attributes.nextElement();
            PSEUtils.writeStringToDigest(attribute.getName(), messageDigest);
            PSEUtils.writeStringToDigest(attribute.getValue(), messageDigest);
        }
        PSEUtils.writeStringToDigest(xmlElement.getValue(), messageDigest);
        Enumeration<?> children = xmlElement.getChildren();
        while(children.hasMoreElements()) {
            XMLElement<?> xmlElementChild = (XMLElement<?>)children.nextElement();
            PSEUtils.xmlElementDigest(xmlElementChild, messageDigest);
        }
    }
/*
 * Copyright  2009 The Apache Software Foundation.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * UtfHelpper
 *
 */
    final public static void writeStringToDigest(final String str,final MessageDigest messageDigest) {
            final int length=str.length();
            int i=0;
        char c;
            while (i<length) {
                    c=str.charAt(i++);
            if (c < 0x80)  {
                messageDigest.update((byte)c);
                continue;
            }
            if ((c >= 0xD800 && c <= 0xDBFF) || (c >= 0xDC00 && c <= 0xDFFF) ){
                    //No Surrogates in sun java
                    messageDigest.update((byte)0x3f);
                    continue;
            }
            char ch;
            int bias;
            int write;
            if (c > 0x07FF) {
                ch=(char)(c>>>12);
                write=0xE0;
                if (ch>0) {
                    write |= ( ch & 0x0F);
                }
                messageDigest.update((byte)write);
                write=0x80;
                bias=0x3F;
            } else {
                    write=0xC0;
                    bias=0x1F;
            }
            ch=(char)(c>>>6);
            if (ch>0) {
                 write|= (ch & bias);
            }
            messageDigest.update((byte)write);
            messageDigest.update((byte)(0x80 | ((c) & 0x3F)));

            }

       }

    /**
     * Compares two byte arrays
     * @param a1
     * @param a2
     * @return
     */
    public static boolean arrayCompare(byte[] a1, byte[] a2) {
        if (a1.length != a2.length)
            return false;
        for (int i=0;i<a1.length;i++)
            if (a1[i]!=a2[i])
                return false;
        return true;
    }

    /**
     * Encrypts byte array
     *
     * @param data decrypted data array
     * @param data inputOffset data array offset
     * @param data inputLen data array length
     * @param cipher cipher
     * @param key public key
     * @return encrypted data
     * @throws IOException
     */
    public static final byte[] encryptAsymmetric(byte[] data, int inputOffset, int inputLen, Cipher cipher, PublicKey key) throws IOException {
        synchronized (cipher) {
            try {
                cipher.init(Cipher.ENCRYPT_MODE, key);
                return cipher.doFinal(data, inputOffset, inputLen);
            } catch (Exception ex) {
                throw new IOException("Failed encrypting stream:", ex);
            }
        }
    }

    /**
     * Decrypts byte array
     *
     * @param data encrypted data
     * @param cipher cipher
     * @param key private key
     * @return decrypted data
     * @throws IOException
     */
    public static byte[] decryptAsymmetric(byte[] data, Cipher cipher, PrivateKey key) throws IOException {
        synchronized (cipher) {
            try {
                cipher.init(Cipher.DECRYPT_MODE, key);
                return cipher.doFinal(data);
            } catch (Exception ex) {
                throw new IOException("Failed decrypting stream:", ex);
            }
        }
    }

    /**
     * Encrypts byte array
     *
     * @param data decrypted data array
     * @param data inputOffset data array offset
     * @param data inputLen data array length
     * @param cipher cipher
     * @param key public key
     * @return encrypted data
     * @throws IOException
     */
    public static final byte[] encryptSymmetric(byte[] data, int inputOffset, int inputLen, Cipher cipher, SecretKey key) throws IOException {
        synchronized (cipher) {
            try {
                cipher.init(Cipher.ENCRYPT_MODE, key);
                return cipher.doFinal(data, inputOffset, inputLen);
            } catch (Exception ex) {
                throw new IOException("Failed encrypting stream:", ex);
            }
        }
    }

    /**
     * Decrypts byte array
     *
     * @param data encrypted data
     * @param cipher cipher
     * @param key private key
     * @return decrypted data
     * @throws IOException
     */
    public static byte[] decryptSymmetric(byte[] data, Cipher cipher, SecretKey key) throws IOException {
        synchronized (cipher) {
            try {
                cipher.init(Cipher.DECRYPT_MODE, key);
                return cipher.doFinal(data);
            } catch (Exception ex) {
                throw new IOException("Failed decrypting stream:", ex);
            }
        }
    }

    /**
    * Generate a symmetric DES3 key.
    *
    * @return A DES3 key
    */
    public static SecretKey generateSymmetricKey() throws NoSuchAlgorithmException {
        KeyGenerator symmetricKeyGenerator = KeyGenerator.getInstance(symmetricAlgorithm);
        return symmetricKeyGenerator.generateKey();
    }

    /**
     * Creates an encoded DESedeKeySpec from a SecretKey
     * 
     * @param key
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    public static byte[] createDESedeKeySpec(SecretKey key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory keyfactory = SecretKeyFactory.getInstance("DESede");
        DESedeKeySpec keyspec = (DESedeKeySpec) keyfactory.getKeySpec(key,
        DESedeKeySpec.class);
        return keyspec.getKey();
    }

    /**
     *
     * Creates a SecretKey from an encoded DESedeKeySpec
     * 
     * @param rawkey
     * @return
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    public static SecretKey createSecretKey(byte[] rawkey) throws InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException {
        DESedeKeySpec keyspec = new DESedeKeySpec(rawkey);
        SecretKeyFactory keyfactory = SecretKeyFactory.getInstance("DESede");
        return keyfactory.generateSecret(keyspec);
    }
}
