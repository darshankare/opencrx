/*
 * ====================================================================
 * Project:     openCRX/Core, http://www.opencrx.org/
 * Description: BulkCreateActivityWizardController
 * Owner:       CRIXP AG, Switzerland, http://www.crixp.com
 * ====================================================================
 *
 * This software is published under the BSD license
 * as listed below.
 * 
 * Copyright (c) 2004-2013, CRIXP Corp., Switzerland
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
package org.opencrx.kernel.portal.wizard;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import javax.jdo.PersistenceManager;
import javax.servlet.http.HttpServletRequest;

import org.opencrx.kernel.account1.jmi1.AccountAddress;
import org.opencrx.kernel.account1.jmi1.AccountFilterGlobal;
import org.opencrx.kernel.account1.jmi1.EMailAddress;
import org.opencrx.kernel.activity1.cci2.ActivityQuery;
import org.opencrx.kernel.activity1.cci2.EMailRecipientQuery;
import org.opencrx.kernel.activity1.jmi1.Activity;
import org.opencrx.kernel.activity1.jmi1.ActivityCreator;
import org.opencrx.kernel.activity1.jmi1.ActivityGroup;
import org.opencrx.kernel.activity1.jmi1.ActivityType;
import org.opencrx.kernel.activity1.jmi1.EMail;
import org.opencrx.kernel.activity1.jmi1.EMailRecipient;
import org.opencrx.kernel.backend.Activities;
import org.opencrx.kernel.backend.Base;
import org.opencrx.kernel.backend.UserHomes;
import org.opencrx.kernel.backend.Workflows;
import org.opencrx.kernel.home1.cci2.WfActionLogEntryQuery;
import org.opencrx.kernel.home1.jmi1.UserHome;
import org.opencrx.kernel.home1.jmi1.WfActionLogEntry;
import org.opencrx.kernel.home1.jmi1.WfProcessInstance;
import org.opencrx.kernel.portal.StringPropertyDataBinding;
import org.opencrx.kernel.workflow.BulkCreateActivityWorkflow;
import org.opencrx.kernel.workflow.BulkCreateActivityWorkflow.CreationType;
import org.opencrx.kernel.workflow1.jmi1.WfProcess;
import org.openmdx.application.dataprovider.layer.persistence.jdbc.Database_1_Attributes;
import org.openmdx.base.accessor.jmi.cci.RefObject_1_0;
import org.openmdx.base.exception.ServiceException;
import org.openmdx.base.jmi1.BasicObject;
import org.openmdx.base.naming.Path;
import org.openmdx.base.persistence.cci.PersistenceHelper;
import org.openmdx.portal.servlet.AbstractWizardController;
import org.openmdx.portal.servlet.ApplicationContext;
import org.openmdx.portal.servlet.ObjectReference;
import org.openmdx.portal.servlet.ViewPort;
import org.openmdx.portal.servlet.ViewPortFactory;
import org.openmdx.portal.servlet.view.TransientObjectView;

/**
 * BulkCreateActivityWizardController
 *
 */
public class BulkCreateActivityWizardController extends AbstractWizardController {

	/**
	 * Constructor.
	 * 
	 */
	public BulkCreateActivityWizardController(
	) {
		super();
	}
	
	/* (non-Javadoc)
	 * @see org.openmdx.portal.servlet.AbstractWizardController#init(javax.servlet.http.HttpServletRequest, java.lang.String, boolean, boolean)
	 */
    @Override
    public boolean init(
    	HttpServletRequest request, 
    	String encoding, 
    	boolean assertRequestId, 
    	boolean assertObjectXri
    ) {
	    if(!super.init(request, encoding, assertRequestId, assertObjectXri)) {
	    	return false;
	    }
		org.openmdx.base.persistence.cci.UserObjects.setBulkLoad(this.getPm(), true);	    
	    RefObject_1_0 obj = this.getObject();
		if(
			obj instanceof AccountFilterGlobal ||
			obj instanceof org.opencrx.kernel.account1.jmi1.Group ||
			obj instanceof org.opencrx.kernel.account1.jmi1.AddressFilterGlobal ||
			obj instanceof org.opencrx.kernel.activity1.jmi1.AddressGroup
		) {
			return true;
		} else {
			return false;
		}
    }

    /**
     * Split string.
     * 
	 * @param text
	 * @param size
	 * @return
	 */
	public List<String> splitString(
		String text, 
		int size
	) {
		// Give the list the right capacity to start with. You could use an array instead if you wanted.
		if (text == null) {
			text = "";
		}
		List<String> ret = new ArrayList<String>((text.length() + size - 1) / size);
	
		for (int start = 0; start < text.length(); start += size) {
			ret.add(text.substring(start, Math.min(text.length(), start + size)));
		}
		return ret;
	}

	/**
	 * Update e-mail recipient.
	 * 
	 * @param email
	 * @param emailAddress
	 * @param partyType
	 * @throws ServiceException
	 */
	public void updateEMailRecipient(
		EMail email,
		EMailAddress emailAddress,
		Activities.PartyType partyType
	) throws ServiceException {
		javax.jdo.PersistenceManager pm = javax.jdo.JDOHelper.getPersistenceManager(email);
		EMailRecipientQuery query = 
			(EMailRecipientQuery)pm.newQuery(EMailRecipient.class);
		query.partyType().equalTo(partyType.getValue());
		query.orderByCreatedAt().descending();
		List<EMailRecipient> emailRecipients = email.getEmailRecipient(query);
		EMailRecipient emailRecipient = null;
		if(emailRecipients.isEmpty()) {
			emailRecipient = pm.newInstance(EMailRecipient.class);
			email.addEmailRecipient(
				Base.getInstance().getUidAsString(), 
				emailRecipient
			);
		} else {
			emailRecipient = emailRecipients.iterator().next();
		}		
		emailRecipient.setEmailHint(emailAddress.getEmailAddress());
		emailRecipient.setParty(emailAddress);
		emailRecipient.setPartyType(partyType.getValue());
		emailRecipient.setPartyStatus(Activities.PartyStatus.ACCEPTED.getValue());		
	}
	
