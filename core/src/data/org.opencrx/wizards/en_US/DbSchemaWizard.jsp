﻿<%@page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%
/*
 * ====================================================================
 * Project:     openCRX/Core, http://www.opencrx.org/
 * Description: DbSchemaWizard
 * Owner:       CRIXP Corp., Switzerland, http://www.crixp.com
 * ====================================================================
 *
 * This software is published under the BSD license
 * as listed below.
 *
 * Copyright (c) 2011-2013, CRIXP Corp., Switzerland
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
java.io.*,
java.text.*,
org.opencrx.kernel.backend.*,
org.opencrx.kernel.portal.wizard.*,
org.opencrx.kernel.generic.*,
org.openmdx.kernel.id.cci.*,
org.openmdx.kernel.id.*,
org.openmdx.base.exception.*,
org.openmdx.base.accessor.jmi.cci.*,
org.openmdx.portal.servlet.*,
org.openmdx.portal.servlet.attribute.*,
org.openmdx.portal.servlet.view.*,
org.openmdx.portal.servlet.control.*,
org.openmdx.portal.servlet.wizards.*,
org.openmdx.base.naming.*
"%>
<%
	final String FORM_NAME = "DbSchemaForm";
	final String WIZARD_NAME = "DbSchemaWizard.jsp";
	
	DbSchemaWizardController wc = new DbSchemaWizardController();
%>
	<t:wizardHandleCommand controller='<%= wc %>' defaultCommand='Refresh' />
<%
	if(response.getStatus() != HttpServletResponse.SC_OK) {
		wc.close();		
		return;
	}
	ApplicationContext app = wc.getApp();
	javax.jdo.PersistenceManager pm = wc.getPm();
	RefObject_1_0 obj = wc.getObject();
%>
<!--
	<meta name="label" content="Database schema wizard">
	<meta name="toolTip" content="Database schema wizard">
	<meta name="targetType" content="_inplace">
	<meta name="forClass" content="org:opencrx:kernel:admin1:Segment">
	<meta name="order" content="9999">
-->
<br />
<div class="OperationDialogTitle"><%= wc.getToolTip() %></div>
<form id="<%= FORM_NAME %>" name="<%= FORM_NAME %>" accept-charset="UTF-8" method="POST" action="<%= wc.getServletPath() %>">
	<input type="hidden" name="<%= Action.PARAMETER_REQUEST_ID %>" value="<%= wc.getRequestId() %>" />
	<input type="hidden" name="<%= Action.PARAMETER_OBJECTXRI %>" value="<%= wc.getObjectIdentity().toXRI() %>" />
	<input type="hidden" id="Command" name="Command" value="" />
	<table class="tableLayout">
		<tr>
			<td class="cellObject">
				<div>
					The wizard does NOT drop or remove any tables, columns and rows.
				</div>
				<br />
				<div id="contentArea">
					<table>
						<tr>
							<td>Connection URL:</td>
							<td>
								<input type="text" name="connectionUrl" id="connectionUrl" tabIndex="9000" style="width:50em;" value="<%= wc.getFormFields().getConnectionUrl() == null ? "java:comp/env/jdbc_opencrx_" + wc.getProviderName() : wc.getFormFields().getConnectionUrl() %>" />
<pre>Examples:
* java:comp/env/jdbc_opencrx_CRX
* jdbc:postgresql://127.0.0.1/CRX
* jdbc:mysql://127.0.0.1/CRX
* jdbc:hsqldb:hsql://127.0.0.1/CRX
* jdbc:db2://127.0.0.1:50000/CRX
* jdbc:oracle:thin:@127.0.0.1:1521:XE
* jdbc:sqlserver://127.0.0.1:1433;databaseName=CRX;selectMethod=cursor</pre>
							</td>
						</tr>
						<tr>	
							<td>User (for jdbc: URLs only):</td>
							<td><input type="text" name="userName" id="userName" tabIndex="9001" style="width:20em;" value="<%= wc.getFormFields().getUserName() == null ? "" : wc.getFormFields().getUserName() %>" /></td>
						</tr>
						<tr>
							<td>Password (for jdbc: URLs only):</td>
							<td><input type="password" name="password" id="password" tabIndex="9002" style="width:20em;" value="<%= wc.getFormFields().getPassword() == null ? "" : wc.getFormFields().getPassword() %>" /></td>
						</tr>
					</table>
					<div id="WaitIndicator" style="float:left;width:50px;height:24px;" class="wait">&nbsp;</div>
					<div id="SubmitArea" style="float:left;display:none;">									
						<input type="submit" name="Validate" id="Validate.Button" tabindex="9010" value="Validate" onclick="javascript:$('WaitIndicator').style.display='block';$('SubmitArea').style.display='none';$('ReportArea').style.display='none';$('Command').value=this.name;" />
						<input type="submit" name="ValidateAndFix" id="ValidateAndFix.Button" tabindex="9020" value="Validate & Fix" onclick="javascript:$('WaitIndicator').style.display='block';$('SubmitArea').style.display='none';$('ReportArea').style.display='none';$('Command').value=this.name;" />
						<input type="submit" name="Cancel" tabindex="9030" value="<%= app.getTexts().getCancelTitle() %>" onclick="javascript:$('Command').value=this.name;" />
					</div>
					<div id="ReportArea">
<%
						if(!wc.getReport().isEmpty()) {
%>				
							<br />
							<pre>Report + <%= new java.util.Date() %>:</pre>
							<table>
<%
								int n = 0;
								for(String reportLine: wc.getReport()) {
%>
									<tr>
										<td style="text-align:center;background-color:<%= reportLine.startsWith("OK") ? "lightgreen;" : reportLine.startsWith("ERROR") ? "red;" : "yellow;" %>"><%= n %></td>
										<td><pre style="display:inline"><%= reportLine %></pre></td>
									</tr>
<%						
									n++;
								}
%>
							</table>
<%
						}
%>
					</div>
				</div>				
			</td>
		</tr>
	</table>
</form>
<script type="text/javascript">
	Event.observe('<%= FORM_NAME %>', 'submit', function(event) {
		$('<%= FORM_NAME %>').request({
			onFailure: function() { },
			onSuccess: function(t) {
				$('UserDialog').update(t.responseText);
			}
		});
		Event.stop(event);
	});
	$('WaitIndicator').style.display='none';
	$('SubmitArea').style.display='block';		
</script>
<t:wizardClose controller="<%= wc %>" />

