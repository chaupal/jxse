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

package net.jxta.endpoint;

import net.jxta.document.MimeMediaType;
import net.jxta.logging.Logging;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.logging.Logger;

/**
 * A Message Element using character sequences for the element data.
 */
public class StringMessageElement extends TextMessageElement {

    /**
     * Logger
     */
    private final static transient Logger LOG = Logger.getLogger(StringMessageElement.class.getName());
    /**
     * The MIME media type we will be use for encoding {@code String}s when no
     * encoding is specified.
     */
    private static final MimeMediaType DEFAULT_TEXT_ENCODING = new MimeMediaType(MimeMediaType.TEXT_DEFAULTENCODING, "charset=\"" + Charset.defaultCharset().name() + "\"", true).intern();
    /**
     * The data for this Message Element.
     */
    protected final CharSequence data;

    /**
     * Returns an appropriate mime type for the given encoding name. The
     * mimetype will contain the canonical name of the encoding.
     *
     * @param encoding name of the desired encoding.
     * @return the mime type.
     * @throws java.io.UnsupportedEncodingException
     *          if the mime is unsupported
     */
    private static MimeMediaType makeMimeType(String encoding) throws UnsupportedEncodingException {
        final InputStreamReader getEncoding = new InputStreamReader(new ByteArrayInputStream(new byte[0]), encoding);

        final String canonicalName = getEncoding.getEncoding();

        return new MimeMediaType(MimeMediaType.TEXT_DEFAULTENCODING, "charset=\"" + canonicalName + "\"", true).intern();
    }

    /**
     * Create a new Message Element from the provided String. The String will
     * be encoded for transmission using UTF-8.
     *
     * @param name  Name of the Element. May be the empty string ("") or null if
     *              the Element is not named.
     * @param value A String containing the contents of this element.
     * @param sig   Message digest/digital signature element. If no signature is
     *              to be specified, pass <code>null</code>.
     * @throws IllegalArgumentException if <code>value</code> is
     *                                  <code>null</code>.
     */
    public StringMessageElement(String name, CharSequence value, MessageElement sig) {
        super(name, MimeMediaType.TEXTUTF8, sig);

        if (null == value) {
            throw new IllegalArgumentException("value must be non-null");
        }

        data = value;
    }

    /**
     * Create a new Message Element from the provided String. The string will
     * be encoded for transmission using specified character encoding.
     *
     * @param name     Name of the MessageElement. May be the empty string ("") or
     *                 <code>null</code> if the MessageElement is not named.
     * @param value    A String containing the contents of this element.
     * @param encoding Name of the character encoding to use. If
     *                 <code>null</code> then the system default character encoding will be
     *                 used. (Using the system default character encoding should be used with
     *                 extreme caution).
     * @param sig      Message digest/digital signature element. If no signature is
     *                 to be specified, pass <code>null</code>.
     * @throws IllegalArgumentException     if <code>value</code> is
     *                                      <code>null</code>.
     * @throws UnsupportedEncodingException if the requested encoding is not
     *                                      supported.
     */
    public StringMessageElement(String name, CharSequence value, String encoding, MessageElement sig) throws UnsupportedEncodingException {
        super(name, (null == encoding) ? DEFAULT_TEXT_ENCODING : makeMimeType(encoding), sig);

        if (null == value) {
            throw new IllegalArgumentException("value must be non-null");
        }

        data = value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof MessageElement) {
            if (!super.equals(obj)) {
                return false;               
            }

            if (obj instanceof StringMessageElement) {
                final StringMessageElement likeMe = (StringMessageElement) obj;

                return data.toString().equals(likeMe.data.toString()); // same chars?
            } else if (obj instanceof TextMessageElement) {
                // have to do a slow char by char comparison. Still better than the stream since it saves encoding.
                // XXX 20020615 bondolo@jxta.org the performance of this could be much improved.
                final TextMessageElement likeMe = (TextMessageElement) obj;

                try {
                    final Reader myReader = getReader();
                    final Reader itsReader = likeMe.getReader();

                    int mine;
                    int its;

                    do {
                        mine = myReader.read();
                        its = itsReader.read();

                        if (mine != its) {
                            // content didn't match
                            return false;
                        }       

                    } while ((-1 != mine) && (-1 != its));

                    return ((-1 == mine) && (-1 == its)); // end at the same time?
                } catch (IOException fatal) {
                    final IllegalStateException failure = new IllegalStateException("MessageElements could not be compared.");

                    failure.initCause(fatal);
                    throw failure;
                }
            } else {
                // have to do a slow stream comparison.
                // XXX 20020615 bondolo the performance of this could be much improved.
                final MessageElement likeMe = (MessageElement) obj;

                try {
                    final InputStream myStream = getStream();
                    final InputStream itsStream = likeMe.getStream();

                    int mine;
                    int its;

                    do {
                        mine = myStream.read();
                        its = itsStream.read();

                        if (mine != its) {
                            // content didn't match
                            return false;
                        }       

                    } while ((-1 != mine) && (-1 != its));

                    return ((-1 == mine) && (-1 == its)); // end at the same time?
                } catch (IOException fatal) {
                    final IllegalStateException failure = new IllegalStateException("MessageElements could not be compared.");
                    failure.initCause(fatal);
                    throw failure;
                }
            }
        }

