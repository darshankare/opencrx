/*
 * ====================================================================
 * Project:     openCRX/Core, http://www.opencrx.org/
 * Name:        $Id: ReferencePropertyDataBinding.java,v 1.6 2012/01/13 17:16:05 wfro Exp $
 * Description: ReferencePropertyDataBinding
 * Revision:    $Revision: 1.6 $
 * Owner:       CRIXP AG, Switzerland, http://www.crixp.com
 * Date:        $Date: 2012/01/13 17:16:05 $
 * ====================================================================
 *
 * This software is published under the BSD license
 * as listed below.
 * 
 * Copyright (c) 2004-2008, CRIXP Corp., Switzerland
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
package org.opencrx.kernel.portal;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jmi.reflect.RefObject;

import org.opencrx.kernel.base.jmi1.Property;
import org.opencrx.kernel.base.jmi1.ReferenceProperty;

public class ReferencePropertyDataBinding extends AbstractPropertyDataBinding {

    public ReferencePropertyDataBinding(
    ) {
        super(PropertySetHolderType.CrxObject);
    }
    
    public ReferencePropertyDataBinding(
        PropertySetHolderType type
    ) {
        super(type);
    }
        
    public Object getValue(
        RefObject object, 
        String qualifiedFeatureName
    ) {
        Property p = this.findProperty(object, qualifiedFeatureName);
        if(p instanceof ReferenceProperty) {
            return ((ReferenceProperty)p).getReferenceValue();
        }
        else {
            return null;
        }
    }

    public void setValue(
        RefObject object, 
        String qualifiedFeatureName, 
        Object newValue
    ) {
        Property p = this.findProperty(object, qualifiedFeatureName);
        if(p == null) {
        	PersistenceManager pm = JDOHelper.getPersistenceManager(object);
        	p = pm.newInstance(ReferenceProperty.class);
            this.createProperty(
                object,
                qualifiedFeatureName,
                p
            );                
        }
        if(p instanceof ReferenceProperty) {
            ((ReferenceProperty)p).setReferenceValue((org.openmdx.base.cci2.BasicObject)newValue);
        }
    }

}
