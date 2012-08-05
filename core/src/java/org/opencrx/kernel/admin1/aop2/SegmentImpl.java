/*
 * ====================================================================
 * Project:     openCRX/Core, http://www.opencrx.org/
 * Name:        $Id: SegmentImpl.java,v 1.7 2012/01/20 01:35:04 wfro Exp $
 * Description: openCRX application plugin
 * Revision:    $Revision: 1.7 $
 * Owner:       CRIXP AG, Switzerland, http://www.crixp.com
 * Date:        $Date: 2012/01/20 01:35:04 $
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
package org.opencrx.kernel.admin1.aop2;

import java.util.ArrayList;
import java.util.List;

import org.opencrx.kernel.admin1.jmi1.Admin1Package;
import org.opencrx.kernel.backend.Admin;
import org.opencrx.kernel.generic.SecurityKeys;
import org.opencrx.kernel.utils.Utils;
import org.openmdx.base.accessor.jmi.cci.JmiServiceException;
import org.openmdx.base.aop2.AbstractObject;
import org.openmdx.base.exception.ServiceException;

public class SegmentImpl
	<S extends org.opencrx.kernel.admin1.jmi1.Segment,N extends org.opencrx.kernel.admin1.cci2.Segment,C extends Void>
	extends AbstractObject<S,N,C> {

    //-----------------------------------------------------------------------
    public SegmentImpl(
        S same,
        N next
    ) {
    	super(same, next);
    }

    //-----------------------------------------------------------------------
    public org.opencrx.kernel.admin1.jmi1.CreateAdministratorResult createAdministrator(
        org.opencrx.kernel.admin1.jmi1.CreateAdministratorParams params
    ) {
    	try {
	        String segmentName = params.getSegmentName() != null ? 
	        	params.getSegmentName() : 
	        	"Standard";            
	        String adminPrincipalName = params.getAdminPrincipalName() != null ? 
	        	params.getAdminPrincipalName() : 
	        	SecurityKeys.ADMIN_PRINCIPAL + SecurityKeys.ID_SEPARATOR + segmentName;
	        String initialPassword = params.getInitialPassword();
	        String initialPasswordVerification = params.getInitialPasswordVerification();    
	        List<String> errors = new ArrayList<String>();
	        Admin.getInstance().createAdministrator(
	            this.sameObject(),
	            segmentName,
	            adminPrincipalName,
	            initialPassword,
	            initialPasswordVerification,
	            errors
	        );
	        if(!errors.isEmpty()) {
	            return ((Admin1Package)this.sameObject().refOutermostPackage().refPackage(Admin1Package.class.getName())).createCreateAdministratorResult(
	                (short)1,
	                errors.toString()
	            );
	        }
	        else {
	            return Utils.getAdminPackage(this.sameManager()).createCreateAdministratorResult(
	                (short)0,
	                null
	            );
	        }
    	}
    	catch(Exception e) {
    		throw new JmiServiceException(e);
    	}
    }
    
    //-----------------------------------------------------------------------
    public org.opencrx.kernel.admin1.jmi1.ImportLoginPrincipalsResult importLoginPrincipals(
        org.opencrx.kernel.admin1.jmi1.ImportLoginPrincipalsParams params
    ) {
        try {
            String statusMessage = Admin.getInstance().importLoginPrincipals(
                this.sameObject(),
                params.getItem()
            );
            return Utils.getAdminPackage(this.sameManager()).createImportLoginPrincipalsResult(
                statusMessage
            );
        }
        catch(ServiceException e) {
            throw new JmiServiceException(e);
        }                
    }
        
}