	/**
	 * Get localized Calendar.
	 * 
	 * @param app
	 * @return
	 */
	public GregorianCalendar getLocalizedCalendar(
	) {
		GregorianCalendar cal = new GregorianCalendar(this.getApp().getCurrentLocale());
		cal.setTimeZone(TimeZone.getTimeZone(this.getApp().getCurrentTimeZone()));
		cal.setMinimalDaysInFirstWeek(4); // this conforms to DIN 1355/ISO 8601
		cal.setFirstDayOfWeek(GregorianCalendar.MONDAY);
		return cal;
	}
	
	/**
	 * Bulk create activities.
	 * 
	 * @param creationType
	 * @throws ServiceException
	 * @throws IOException
	 */
	public void createActivities(
		CreationType creationType
	) throws ServiceException {
		PersistenceManager pm = this.getPm();
		ApplicationContext app = this.getApp();
		this.creationType = creationType;
   		String name = (String)this.formFields.get("org:opencrx:kernel:activity1:Activity:name");
   		String description = (String)this.formFields.get("org:opencrx:kernel:activity1:Activity:description");
   		String detailedDescription = (String)this.formFields.get("org:opencrx:kernel:activity1:Activity:detailedDescription");
   		String messageSubject = (String)this.formFields.get("org:opencrx:kernel:activity1:EMail:messageSubject");
   		String messageBody = (String)this.formFields.get("org:opencrx:kernel:activity1:EMail:messageBody");
   		Date scheduledStart = (Date)this.formFields.get("org:opencrx:kernel:activity1:Activity:scheduledStart");
   		Date scheduledEnd = (Date)this.formFields.get("org:opencrx:kernel:activity1:Activity:scheduledEnd");
   		Short priority = (Short)this.formFields.get("org:opencrx:kernel:activity1:Activity:priority");
   		Date dueBy = (Date)this.formFields.get("org:opencrx:kernel:activity1:Activity:dueBy");
		this.selectedAccountsOnly = true;
		// Create activities
		if(this.canCreate) {
	    	try {
  				UserHome currentUserHome = UserHomes.getInstance().getUserHome(this.getObjectIdentity(), pm, true);		  
				org.opencrx.kernel.workflow1.jmi1.Segment workflowSegment = Workflows.getInstance().getWorkflowSegment(pm, this.getProviderName(), this.getSegmentName());
				WfProcess wfProcess = Workflows.getInstance().findWfProcess(org.opencrx.kernel.backend.Workflows.WORKFLOW_NAME_BULK_CREATE_ACTIVITY, workflowSegment);
  				pm.currentTransaction().begin();
				WfProcessInstance wfProcessInstance = Workflows.getInstance().executeWorkflow(
					this.activityCreator.getName() + " / " + creationType.name() + " / " + currentUserHome.refGetPath().getBase(),
					currentUserHome, 
					wfProcess,
					this.activityCreator,
					null, // triggeredBy
					null, // triggeredByEventId, 
					null, // triggeredByEventType
					null // parentProcessInstance
				);
				// Set BulkCreateActivityWorkflow parameters
				{
					org.opencrx.kernel.base.jmi1.IntegerProperty integerProperty = pm.newInstance(org.opencrx.kernel.base.jmi1.IntegerProperty.class);
					integerProperty.setName(BulkCreateActivityWorkflow.OPTION_LOCALE);
					integerProperty.setIntegerValue(this.locale == null ? 0 : this.locale.intValue());
					wfProcessInstance.addProperty(
						Base.getInstance().getUidAsString(),
						integerProperty
					);
				}
				{
					org.opencrx.kernel.base.jmi1.StringProperty stringProperty = pm.newInstance(org.opencrx.kernel.base.jmi1.StringProperty.class);
					stringProperty.setName(BulkCreateActivityWorkflow.OPTION_DEFAULT_PLACEHOLDERS);
					stringProperty.setStringValue((String)this.formFields.get("org:opencrx:kernel:base:Note:text"));
					wfProcessInstance.addProperty(
						Base.getInstance().getUidAsString(),
						stringProperty
					);
				}
				{
					org.opencrx.kernel.base.jmi1.StringProperty stringProperty = pm.newInstance(org.opencrx.kernel.base.jmi1.StringProperty.class);
					stringProperty.setName(BulkCreateActivityWorkflow.OPTION_CREATION_TYPE);
					stringProperty.setStringValue(creationType.name());
					wfProcessInstance.addProperty(
						Base.getInstance().getUidAsString(),
						stringProperty
					);
				}
				BasicObject accounts = null;
				if(this.accountFilter != null) {
					accounts = this.accountFilter;
				} else if(this.group != null) {
					accounts = this.group;
				} else if(this.addressFilter != null) {
					accounts = this.addressFilter;
				} else if(this.addressGroup != null) {
					accounts = this.addressGroup;
				}					
				if(accounts != null) {
					org.opencrx.kernel.base.jmi1.ReferenceProperty referenceProperty = pm.newInstance(org.opencrx.kernel.base.jmi1.ReferenceProperty.class);
					referenceProperty.setName(BulkCreateActivityWorkflow.OPTION_ACCOUNTS_SELECTOR);
					referenceProperty.setReferenceValue(accounts);
					wfProcessInstance.addProperty(
						Base.getInstance().getUidAsString(),
						referenceProperty
					);
				}
				if(name != null) {
					org.opencrx.kernel.base.jmi1.StringProperty stringProperty = pm.newInstance(org.opencrx.kernel.base.jmi1.StringProperty.class);
					stringProperty.setName(BulkCreateActivityWorkflow.OPTION_ACTIVITY_NAME);
					stringProperty.setStringValue(name);
					wfProcessInstance.addProperty(
						Base.getInstance().getUidAsString(),
						stringProperty
					);
				}
				if(description != null) {
					org.opencrx.kernel.base.jmi1.StringProperty stringProperty = pm.newInstance(org.opencrx.kernel.base.jmi1.StringProperty.class);
					stringProperty.setName(BulkCreateActivityWorkflow.OPTION_ACTIVITY_DESCRIPTION);
					stringProperty.setStringValue(description);
					wfProcessInstance.addProperty(
						Base.getInstance().getUidAsString(),
						stringProperty
					);
				}
				if(detailedDescription != null) {
					org.opencrx.kernel.base.jmi1.StringProperty stringProperty = pm.newInstance(org.opencrx.kernel.base.jmi1.StringProperty.class);
					stringProperty.setName(BulkCreateActivityWorkflow.OPTION_ACTIVITY_DETAILED_DESCRIPTION);
					stringProperty.setStringValue(detailedDescription);
					wfProcessInstance.addProperty(
						Base.getInstance().getUidAsString(),
						stringProperty
					);
				}
				if(priority != null) {
					org.opencrx.kernel.base.jmi1.IntegerProperty integerProperty = pm.newInstance(org.opencrx.kernel.base.jmi1.IntegerProperty.class);
					integerProperty.setName(BulkCreateActivityWorkflow.OPTION_ACTIVITY_PRIORITY);
					integerProperty.setIntegerValue(priority.intValue());
					wfProcessInstance.addProperty(
						Base.getInstance().getUidAsString(),
						integerProperty
					);
				}
				if(scheduledStart != null) {
					org.opencrx.kernel.base.jmi1.DateTimeProperty dateTimeProperty = pm.newInstance(org.opencrx.kernel.base.jmi1.DateTimeProperty.class);
					dateTimeProperty.setName(BulkCreateActivityWorkflow.OPTION_ACTIVITY_SCHEDULED_START);
					dateTimeProperty.setDateTimeValue(scheduledStart);
					wfProcessInstance.addProperty(
						Base.getInstance().getUidAsString(),
						dateTimeProperty
					);
				}
				if(scheduledEnd != null) {
					org.opencrx.kernel.base.jmi1.DateTimeProperty dateTimeProperty = pm.newInstance(org.opencrx.kernel.base.jmi1.DateTimeProperty.class);
					dateTimeProperty.setName(BulkCreateActivityWorkflow.OPTION_ACTIVITY_SCHEDULED_END);
					dateTimeProperty.setDateTimeValue(scheduledEnd);
					wfProcessInstance.addProperty(
						Base.getInstance().getUidAsString(),
						dateTimeProperty
					);
				}
				if(dueBy != null) {
					org.opencrx.kernel.base.jmi1.DateTimeProperty dateTimeProperty = pm.newInstance(org.opencrx.kernel.base.jmi1.DateTimeProperty.class);
					dateTimeProperty.setName(BulkCreateActivityWorkflow.OPTION_ACTIVITY_DUE_BY);
					dateTimeProperty.setDateTimeValue(dueBy);
					wfProcessInstance.addProperty(
						Base.getInstance().getUidAsString(),
						dateTimeProperty
					);
				}
				if(this.sender != null) {
					org.opencrx.kernel.base.jmi1.ReferenceProperty referenceProperty = pm.newInstance(org.opencrx.kernel.base.jmi1.ReferenceProperty.class);
					referenceProperty.setName(BulkCreateActivityWorkflow.OPTION_EMAIL_SENDER);
					referenceProperty.setReferenceValue(this.sender);
					wfProcessInstance.addProperty(
						Base.getInstance().getUidAsString(),
						referenceProperty
					);					
				}
				if(messageSubject != null) {
					org.opencrx.kernel.base.jmi1.StringProperty stringProperty = pm.newInstance(org.opencrx.kernel.base.jmi1.StringProperty.class);
					stringProperty.setName(BulkCreateActivityWorkflow.OPTION_EMAIL_MESSAGE_SUBJECT);
					stringProperty.setStringValue(messageSubject);
					wfProcessInstance.addProperty(
						Base.getInstance().getUidAsString(),
						stringProperty
					);
				}
				if(messageBody != null) {
					int idx = 0;
					for(String messageBodyPart: this.splitString(messageBody, 2048)) {
						org.opencrx.kernel.base.jmi1.StringProperty stringProperty = pm.newInstance(org.opencrx.kernel.base.jmi1.StringProperty.class);
						stringProperty.setName(BulkCreateActivityWorkflow.OPTION_EMAIL_MESSAGE_BODY + idx);
						stringProperty.setStringValue(messageBodyPart);
						wfProcessInstance.addProperty(
							Base.getInstance().getUidAsString(),
							stringProperty
						);
						idx++;
					}
				}
				@SuppressWarnings("unchecked")
                List<Short> emailAddressUsages = (List<Short>)this.formFields.get("org:opencrx:kernel:address1:Addressable:usage");
				if(emailAddressUsages != null) {
					int idx = 0;
					for(Short emailAddressUsage: emailAddressUsages) {
						org.opencrx.kernel.base.jmi1.IntegerProperty integerProperty = pm.newInstance(org.opencrx.kernel.base.jmi1.IntegerProperty.class);
						integerProperty.setName(BulkCreateActivityWorkflow.OPTION_EMAIL_ADDRESS_USAGE + idx);
						integerProperty.setIntegerValue(emailAddressUsage.intValue());
						wfProcessInstance.addProperty(
							Base.getInstance().getUidAsString(),
							integerProperty
						);
						idx++;
					}
				}
				if(this.formFields.get("org:opencrx:kernel:account1:AccountAssignment:account") != null) {
					org.opencrx.kernel.base.jmi1.ReferenceProperty referenceProperty = pm.newInstance(org.opencrx.kernel.base.jmi1.ReferenceProperty.class);
					referenceProperty.setName(BulkCreateActivityWorkflow.OPTION_TEST_ACCOUNT);
					referenceProperty.setReferenceValue(
						(BasicObject)pm.getObjectById((Path)this.formFields.get("org:opencrx:kernel:account1:AccountAssignment:account"))
					);
					wfProcessInstance.addProperty(
						Base.getInstance().getUidAsString(),
						referenceProperty
					);
				}
				for(int i = 0; i < 3; i++) {
					if(this.formFields.get("org:opencrx:kernel:activity1:EMail:to" + i) != null) {
						org.opencrx.kernel.base.jmi1.ReferenceProperty referenceProperty = pm.newInstance(org.opencrx.kernel.base.jmi1.ReferenceProperty.class);
						referenceProperty.setName(BulkCreateActivityWorkflow.OPTION_TEST_EMAIL + i);
						referenceProperty.setReferenceValue(
							(BasicObject)pm.getObjectById((Path)this.formFields.get("org:opencrx:kernel:activity1:EMail:to" + i))
						);
						wfProcessInstance.addProperty(
							Base.getInstance().getUidAsString(),
							referenceProperty
						);
					}
				}
				if(this.excludeNoBulkEMail != null) {
					org.opencrx.kernel.base.jmi1.BooleanProperty booleanProperty = pm.newInstance(org.opencrx.kernel.base.jmi1.BooleanProperty.class);
					booleanProperty.setName(BulkCreateActivityWorkflow.OPTION_EXCLUDE_NO_BULK_EMAIL);
					booleanProperty.setBooleanValue(this.excludeNoBulkEMail);
					wfProcessInstance.addProperty(
						Base.getInstance().getUidAsString(),
						booleanProperty
					);
				}
				pm.currentTransaction().commit();
				// In test mode execute BulkCreateActivityWorkflow immediately instead 
				// of waiting for WorkflowHandlerServlet
				if(
					creationType == CreationType.CREATE ||
					creationType == CreationType.CREATE_TEST ||
					creationType == CreationType.CREATE_TEST_CONFIRMED
				) {
					try {
						pm.currentTransaction().begin();
						wfProcessInstance.setStartedOn(new Date());
                        pm.currentTransaction().commit();
						new BulkCreateActivityWorkflow().execute(wfProcessInstance);
						pm.currentTransaction().begin();
                    	pm.refresh(wfProcessInstance);
                    	wfProcessInstance.setFailed(Boolean.FALSE);
                    	wfProcessInstance.setLastActivityOn(new Date());
                    	wfProcessInstance.setStepCounter(
                            new Integer(wfProcessInstance.getStepCounter().intValue() + 1)
                        );
                        pm.currentTransaction().commit();
                        this.executionReport = new ArrayList<String>();
                        WfActionLogEntryQuery wfActionLogEntryQuery = (WfActionLogEntryQuery)pm.newQuery(WfActionLogEntry.class);
                        wfActionLogEntryQuery.orderByCreatedAt().ascending();
                        for(WfActionLogEntry wfActionLogEntry: wfProcessInstance.getActionLog(wfActionLogEntryQuery)) {
                        	this.executionReport.add(wfActionLogEntry.getName());                        	
                        	this.executionReport.add(wfActionLogEntry.getDescription());                        	
                        }
					} catch(Exception e) {
						try {
							pm.currentTransaction().rollback();
						} catch(Exception ignore) {}
						if(wfProcessInstance != null) {
							pm.currentTransaction().begin();
							wfProcessInstance.setFailed(Boolean.TRUE);
							pm.currentTransaction().commit();
						}
					}
				} else {
					this.setExitAction(
						new ObjectReference(wfProcess, app).getSelectObjectAction()
					);
				}
			} catch(Exception e) {
				try {
					pm.currentTransaction().rollback();
				} catch(Exception e1) {}
				new ServiceException(e).log();
			}
		}
	}

