/*
 * ====================================================================
 * Project:     openCRX/core, http://www.opencrx.org/
 * Description: XMLWriter
 * Owner:       CRIXP AG, Switzerland, http://www.crixp.com
 * ====================================================================
 *
 * This software is published under the BSD license
 * as listed below.
 * 
 * Copyright (c) 2010, CRIXP Corp., Switzerland
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions 
 * are met:
 * 
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in
 * the documentation and/or other materials provided with the
 * distribution.
 * 
 * * Neither the name of CRIXP Corp. nor the names of the contributors
 * to openCRX may be used to endorse or promote products derived
 * from this software without specific prior written permission
 * 
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * ------------------
 * 
 * This product includes software developed by the Apache Software
 * Foundation (http://www.apache.org/).
 * 
 * This product includes software developed by contributors to
 * openMDX (http://www.openmdx.org/)
 */

/*
 * This source was originally published under net.sf.webdav.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.opencrx.application.uses.net.sf.webdav.fromcatalina;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;

/**
 * XMLWriter helper class.
 * 
 * @author <a href="mailto:remm@apache.org">Remy Maucherat</a>
 */
public class XMLWriter {

    // -------------------------------------------------------------- Constants

    /**
     * Opening tag.
     */
    public static final int OPENING = 0;

    /**
     * Closing tag.
     */
    public static final int CLOSING = 1;

    /**
     * Element with no content.
     */
    public static final int NO_CONTENT = 2;

    // ----------------------------------------------------- Instance Variables

    /**
     * Buffer.
     */
    protected StringBuffer _buffer = new StringBuffer();

    /**
     * Writer.
     */
    protected Writer _writer = null;

    /**
     * Namespaces to be declared in the root element
     */
    protected Map<String, String> _namespaces;

    /**
     * Is true until the root element is written
     */
    protected boolean _isRootElement = true;

    // ----------------------------------------------------------- Constructors

    /**
     * Constructor.
     */
    public XMLWriter(Map<String, String> namespaces) {
        _namespaces = namespaces;
    }

    /**
     * Constructor.
     */
    public XMLWriter(Writer writer, Map<String, String> namespaces) {
        _writer = writer;
        _namespaces = namespaces;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Retrieve generated XML.
     * 
     * @return String containing the generated XML
     */
    @Override
    public String toString() {
        return _buffer.toString();
    }

    /**
     * Write property to the XML.
     * 
     * @param name
     *      Property name
     * @param value
     *      Property value
     */
    public void writeProperty(String name, String value) {
        writeElement(name, OPENING);
        _buffer.append(value);
        writeElement(name, CLOSING);
    }

    /**
     * Write property to the XML.
     * 
     * @param name
     *      Property name
     */
    public void writeProperty(String name) {
        writeElement(name, NO_CONTENT);
    }

    /**
     * Write an element.
     * 
     * @param name
     *      Element name
     * @param type
     *      Element type
     */
    public void writeElement(
    	String name, 
    	int type
    ) {
        StringBuffer nsdecl = new StringBuffer();
        if (_isRootElement) {
            for (Iterator<String> iter = _namespaces.keySet().iterator(); iter.hasNext();) {
                String fullName = iter.next();
                String abbrev = _namespaces.get(fullName);
                nsdecl.append(" xmlns:").append(abbrev).append("=\"").append(fullName).append("\"");
            }
            _isRootElement = false;
        }
        int pos = name.lastIndexOf(':');
        if (pos >= 0) {
            // lookup prefix for namespace
            String fullns = name.substring(0, pos);
            String prefix = _namespaces.get(fullns);
            if (prefix == null) {
                // there is no prefix for this namespace
                name = name.substring(pos + 1);
                nsdecl.append(" xmlns=\"").append(fullns).append("\"");
            } else {
                // there is a prefix
                name = prefix + ":" + name.substring(pos + 1);
            }
        } else {
            throw new IllegalArgumentException(
                    "All XML elements must have a namespace");
        }
        switch (type) {
	        case OPENING:
	            _buffer.append("<");
	            _buffer.append(name);
	            _buffer.append( nsdecl);
	            _buffer.append( ">");
	            break;
	        case CLOSING:
	            _buffer.append("</");
	            _buffer.append( name);
	            _buffer.append( ">\n");
	            break;
	        case NO_CONTENT:
	        default:
	            _buffer.append("<");
	            _buffer.append( name);
	            _buffer.append( nsdecl);
	            _buffer.append( "/>");
	            break;
        }
    }

    /**
     * Write text.
     * 
     * @param text
     *      Text to append
     */
    public void writeText(String text) {
        _buffer.append(text);
    }

    /**
     * Write data.
     * 
     * @param data
     *      Data to append
     */
    public void writeData(String data) {
        _buffer.append("<![CDATA[");
        _buffer.append( data);
        _buffer.append( "]]>");
    }

    /**
     * Write XML Header.
     */
    public void writeXMLHeader() {
        _buffer.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n");
    }

    /**
     * Send data and reinitializes buffer.
     */
    public void sendData() throws IOException {
        if (_writer != null) {
            _writer.write(_buffer.toString());
            _writer.flush();
            _buffer = new StringBuffer();
        }
    }

}
