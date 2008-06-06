/*
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights reserved.
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
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Xindice" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation and was
 * originally based on software copyright (c) 1999-2001, The dbXML
 * Group, L.L.C., http://www.dbxmlgroup.com.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package net.jxta.impl.xindice.core;

import java.util.HashMap;
import java.util.Map;

/**
 * FaultCodes defines the Xindice specific fault codes and associated error
 * messages.
 */
public enum FaultCodes {

    // the constants below have been pasted from
    // org.apache.xindice.client.corba.db.FaultCodes
    GEN(0, "General Error"),
    OBJ(100, "XMLObject invocation error"),
    COL(200, "Collection-related error"),
    IDX(300, "Index-related error"),
    TRX(400, "Transaction-related error"),
    DBE(500, "Database-related error"),
    QRY(600, "Query-related error"),
    SEC(700, "Security-related error"),
    URI(800, "URI-related error"),
    JAVA(2000, "JVM Runtime error"),
    GEN_UNKNOWN(0, "Unknown Error"),
    GEN_GENERAL_ERROR(40, "General Error"),
    GEN_CRITICAL_ERROR(70, "Critical Error"),
    GEN_FATAL_ERROR(90, "Fatal Error"),
    // XMLObject invocation errors 100 series
    OBJ_OBJECT_NOT_FOUND(100, "XMLObject Not Found"),
    OBJ_METHOD_NOT_FOUND(101, "XMLObject Method Not Found"),
    OBJ_NULL_RESULT(140, "XMLObject Null Result"),
    OBJ_INVALID_RESULT(141, "XMLObject Invalid Result"),
    OBJ_DUPLICATE_OBJECT(142, "XMLObject Duplicate Object"),
    OBJ_RUNTIME_EXCEPTION(170, "XMLObject Runtime Exception"),
    OBJ_CLASS_FORMAT_ERROR(171, "XMLObject Class Format Error"),
    OBJ_INVALID_CONTEXT(172, "XMLObject Invalid Context"),
    OBJ_CANNOT_CREATE(173, "XMLObject Cannot Create"),
    // Collection-related errors 200 series
    COL_COLLECTION_NOT_FOUND(200, "Collection Not Found"),
    COL_DOCUMENT_NOT_FOUND(201, "Collection Document Not Found"),
    COL_DUPLICATE_COLLECTION(240, "Collection Duplicated"),
    COL_NULL_RESULT(241, "Collection Null Result"),
    COL_NO_FILER(242, "Collection No Filer"),
    COL_NO_INDEXMANAGER(243, "Collection No IndexManager"),
    COL_DOCUMENT_MALFORMED(244, "Collection Document Malformed"),
    COL_CANNOT_STORE(245, "Collection Cannot Store"),
    COL_CANNOT_RETRIEVE(246, "Collection Cannot Retrieve"),
    COL_COLLECTION_READ_ONLY(247, "Collection Read-only"),
    COL_COLLECTION_CLOSED(248, "Collection Closed"),
    COL_CANNOT_CREATE(270, "Collection Cannot Create"),
    COL_CANNOT_DROP(271, "Collection Cannot Drop"),
    // Index-related errors 300 series
    IDX_VALUE_NOT_FOUND(300, "Index Value Not Found"),
    IDX_INDEX_NOT_FOUND(301, "Index Not Found"),
    IDX_MATCHES_NOT_FOUND(340, "Index Matches Not Found"),
    IDX_DUPLICATE_INDEX(341, "Index Duplicate Index"),
    IDX_NOT_SUPPORTED(370, "Index Not Supported"),
    IDX_STYLE_NOT_FOUND(371, "Index Style Not Found"),
    IDX_CORRUPTED(372, "Index Corrupted"),
    IDX_CANNOT_CREATE(373, "Index Cannot Create"),
    // Transaction-related errors 400 series
    TRX_DOC_LOCKED(400, "Transaction Document Locked"),
    TRX_NO_CONTEXT(440, "Transaction No Context"),
    TRX_NOT_ACTIVE(441, "Transaction Not Active"),
    TRX_NOT_SUPPORTED(470, "Transaction Not Supported"),
    // Database-related errors 500 series
    DBE_NO_PARENT(500, "Database No Parent"),
    DBE_CANNOT_DROP(570, "Database Cannot Drop"),
    DBE_CANNOT_CREATE(571, "Database Cannot Create"),
    // Query-related errors 600 series
    QRY_NULL_RESULT(600, "Query Null Result"),
    QRY_COMPILATION_ERROR(640, "Query Compilation Error"),
    QRY_PROCESSING_ERROR(641, "Query Processing Error"),
    QRY_NOT_SUPPORTED(670, "Query Not Supported"),
    QRY_STYLE_NOT_FOUND(671, "Query Style Not Found"),
    // Security-related errors 700 series
    SEC_INVALID_USER(770, "Security Invalid User"),
    SEC_INVALID_GROUP(771, "Security Invalid Group"),
    SEC_INVALID_ACCESS(772, "Security Invalid Access"),
    SEC_INVALID_CREDENTIALS(773, "Security Invalid Credentials"),
    URI_EMPTY(800, "URI Empty"),
    URI_NULL(801, "URI Null"),
    URI_PARSE_ERROR(820, "URI Parse Error"),
    JAVA_RUNTIME_ERROR(2070, "Java Runtime Error");
    /**
     * All known faults.
     */
    private static final Map<Integer, FaultCodes> knownFaults = new HashMap<Integer, FaultCodes>();
    /**
     *  Numeric code for this fault.
     */
    private final int code;
    /**
     * Message for this fault.
     */
    private final String message;