	/**
	 * OK action.
	 * 
	 * @param isInitialized
	 * @param excludeNoBulkEMail
	 * @param formFields
	 * @throws ServiceException
	 */
	public void doOK(
		@RequestParameter(name = "isInitialized") Boolean isInitialized,
		@RequestParameter(name = "excludeNoBulkEMail") Boolean excludeNoBulkEMail,
		@FormParameter(forms = {"BulkCreateActivityFormCreator", "BulkCreateActivityFormPlaceHolders", "BulkCreateActivityFormActivity", "BulkCreateActivityFormEMail", "BulkCreateActivityFormEMailTo", "BulkCreateActivityFormRecipient"}) Map<String,Object> formFields		
	) throws ServiceException {
		this.doRefresh(
			isInitialized,
			excludeNoBulkEMail, 
			formFields
		);
	}

	/**
	 * Create action.
	 * 
	 * @param isInitialized
	 * @param excludeNoBulkEMail
	 * @param formFields
	 * @throws ServiceException
	 */
	public void doCreate(
		@RequestParameter(name = "isInitialized") Boolean isInitialized,
		@RequestParameter(name = "excludeNoBulkEMail") Boolean excludeNoBulkEMail,
		@FormParameter(forms = {"BulkCreateActivityFormCreator", "BulkCreateActivityFormPlaceHolders", "BulkCreateActivityFormActivity", "BulkCreateActivityFormEMail", "BulkCreateActivityFormEMailTo", "BulkCreateActivityFormRecipient"}) Map<String,Object> formFields		
	) throws ServiceException {
		this.doRefresh(
			isInitialized,
			excludeNoBulkEMail, 
			formFields
		);
		this.createActivities(CreationType.CREATE);
	}

