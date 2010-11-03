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

package net.jxta.test.http;

import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 *
 * @version $Id: Dispatcher.java,v 1.2 2007/01/26 02:13:59 bondolo Exp $
 *
 * @author james todd [gonzo at jxta dot org]
 */

public class Dispatcher {

    private static final String QUESTION_MARK = "?";
    private static final String EQUAL = "=";
    private static final String AMPERSAND = "&";
    private static final String DEFAULT_URL_ENCODING = "UTF-8";
    private static final int SLEEP = 100;
    private static final boolean VERBOSE = false;

    private List cookies = new ArrayList();
    private boolean isCookieEnabled = false;
    private int maxWait = 0;

    public Dispatcher() {}

    public boolean isCookieEnabled() {
        return this.isCookieEnabled;
    }

    public void setCookieEnabled(boolean isCookieEnabled) {
        this.isCookieEnabled = isCookieEnabled;
    }

    public void setMaxWait(int maxWait) {
        this.maxWait = maxWait;
    }

    public int getMaxWait() {
        return this.maxWait;
    }

    public Message dispatch(URL url) throws IOException {
        return dispatch(url, (Map) null);
    }

    public Message dispatch(URL url, Map queryString) throws IOException {
        return dispatch(url, queryString, (Message) null);
    }

    public Message dispatch(URL url, Message message) throws IOException {
        return dispatch(url, (Map) null, message);
    }

    public Message dispatch(URL url, Map queryString, Message message) throws IOException {
        URL u = bindQueryString(url, queryString);
        Dispatchable dispatcher = DispatchableFactory.create(u, message);
        String header = null;

        for (Iterator c = this.cookies.iterator(); c.hasNext();) {
            header = (String) c.next();

            if (isCookieEnabled()) {
                dispatcher.setHeader(Constants.MIME.Key.COOKIE, header);
            }
        }

        Dispatch dispatch = new Dispatch(dispatcher);

        new Thread(dispatch, Dispatcher.class.getName() + ":dispatch").start();

        Timer timer = null;

        if (getMaxWait() > 0) {
            timer = new Timer();

            timer.schedule(new DispatchTimerTask(dispatch), getMaxWait());
        }

        while (!dispatch.isDone()) {
            try {
                Thread.sleep(SLEEP);
            } catch (InterruptedException ie) {
                if (VERBOSE) {
                    ie.printStackTrace();
                }
            }
        }

        Message response = dispatch.getResponse();

        dispatcher.close();

        if (timer != null) {
            timer.cancel();
        }

        if (response != null) {
            for (Iterator h = response.getHeaders(Constants.MIME.Key.SET_COOKIE); h.hasNext();) {
                this.cookies.add((String) h.next());
            }
        }

        return response;
    }

    private URL bindQueryString(URL requestURL, Map queryString) {
        URL u = requestURL;

        if (u != null && queryString != null) {
            StringBuilder sb = new StringBuilder();
            Iterator keys = queryString.keySet().iterator();
            String key = null;
            String value = null;

            while (keys.hasNext()) {
                key = (String) keys.next();
                value = (String) queryString.get(key);
                if (sb.length() > 0) {
                    sb.append(AMPERSAND);
                }
                sb.append(urlEncode(key) + EQUAL + urlEncode(value));
            }
            String s = sb.toString().trim();

            try {
                u = new URL(u.toString() + ((s.length() > 0 ? QUESTION_MARK + s : "")));
            } catch (MalformedURLException mue) {
                if (VERBOSE) {
                    mue.printStackTrace();
                }
            }
        }

        return u;
    }

    private String urlEncode(String value) {
        return urlEncode(value, DEFAULT_URL_ENCODING);
    }

    private String urlEncode(String value, String encoding) {
        String s = null;

        try {
            s = URLEncoder.encode(value, encoding);
        } catch (UnsupportedEncodingException use) {
            if (VERBOSE) {
                use.printStackTrace();
            }
        }

        return s;
    }
}

class DispatchTimerTask extends TimerTask {

    private Dispatch dispatcher = null;

    public DispatchTimerTask(Dispatch dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public void run() {
        if (!this.dispatcher.isDone()) {
            this.dispatcher.interrupt();
        }
    }
}

class Dispatch implements Runnable {

    private static int SLEEP = 100;
    private Dispatchable dispatchable = null;
    private Message response = null;
    private boolean isDone = false;
    private boolean interrupted = false;

    public Dispatch(Dispatchable dispatchable) {
        this.dispatchable = dispatchable;
    }

    public Message getResponse() {
        return this.response;
    }

    public boolean isDone() {
        return this.isDone;
    }

    public void interrupt() {
        setInterrupted(true);
    }

    public void run() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    response = dispatchable.dispatch();
                } catch (IOException ioe) {}

                isDone = true;
            }
        }, Dispatcher.class.getName() + ":timer");

        t.start();

        while (!this.isDone && !isInterrupted()) {
            try {
                Thread.sleep(SLEEP);
            } catch (InterruptedException ie) {
                setInterrupted(true);
            }
        }

        if (isInterrupted()) {
            this.dispatchable.close();
            t.interrupt();
        }

        this.isDone = true;
    }

    private boolean isInterrupted() {
        return (this.interrupted = this.interrupted || Thread.interrupted());
    }

    private void setInterrupted(boolean interrupted) {
        this.interrupted = interrupted;
    }
}