        return false; // not a new message element
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int result = super.hashCode() * 6037 + // a prime
                data.toString().hashCode();

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return data.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized byte[] getBytes(boolean copy) {
        byte[] cachedBytes = null;

        if (null != cachedGetBytes) {
            cachedBytes = cachedGetBytes.get();                 
        }

        if (null == cachedBytes) {

            Logging.logCheckedFiner(LOG, "Creating getBytes of ", getClass().getName(), '@', Integer.toHexString(System.identityHashCode(this)));

            final String charset = type.getParameter("charset");

            try {
                // implementation note : As of 1.6.0_07 this impl results in one 
                // less copy of the data then data.toString().getBytes(charset)
                final Charset characterset = Charset.forName(charset);
                final CharsetEncoder encoder = characterset.newEncoder();
                final ByteArrayOutputStream bos = new ByteArrayOutputStream((int) ((double) encoder.maxBytesPerChar() * data.length()));
                sendToStream(bos);
                bos.close();
                cachedBytes = bos.toByteArray();

            } catch (IOException caught) {

                Logging.logCheckedWarning(LOG, "MessageElement Data could not be generated\n", caught);
                final IllegalStateException failure = new IllegalStateException("MessageElement Data could not be generated");
                failure.initCause(caught);
                throw failure;

            }

            cachedGetBytes = new SoftReference<byte[]>(cachedBytes);
        }

        if (!copy) {
            return cachedBytes;
        }

        final byte[] bytesCopy = cachedBytes.clone();

        return bytesCopy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCharLength() {
        return data.length();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized char[] getChars(boolean copy) {
        char[] cachedChars = null;

        if (null != cachedGetChars) {
            cachedChars = cachedGetChars.get();
        }

        if (null == cachedChars) {

            Logging.logCheckedFiner(LOG, "creating cachedGetChars of ", getClass().getName(), '@', Integer.toHexString(hashCode()));

            if ((data instanceof CharBuffer) && ((CharBuffer) data).hasArray()) {
                cachedChars = ((CharBuffer) data).array();
            } else {
                cachedChars = data.toString().toCharArray();
            }

            // if this is supposed to be a shared buffer then we can cache it.
            cachedGetChars = new SoftReference<char[]>(cachedChars);

        }

        if (!copy) {
            return cachedChars;
        }

        return cachedChars.clone();

    }

    /**
     * {@inheritDoc}
     */
    public InputStream getStream() throws IOException {
        byte cachedBytes[] = null;

        synchronized (this) {
            if (null != cachedGetBytes) {
                cachedBytes = cachedGetBytes.get();
            }
        }

        if (null != cachedBytes) {
            return new ByteArrayInputStream(cachedBytes);
        } else {
            final String charset = type.getParameter("charset");
            return new CharBufferInputStream(data, charset);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return InputStream of the stream containing element data.
     * @throws IOException when there is a problem getting a reader.
     */
    public Reader getReader() throws IOException {

        return new CharArrayReader(getChars(false));
    }

    @Override
    public void sendToStream(OutputStream sendTo) throws IOException {
        final Writer osw = new OutputStreamWriter(sendTo, type.getParameter("charset"));

        sendToWriter(osw);
        osw.flush();
    }

    @Override
    public void sendToWriter(Writer sendTo) throws IOException {
        sendTo.append(data);
    }

    /**
     * Presents a {@link java.nio.CharBuffer} as an {@link java.io.InputStream}.
     **/
    static class CharBufferInputStream extends InputStream {

        private final CharBuffer charData;
        private final CharsetEncoder conversion;
        private boolean marked = false;
        private byte mark_multiByteChar[];
        private int mark_position;
        private byte multiByteChar[];
        private int position;

        /**
         * @param b        the char buffer
         * @param encoding the charset encoding
         */
        public CharBufferInputStream(CharBuffer b, String encoding) {
            this(b.duplicate(), Charset.forName(encoding));
        }

        /**
         * @param s        the char sequence
         * @param encoding the charset encoding
         */
        public CharBufferInputStream(CharSequence s, String encoding) {
            this(CharBuffer.wrap(s), Charset.forName(encoding));

        }

        /**
         * @param b The char buffer. <b>Must be for exclusive use of this
         * instance.</n>
         * @param encoding the charset encoding
         */
        public CharBufferInputStream(CharBuffer b, Charset encoding) {
            charData = b;

            conversion = encoding.newEncoder();
            conversion.onMalformedInput(CodingErrorAction.REPORT);
            conversion.onUnmappableCharacter(CodingErrorAction.REPORT);

            final int maxBytes = new Float(conversion.maxBytesPerChar()).intValue();

            multiByteChar = new byte[maxBytes];
            position = multiByteChar.length;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void mark(int ignored) {
            charData.mark();
            mark_multiByteChar = multiByteChar.clone();
            mark_position = position;
            marked = true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean markSupported() {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void reset() throws IOException {

            if (!marked) {
                throw new IOException("reset() called before mark()");
            }

            charData.reset();
            multiByteChar = mark_multiByteChar.clone();
            position = mark_position;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int read() throws IOException {
            // prefill the buffer
            while (multiByteChar.length == position) {
                final int readsome = read(multiByteChar, 0, multiByteChar.length);

                if (-1 == readsome) {
                    return -1;
                }

                position = multiByteChar.length - readsome;

                if ((0 != position) && (0 != readsome)) {
                    System.arraycopy(multiByteChar, 0, multiByteChar, position, readsome);
                }
            }

            return (multiByteChar[position++] & 0xFF);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int read(byte[] buffer) throws IOException {
            return read(buffer, 0, buffer.length);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            // handle partial characters;
            if (multiByteChar.length != position) {
                final int copying = Math.min(length, multiByteChar.length - position);

                System.arraycopy(multiByteChar, position, buffer, offset, copying);
                position += copying;
                return copying;
            }

            final ByteBuffer bb = ByteBuffer.wrap(buffer, offset, length);

            final int before = bb.remaining();

            final CoderResult result = conversion.encode(charData, bb, true);

            final int readin = before - bb.remaining();

            if (CoderResult.UNDERFLOW == result) {
                if (0 == readin) {
                    return -1;
                } else {
                    return readin;
                }
            }

            if (CoderResult.OVERFLOW == result) {
                return readin;
            }

            result.throwException();

            // NEVERREACHED
            return 0;
        }
    }
}