	/**
	 * CreateConfirmed action.
	 * 
	 * @param isInitialized
	 * @param excludeNoBulkEMail
	 * @param formFields
	 * @throws ServiceException
	 */
	public void doCreateConfirmed(
		@RequestParameter(name = "isInitialized") Boolean isInitialized,
		@RequestParameter(name = "excludeNoBulkEMail") Boolean excludeNoBulkEMail,
		@FormParameter(forms = {"BulkCreateActivityFormCreator", "BulkCreateActivityFormPlaceHolders", "BulkCreateActivityFormActivity", "BulkCreateActivityFormEMail", "BulkCreateActivityFormEMailTo", "BulkCreateActivityFormRecipient"}) Map<String,Object> formFields		
	) throws ServiceException {
		this.doRefresh(
			isInitialized,
			excludeNoBulkEMail, 
			formFields
		);
		this.createActivities(CreationType.CREATE_CONFIRMED);
	}
	
	/**
	 * CreateTest action.
	 * 
	 * @param isInitialized
	 * @param excludeNoBulkEMail
	 * @param formFields
	 * @throws ServiceException
	 */
	public void doCreateTest(
		@RequestParameter(name = "isInitialized") Boolean isInitialized,
		@RequestParameter(name = "excludeNoBulkEMail") Boolean excludeNoBulkEMail,
		@FormParameter(forms = {"BulkCreateActivityFormCreator", "BulkCreateActivityFormPlaceHolders", "BulkCreateActivityFormActivity", "BulkCreateActivityFormEMail", "BulkCreateActivityFormEMailTo", "BulkCreateActivityFormRecipient"}) Map<String,Object> formFields		
	) throws ServiceException {
		this.doRefresh(
			isInitialized,
			excludeNoBulkEMail, 
			formFields
		);
		this.createActivities(CreationType.CREATE_TEST);
	}

