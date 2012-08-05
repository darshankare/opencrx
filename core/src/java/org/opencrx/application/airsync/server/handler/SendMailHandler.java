/*
 * ====================================================================
 * Project:     openCRX/Core, http://www.opencrx.org/
 * Name:        $Id: SendMailHandler.java,v 1.6 2010/03/15 18:17:05 wfro Exp $
 * Description: openCRX application plugin
 * Revision:    $Revision: 1.6 $
 * Owner:       CRIXP AG, Switzerland, http://www.crixp.com
 * Date:        $Date: 2010/03/15 18:17:05 $
 * ====================================================================
 *
 * This software is published under the BSD license
 * as listed below.
 * 
 * Copyright (c) 2004-2007, CRIXP Corp., Switzerland
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
package org.opencrx.application.airsync.server.handler;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.opencrx.application.airsync.server.SyncRequest;
import org.opencrx.application.airsync.server.SyncResponse;
import org.opencrx.application.airsync.server.spi.IRequestHandler;
import org.opencrx.application.airsync.server.spi.ISyncBackend;
import org.openmdx.base.exception.ServiceException;

public class SendMailHandler implements IRequestHandler {

	public SendMailHandler(
		ISyncBackend backend
	) {
		this.backend = backend;
	}
	
	@Override
    public void handle(
    	SyncRequest request, 
    	SyncResponse response, 
    	boolean requestHasBody
    ) throws IOException {
		try {
			this.backend.sendMail(
				request, 
				request.getInputStream()
			);            
			response.setStatus(HttpServletResponse.SC_OK);
		} catch(Exception e) {
			new ServiceException(e).log();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
    }

	protected final ISyncBackend backend;
	
}