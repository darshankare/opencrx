<%@page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8"%><%
/*
 * ====================================================================
 * Project:     opencrx, http://www.opencrx.org/
 * Name:        $Id: MessageOfTheDayDashlet.jsp,v 1.3 2010/04/27 12:16:11 wfro Exp $
 * Description: MessageOfTheDayDashlet
 * Revision:    $Revision: 1.3 $
 * Owner:       CRIXP Corp., Switzerland, http://www.crixp.com
 * Date:        $Date: 2010/04/27 12:16:11 $
 * ====================================================================
 *
 * This software is published under the BSD license
 * as listed below.
 *
 * Copyright (c) 2009-2012, CRIXP Corp., Switzerland
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
%>
<%@ page session="true" import="
java.util.*,
java.util.zip.*,
java.io.*,
java.text.*,
java.math.*,
java.net.*,
org.openmdx.base.accessor.jmi.cci.*,
org.openmdx.base.naming.*,
org.openmdx.base.exception.*,
org.openmdx.portal.servlet.*,
org.opencrx.kernel.backend.*,
org.opencrx.kernel.generic.*,
org.openmdx.kernel.log.*
" %>

<%!

	public org.opencrx.kernel.document1.jmi1.Document findDocument(
		String documentName,
		org.opencrx.kernel.document1.jmi1.Segment segment
	) throws ServiceException {
		return Documents.getInstance().findDocument(
			documentName,
			segment
		);
	}

%><%
	final String MESSAGE_OF_THE_DAY_DOCUMENT_NAME = "Message of the day.html";
	ApplicationContext app = (ApplicationContext)session.getValue(WebKeys.APPLICATION_KEY);
	ViewsCache viewsCache = (ViewsCache)session.getValue(WebKeys.VIEW_CACHE_KEY_SHOW);
	String parameters = request.getParameter(WebKeys.REQUEST_PARAMETER);
	if(app != null && parameters != null) {
		String xri = Action.getParameter(parameters, Action.PARAMETER_OBJECTXRI);
		String requestId = request.getParameter(Action.PARAMETER_REQUEST_ID);
		String dashletId = Action.getParameter(parameters, Action.PARAMETER_ID);
%>
		<div>
<%				
			if(xri != null && requestId != null && dashletId != null && viewsCache.getView(requestId) != null) {
				javax.jdo.PersistenceManager pm = app.getNewPmData();
				RefObject_1_0 obj = (RefObject_1_0)pm.getObjectById(new Path(xri));
				String providerName = obj.refGetPath().get(2);
				String segmentName = obj.refGetPath().get(4);
				
				org.opencrx.kernel.document1.jmi1.Segment documentSegment = Documents.getInstance().getDocumentSegment(pm, providerName, segmentName);
				
				String messageOfTheDay = "";
				try {
					org.opencrx.kernel.document1.jmi1.Document messageOfTheDayDoc = findDocument (
						MESSAGE_OF_THE_DAY_DOCUMENT_NAME,
						documentSegment
					);
					if (messageOfTheDayDoc != null) {
						org.opencrx.kernel.document1.jmi1.MediaContent headRevision =
							(org.opencrx.kernel.document1.jmi1.MediaContent)messageOfTheDayDoc.getHeadRevision();
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						org.w3c.cci2.BinaryLargeObjects.streamCopy(
							headRevision.getContent().getContent(),
							0L,
							bos
						);
						bos.close();
						messageOfTheDay += "<pre>" + headRevision.getModifiedAt() + "</pre>";
						messageOfTheDay += new String(bos.toByteArray(), "UTF-8");
						messageOfTheDay += "<br />";
					}
					if(messageOfTheDay.length() == 0) {
						messageOfTheDay += "<pre> no message as of " + new Date() + "</pre>";
					}
				} catch (Exception e) {
					new ServiceException(e).log();
%>
					<p>
				    <i>Dashlet Exception - see log file for details</i>
			    </p>
<%
				}
				pm.close();
%>
				<%= messageOfTheDay %>
<%
			}
			else {
%>
				<p>
			    <i>Dashlet invoked with missing or invalid parameters:</i>
			    <ul>
				    <li><b>RequestId:</b> <%= requestId %></li>
				    <li><b>XRI:</b> <%= xri %></li>
				    <li><b>Dashlet-Id:</b> <%= dashletId %></li>
					</ul>
				</p>
<%
			}
%>		     
		</div>
<%			
  	}
%>