	/**
	 * CreateTestConfirmed action.
	 * 
	 * @param isInitialized
	 * @param excludeNoBulkEMail
	 * @param formFields
	 * @throws ServiceException
	 */
	public void doCreateTestConfirmed(
		@RequestParameter(name = "isInitialized") Boolean isInitialized,
		@RequestParameter(name = "excludeNoBulkEMail") Boolean excludeNoBulkEMail,
		@FormParameter(forms = {"BulkCreateActivityFormCreator", "BulkCreateActivityFormPlaceHolders", "BulkCreateActivityFormActivity", "BulkCreateActivityFormEMail", "BulkCreateActivityFormEMailTo", "BulkCreateActivityFormRecipient"}) Map<String,Object> formFields		
	) throws ServiceException {
		this.doRefresh(
			isInitialized,
			excludeNoBulkEMail, 
			formFields
		);
		this.createActivities(CreationType.CREATE_TEST_CONFIRMED);
	}

	/**
	 * Save action.
	 * 
	 * @param isInitialized
	 * @param excludeNoBulkEMail
	 * @param formFields
	 * @throws ServiceException
	 */
	public void doSave(
		@RequestParameter(name = "isInitialized") Boolean isInitialized,
		@RequestParameter(name = "excludeNoBulkEMail") Boolean excludeNoBulkEMail,
		@FormParameter(forms = {"BulkCreateActivityFormCreator", "BulkCreateActivityFormPlaceHolders", "BulkCreateActivityFormActivity", "BulkCreateActivityFormEMail", "BulkCreateActivityFormEMailTo", "BulkCreateActivityFormRecipient"}) Map<String,Object> formFields		
	) throws ServiceException {
		PersistenceManager pm = this.getPm();
		this.doRefresh(
			isInitialized,
			excludeNoBulkEMail, 
			formFields
		);
		String text = (String)formFields.get("org:opencrx:kernel:base:Note:text");
		try {
			Properties placeHolders = new Properties();
			if(text != null) {
				placeHolders.load(new StringReader(text));
			}
			BulkCreateActivityWorkflow bulkCreateActivityWorkflow = new BulkCreateActivityWorkflow();
			bulkCreateActivityWorkflow.updatePlaceHolders(
				placeHolders,
				(String)formFields.get("org:opencrx:kernel:activity1:Activity:description")
			);
			bulkCreateActivityWorkflow.updatePlaceHolders(
				placeHolders,
				(String)formFields.get("org:opencrx:kernel:activity1:Activity:detailedDescription")
			);
			bulkCreateActivityWorkflow.updatePlaceHolders(
				placeHolders,
				(String)formFields.get("org:opencrx:kernel:activity1:EMail:messageSubject")
			);
			bulkCreateActivityWorkflow.updatePlaceHolders(
				placeHolders,
				(String)formFields.get("org:opencrx:kernel:activity1:EMail:messageBody")
			);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();			
			placeHolders.store(new OutputStreamWriter(bos, "UTF-8"), "");
			// Update form values
			formFields.put(
				"org:opencrx:kernel:base:Note:text",
				bos.toString("UTF-8")
			);
		} catch(Exception ignore) {}
		// Save as properties on activity creator
		if(this.activityCreator != null && this.locale != null) {
			try {
				pm.currentTransaction().begin();
				StringPropertyDataBinding dataBinding = new StringPropertyDataBinding();
				dataBinding.setValue(
					this.activityCreator, 
					":BulkCreateActivityWizardSettings." + this.locale + "!name", 
					formFields.get("org:opencrx:kernel:activity1:Activity:name")
				);
				dataBinding.setValue(
					this.activityCreator, 
					":BulkCreateActivityWizardSettings." + locale + "!description", 
					formFields.get("org:opencrx:kernel:activity1:Activity:description")
				);
				dataBinding.setValue(
					this.activityCreator, 
					":BulkCreateActivityWizardSettings." + locale + "!detailedDescription", 
					formFields.get("org:opencrx:kernel:activity1:Activity:detailedDescription")
				);
				dataBinding.setValue(
					this.activityCreator, 
					":BulkCreateActivityWizardSettings." + locale + "!messageSubject", 
					formFields.get("org:opencrx:kernel:activity1:EMail:messageSubject")
				);
				// split messageBody into pieces of 2048 chars
				List<String> messageBodyParts = this.splitString((String)formFields.get("org:opencrx:kernel:activity1:EMail:messageBody"), 2048);
				int idx = 0;
				for(int i = 0; i < messageBodyParts.size(); i++) {
					try {
						dataBinding.setValue(
							this.activityCreator, 
							":BulkCreateActivityWizardSettings." + locale + "!messageBody" + i, 
							messageBodyParts.get(i)
						);
						idx++;
					} catch (Exception e) {
						new ServiceException(e).log();
					}
				}
				// reset unused messageBody properties if they exist
				try {
					while (dataBinding.getValue(this.activityCreator, ":BulkCreateActivityWizardSettings." + locale + "!messageBody" + idx) != null) {
						org.opencrx.kernel.base.jmi1.Property property = dataBinding.findProperty(
							activityCreator, 
							":BulkCreateActivityWizardSettings." + locale + "!messageBody" + idx
						);
						property.refDelete();
						idx++;
					}
				} catch (Exception e) {
					new ServiceException(e).log();
				}				
				dataBinding.setValue(
					this.activityCreator, 
					":BulkCreateActivityWizardSettings." + locale + "!placeHolders", 
					formFields.get("org:opencrx:kernel:base:Note:text")
				);
				Path senderPath = (Path)formFields.get("org:opencrx:kernel:activity1:EMail:sender");
				dataBinding.setValue(
					this.activityCreator, 
					":BulkCreateActivityWizardSettings." + locale + "!sender", 
					(senderPath == null ? null : senderPath.toXRI())
				);
				int i = 0;
				@SuppressWarnings("unchecked")
                List<Short> usages = (List<Short>)formFields.get("org:opencrx:kernel:address1:Addressable:usage");
				for(Short usage: usages) {
					dataBinding.setValue(
						activityCreator,
						":BulkCreateActivityWizardSettings." + locale + "!usage." + i,
						usage.toString()
					);
					i++;
				}
				pm.currentTransaction().commit();
			} catch(Exception e) {
				new ServiceException(e).log();
				try {
					pm.currentTransaction().rollback();
				} catch(Exception e0) {}
			}
		}
	}		