    private FaultCodes(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 
     * @param code The numeric fault code.
     * @return The cooresponding fault or {@code FaultCodes.GEN_UNKNOWN} if the
     * code does not match a known fault.
     */
    public static FaultCodes toFaultCodes(int code) {
        FaultCodes result;
        
        switch (code) {
            case 100:
                result = OBJ_OBJECT_NOT_FOUND;
                break;
            case 101:
                result = OBJ_METHOD_NOT_FOUND;
                break;
            case 140:
                result = OBJ_NULL_RESULT;
                break;

            case 141:
                result = OBJ_INVALID_RESULT;
                break;
            case 142:
                result = OBJ_DUPLICATE_OBJECT;
                break;

            case 170:
                result = OBJ_RUNTIME_EXCEPTION;
                break;
            case 171:
                result = OBJ_CLASS_FORMAT_ERROR;
                break;
            case 172:
                result = OBJ_INVALID_CONTEXT;
                break;
            case 173:
                result = OBJ_CANNOT_CREATE;
                break;

            // Collection-related errors 200 series
            case 200:
                result = COL_COLLECTION_NOT_FOUND;
                break;
            case 201:
                result = COL_DOCUMENT_NOT_FOUND;
                break;

            case 240:
                result = COL_DUPLICATE_COLLECTION;
                break;
            case 241:
                result = COL_NULL_RESULT;
                break;
            case 242:
                result = COL_NO_FILER;
                break;
            case 243:
                result = COL_NO_INDEXMANAGER;
                break;
            case 244:
                result = COL_DOCUMENT_MALFORMED;
                break;
            case 245:
                result = COL_CANNOT_STORE;
                break;
            case 246:
                result = COL_CANNOT_RETRIEVE;
                break;
            case 247:
                result = COL_COLLECTION_READ_ONLY;
                break;
            case 248:
                result = COL_COLLECTION_CLOSED;
                break;

            case 270:
                result = COL_CANNOT_CREATE;
                break;
            case 271:
                result = COL_CANNOT_DROP;
                break;

            // Index-related errors 300 series
            case 300:
                result = IDX_VALUE_NOT_FOUND;
                break;
            case 301:
                result = IDX_INDEX_NOT_FOUND;
                break;

            case 340:
                result = IDX_MATCHES_NOT_FOUND;
                break;
            case 341:
                result = IDX_DUPLICATE_INDEX;
                break;

            case 370:
                result = IDX_NOT_SUPPORTED;
                break;
            case 371:
                result = IDX_STYLE_NOT_FOUND;
                break;
            case 372:
                result = IDX_CORRUPTED;
                break;
            case 373:
                result = IDX_CANNOT_CREATE;
                break;

            // Transaction-related errors 400 series
            case 400:
                result = TRX_DOC_LOCKED;
                break;

            case 440:
                result = TRX_NO_CONTEXT;
                break;
            case 441:
                result = TRX_NOT_ACTIVE;
                break;

            case 470:
                result = TRX_NOT_SUPPORTED;
                break;

            // Database-related errors 500 series
            case 500:
                result = DBE_NO_PARENT;
                break;

            case 570:
                result = DBE_CANNOT_DROP;
                break;
            case 571:
                result = DBE_CANNOT_CREATE;
                break;

            // Query-related errors 600 series
            case 600:
                result = QRY_NULL_RESULT;
                break;

            case 640:
                result = QRY_COMPILATION_ERROR;
                break;
            case 641:
                result = QRY_PROCESSING_ERROR;
                break;

            case 670:
                result = QRY_NOT_SUPPORTED;
                break;
            case 671:
                result = QRY_STYLE_NOT_FOUND;
                break;

            // Security-related errors 700 series
            case 770:
                result = SEC_INVALID_USER;
                break;
            case 771:
                result = SEC_INVALID_GROUP;
                break;
            case 772:
                result = SEC_INVALID_ACCESS;
                break;
            case 773:
                result = SEC_INVALID_CREDENTIALS;
                break;

            case 800:
                result = URI_EMPTY;
                break;
            case 801:
                result = URI_NULL;
                break;

            case 820:
                result = URI_PARSE_ERROR;
                break;

            case 2070:
                result = JAVA_RUNTIME_ERROR;
                break;
                
            default:
                result = FaultCodes.GEN_UNKNOWN;
                break;
        }

        return result;
    }

    /**
     * Returns the fault code in numeric form.
     *
     * @param code The Fault Code
     * @return It's corresponding numeric code
     */
    public int getCode() {
        return code;
    }

    /**
     * getMessage returns a textual form for the specified fault code.
     *
     * @param code The Fault Code
     * @return It's textual form
     */
    public String getMessage() {
        return message;
    }
}