	/**
	 * Refresh action.
	 * 
	 * @param isInitialized
	 * @param excludeNoBulkEMail
	 * @param formFields
	 * @throws ServiceException
	 */
	public void doRefresh(
		@RequestParameter(name = "isInitialized") Boolean isInitialized,
		@RequestParameter(name = "excludeNoBulkEMail") Boolean excludeNoBulkEMail,
		@FormParameter(forms = {"BulkCreateActivityFormCreator", "BulkCreateActivityFormPlaceHolders", "BulkCreateActivityFormActivity", "BulkCreateActivityFormEMail", "BulkCreateActivityFormEMailTo", "BulkCreateActivityFormRecipient"}) Map<String,Object> formFields		
	) throws ServiceException {
		PersistenceManager pm = this.getPm();
		RefObject_1_0 obj = this.getObject();
		this.formFields = formFields;
		if(!Boolean.TRUE.equals(isInitialized)) {
			this.excludeNoBulkEMail = true;
		} else {
			this.excludeNoBulkEMail = excludeNoBulkEMail;
		}
		if(obj instanceof AccountFilterGlobal) {
			this.accountFilter = (AccountFilterGlobal)obj;
		} else if(obj instanceof org.opencrx.kernel.account1.jmi1.Group) {
			this.group = (org.opencrx.kernel.account1.jmi1.Group)obj;
		} else if(obj instanceof org.opencrx.kernel.account1.jmi1.AddressFilterGlobal) {
			this.addressFilter = (org.opencrx.kernel.account1.jmi1.AddressFilterGlobal)obj;
		} else if(obj instanceof org.opencrx.kernel.activity1.jmi1.AddressGroup) {
			this.addressGroup = (org.opencrx.kernel.activity1.jmi1.AddressGroup)obj;
		}
		// Initialize formFields on first call
		this.activityCreator = this.formFields.get("org:opencrx:kernel:activity1:Activity:lastAppliedCreator") != null
			? (ActivityCreator)pm.getObjectById(
			   	formFields.get("org:opencrx:kernel:activity1:Activity:lastAppliedCreator")
			  )
			: null;
		this.locale = (Number)formFields.get("org:opencrx:kernel:generic:LocalizedField:locale");
		this.sender = null;
		try {
			Path senderPath = null;
			if(this.formFields.get("org:opencrx:kernel:activity1:EMail:sender") != null) {
				if(this.formFields.get("org:opencrx:kernel:activity1:EMail:sender") instanceof String) {
					senderPath = new Path((String)this.formFields.get("org:opencrx:kernel:activity1:EMail:sender"));
				} else {
					senderPath = (Path)this.formFields.get("org:opencrx:kernel:activity1:EMail:sender");
				}
				this.sender = (AccountAddress)pm.getObjectById(senderPath);
			}
		} catch(Exception e) {
			new ServiceException(e).log();
		}
		this.activityType = null;
		if(this.activityCreator != null) {
			try {
				this.activityType = this.activityCreator.getActivityType();
			} catch (Exception e) {
				new ServiceException(e).log();
			}
		}
		this.canCreate = 
			this.activityCreator != null && 
			this.activityType != null &&
			(this.sender != null || this.activityType.getActivityClass() != Activities.ActivityClass.EMAIL.getValue());
	}

	/**
	 * Reload action.
	 * 
	 * @param isInitialized
	 * @param excludeNoBulkEMail
	 * @param formFields
	 * @throws ServiceException
	 */
	public void doReload(
		@RequestParameter(name = "isInitialized") Boolean isInitialized,
		@RequestParameter(name = "excludeNoBulkEMail") Boolean excludeNoBulkEMail,
		@FormParameter(forms = {"BulkCreateActivityFormCreator", "BulkCreateActivityFormPlaceHolders", "BulkCreateActivityFormActivity", "BulkCreateActivityFormEMail", "BulkCreateActivityFormEMailTo", "BulkCreateActivityFormRecipient"}) Map<String,Object> formFields		
	) throws ServiceException {
		PersistenceManager pm = this.getPm();
		this.doRefresh(
			isInitialized,
			excludeNoBulkEMail, 
			formFields
		);
		if(this.activityCreator != null) {
			org.opencrx.kernel.generic.cci2.PropertySetQuery propertySetFilter = 
				(org.opencrx.kernel.generic.cci2.PropertySetQuery)pm.newQuery(org.opencrx.kernel.generic.jmi1.PropertySet.class);
			propertySetFilter.name().equalTo("BulkCreateActivityWizardSettings." + this.locale);
			boolean needsInit = !this.activityCreator.getPropertySet(propertySetFilter).iterator().hasNext();
			StringPropertyDataBinding dataBinding = new StringPropertyDataBinding();
			this.formFields.put(
				"org:opencrx:kernel:activity1:Activity:name",
				needsInit
					? this.activityCreator.getName()
					: dataBinding.getValue(
							activityCreator, 
							":BulkCreateActivityWizardSettings." + locale + "!name"
						)
			);
			this.formFields.put(
				"org:opencrx:kernel:activity1:Activity:description",
				dataBinding.getValue(
					activityCreator, 
					":BulkCreateActivityWizardSettings." + locale + "!description"
				)					
			);
			this.formFields.put(
				"org:opencrx:kernel:activity1:Activity:detailedDescription",
				dataBinding.getValue(
					activityCreator, 
					":BulkCreateActivityWizardSettings." + locale + "!detailedDescription"
				)					
			);
			this.formFields.put(
				"org:opencrx:kernel:activity1:EMail:messageSubject",
				dataBinding.getValue(
					activityCreator, 
					":BulkCreateActivityWizardSettings." + locale + "!messageSubject"
				)					
			);
			// get messageBodyPieces
			String tempMessageBody = "";
			int idx = 0;
			try {
				while (dataBinding.getValue(activityCreator, ":BulkCreateActivityWizardSettings." + locale + "!messageBody" + idx) != null) {
					tempMessageBody += dataBinding.getValue(activityCreator, ":BulkCreateActivityWizardSettings." + locale + "!messageBody" + idx);
					idx++;
				}
			} catch (Exception e) {}
			this.formFields.put(
				"org:opencrx:kernel:activity1:EMail:messageBody",
				tempMessageBody					
			);
			this.formFields.put(
				"org:opencrx:kernel:base:Note:text",
				dataBinding.getValue(
					activityCreator, 
					":BulkCreateActivityWizardSettings." + locale + "!placeHolders"
				)
			);
			this.formFields.put(
				"org:opencrx:kernel:activity1:EMail:sender",
				dataBinding.getValue(
					activityCreator, 
					":BulkCreateActivityWizardSettings." + locale + "!sender"
				)
			);
			List<Short> usagesList = new ArrayList<Short>();
			if (needsInit) {
					usagesList.add(Short.valueOf((String)"500")); // default Usage: Business
			}
			for(int i = 0; i < 10; i++) {
				Object usage = dataBinding.getValue(
					activityCreator, 
					":BulkCreateActivityWizardSettings." + locale + "!usage." + i
				);
				if(usage != null) {
					usagesList.add(Short.valueOf((String)usage));
				}
			}
			this.formFields.put(
				"org:opencrx:kernel:address1:Addressable:usage",
				usagesList
			);
			this.doRefresh(
				isInitialized, 
				excludeNoBulkEMail, 
				this.formFields
			);
		}		
	}

	/**
	 * CountActivities action.
	 * 
	 * @param isInitialized
	 * @param excludeNoBulkEMail
	 * @param activityGroupXri
	 * @param formFields
	 * @throws ServiceException
	 */
	public void doCountActivities(
		@RequestParameter(name = "isInitialized") Boolean isInitialized,
		@RequestParameter(name = "excludeNoBulkEMail") Boolean excludeNoBulkEMail,
		@RequestParameter(name = "activityGroupXri") String activityGroupXri,
		@FormParameter(forms = {"BulkCreateActivityFormCreator", "BulkCreateActivityFormPlaceHolders", "BulkCreateActivityFormActivity", "BulkCreateActivityFormEMail", "BulkCreateActivityFormEMailTo", "BulkCreateActivityFormRecipient"}) Map<String,Object> formFields				
	) throws ServiceException {
		PersistenceManager pm = this.getPm();
		this.doRefresh(
			isInitialized,
			excludeNoBulkEMail,
			formFields
		);
		this.activityGroup = activityGroupXri != null && !activityGroupXri.isEmpty()
			? (ActivityGroup)pm.getObjectById(new Path(activityGroupXri))
			: null;
		if(this.activityGroup != null) {
	    	ActivityQuery query = (ActivityQuery)pm.newQuery(Activity.class);
	    	org.openmdx.base.query.Extension queryExtension = PersistenceHelper.newQueryExtension(query);
	    	queryExtension.setClause(
	    		Database_1_Attributes.HINT_COUNT + "(1=1)"
	    	);
	    	List<Activity> activities = this.activityGroup.getFilteredActivity(query);
	    	this.numberOfFilteredActivities = activities.size();
		}
	}

	/**
	 * DeleteActivities action.
	 * 
	 * @param isInitialized
	 * @param excludeNoBulkEMail
	 * @param activityGroupXri
	 * @param formFields
	 * @throws ServiceException
	 */
	public void doDeleteActivities(
		@RequestParameter(name = "isInitialized") Boolean isInitialized,
		@RequestParameter(name = "excludeNoBulkEMail") Boolean excludeNoBulkEMail,
		@RequestParameter(name = "activityGroupXri") String activityGroupXri,
		@FormParameter(forms = {"BulkCreateActivityFormCreator", "BulkCreateActivityFormPlaceHolders", "BulkCreateActivityFormActivity", "BulkCreateActivityFormEMail", "BulkCreateActivityFormEMailTo", "BulkCreateActivityFormRecipient"}) Map<String,Object> formFields				
	) throws ServiceException {
		PersistenceManager pm = this.getPm();
		this.doRefresh(
			isInitialized,
			excludeNoBulkEMail,
			formFields
		);
		this.activityGroup = activityGroupXri != null && !activityGroupXri.isEmpty()
			? (ActivityGroup)pm.getObjectById(new Path(activityGroupXri))
			: null;
		if(this.activityGroup != null) {
			Collection<Activity> activities = this.activityGroup.getFilteredActivity();
			pm.evictAll(activities);
			while(!activities.isEmpty()) {
				List<Path> activityIdentities = new ArrayList<Path>();
				for(Activity activity : activities) {
					activityIdentities.add(activity.refGetPath());
					if(activityIdentities.size() >= 100) {
						break;
					}
				}				
				try {
					pm.currentTransaction().begin();
					for(Path activityIdentity: activityIdentities) {
						((Activity)pm.getObjectById(activityIdentity)).refDelete();
					}
					pm.currentTransaction().commit();
				} catch (Exception e) {
					try {
						pm.currentTransaction().rollback();
					} catch(Exception ignore) {}
					new ServiceException(e).log();
					break;
				}
				pm.evictAll(activities);				
			}
		}
	}

	/**
	 * ConfirmDeleteActivities action.
	 * 
	 * @param isInitialized
	 * @param excludeNoBulkEMail
	 * @param activityGroupXri
	 * @param formFields
	 * @throws ServiceException
	 */
	public void doConfirmDeleteActivities(
		@RequestParameter(name = "isInitialized") Boolean isInitialized,
		@RequestParameter(name = "excludeNoBulkEMail") Boolean excludeNoBulkEMail,
		@RequestParameter(name = "activityGroupXri") String activityGroupXri,
		@FormParameter(forms = {"BulkCreateActivityFormCreator", "BulkCreateActivityFormPlaceHolders", "BulkCreateActivityFormActivity", "BulkCreateActivityFormEMail", "BulkCreateActivityFormEMailTo", "BulkCreateActivityFormRecipient"}) Map<String,Object> formFields				
	) throws ServiceException {
		PersistenceManager pm = this.getPm();
		this.doRefresh(
			isInitialized,
			excludeNoBulkEMail,
			formFields
		);
		this.activityGroup = activityGroupXri != null && !activityGroupXri.isEmpty()
			? (ActivityGroup)pm.getObjectById(new Path(activityGroupXri))
			: null;
		this.confirmDeleteActivities = true;
	}

	/**
	 * Cancel action.
	 * 
	 * @throws ServiceException
	 */
	public void doCancel(
	) throws ServiceException {
		this.setExitAction(
			new ObjectReference(this.getObject(), this.getApp()).getSelectObjectAction()
		);
	}

	/**
	 * @return the excludeNoBulkEMail
	 */
	public Boolean getExcludeNoBulkEMail(
	) {
		return this.excludeNoBulkEMail;
	}

	/**
	 * @return the activityCreator
	 */
	public ActivityCreator getActivityCreator(
	) {
		return this.activityCreator;
	}
	
	/**
	 * @return the canCreate
	 */
	public Boolean getCanCreate(
	) {
		return this.canCreate;
	}

	/**
	 * @return the creationType
	 */
	public CreationType getCreationType(
	) {
		return this.creationType;
	}
	
	/**
	 * @return the selectedAccountsOnly
	 */
	public Boolean getSelectedAccountsOnly(
	) {
		return this.selectedAccountsOnly;
	}

	/**
	 * @return the activityType
	 */
	public ActivityType getActivityType(
	) {
		return this.activityType;
	}

	/**
	 * @return the numberOfFilteredActivities
	 */
	public Integer getNumberOfFilteredActivities(
	) {
		return this.numberOfFilteredActivities;
	}

	/**
	 * @return the confirmDeleteActivities
	 */
	public Boolean getConfirmDeleteActivities(
	) {
		return this.confirmDeleteActivities;
	}

	/**
	 * @return the activityGroup
	 */
	public ActivityGroup getActivityGroup(
	) {
		return this.activityGroup;
	}

	/**
	 * @return the executionReport
	 */
	public List<String> getExecutionReport(
	) {
		return this.executionReport;
	}

	/**
	 * @return the formFields
	 */
	public Map<String, Object> getFormFields(
	) {
		return this.formFields;
	}

	/**
	 * Get view port.
	 * 
	 * @param out
	 * @return
	 */
	public ViewPort getViewPort(
		Writer out
	) {
		if(this.viewPort == null) {
			TransientObjectView view = new TransientObjectView(
				this.getFormFields(),
				this.getApp(),
				this.getObject(),
				this.getPm()
			);
			this.viewPort = ViewPortFactory.openPage(
				view,
				this.getRequest(),
				out
			);
		}
		return this.viewPort;
	}

	/* (non-Javadoc)
	 * @see org.openmdx.portal.servlet.AbstractWizardController#close()
	 */
    @Override
    public void close(
    ) throws ServiceException {
	    super.close();
	    if(this.viewPort != null) {
	    	this.viewPort.close(false);
	    }
    }

	//-----------------------------------------------------------------------
	// Members
	//-----------------------------------------------------------------------	
	public static final String CONTACT_CLASS = "org:opencrx:kernel:account1:Contact";
	public static final String TIMER_CLASS = "org:opencrx:kernel:home1:Timer";
	public static final int NUM_OF_TEST_ACTIVITIES = 3;

	private Map<String,Object> formFields;
	private ViewPort viewPort;
	private Boolean canCreate;
	private CreationType creationType;
	private Boolean selectedAccountsOnly;
	private ActivityGroup activityGroup;
	private Integer numberOfFilteredActivities;
	private Boolean confirmDeleteActivities;
	private Boolean excludeNoBulkEMail;
	private ActivityCreator activityCreator;
	private ActivityType activityType;
	private AccountAddress sender;
	private Number locale;
	private List<String> executionReport = null;
	private AccountFilterGlobal accountFilter;
	private org.opencrx.kernel.account1.jmi1.Group group;
	private org.opencrx.kernel.account1.jmi1.AddressFilterGlobal addressFilter;
	private org.opencrx.kernel.activity1.jmi1.AddressGroup addressGroup;
	
}
