/*
 * ====================================================================
 * Project:     openCRX/Core, http://www.opencrx.org/
 * Description: SubscriptionHandlerServlet
 * Owner:       CRIXP AG, Switzerland, http://www.crixp.com
 * ====================================================================
 *
 * This software is published under the BSD license
 * as listed below.
 * 
 * Copyright (c) 2004-2012, CRIXP Corp., Switzerland
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
package org.opencrx.kernel.workflow.servlet;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jmi.reflect.RefObject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opencrx.kernel.backend.Accounts;
import org.opencrx.kernel.backend.Activities;
import org.opencrx.kernel.backend.Buildings;
import org.opencrx.kernel.backend.Contracts;
import org.opencrx.kernel.backend.Depots;
import org.opencrx.kernel.backend.Documents;
import org.opencrx.kernel.backend.Forecasts;
import org.opencrx.kernel.backend.Models;
import org.opencrx.kernel.backend.Products;
import org.opencrx.kernel.backend.UserHomes;
import org.opencrx.kernel.backend.UserHomes.TimerState;
import org.opencrx.kernel.backend.Workflows;
import org.opencrx.kernel.base.cci2.AuditEntryQuery;
import org.opencrx.kernel.base.jmi1.AuditEntry;
import org.opencrx.kernel.base.jmi1.Auditee;
import org.opencrx.kernel.base.jmi1.ExecuteWorkflowParams;
import org.opencrx.kernel.base.jmi1.ObjectCreationAuditEntry;
import org.opencrx.kernel.base.jmi1.ObjectModificationAuditEntry;
import org.opencrx.kernel.base.jmi1.ObjectRemovalAuditEntry;
import org.opencrx.kernel.base.jmi1.TestAndSetVisitedByParams;
import org.opencrx.kernel.base.jmi1.TestAndSetVisitedByResult;
import org.opencrx.kernel.generic.SecurityKeys;
import org.opencrx.kernel.home1.cci2.SubscriptionQuery;
import org.opencrx.kernel.home1.cci2.TimerQuery;
import org.opencrx.kernel.home1.jmi1.Subscription;
import org.opencrx.kernel.home1.jmi1.Timer;
import org.opencrx.kernel.home1.jmi1.UserHome;
import org.opencrx.kernel.utils.Utils;
import org.opencrx.kernel.workflow1.jmi1.Topic;
import org.opencrx.kernel.workflow1.jmi1.WfProcess;
import org.openmdx.base.exception.ServiceException;
import org.openmdx.base.jmi1.BasicObject;
import org.openmdx.base.jmi1.ContextCapable;
import org.openmdx.base.naming.Path;
import org.openmdx.base.persistence.cci.PersistenceHelper;
import org.openmdx.base.text.conversion.Base64;
import org.openmdx.kernel.exception.BasicException;
import org.openmdx.kernel.id.UUIDs;
import org.openmdx.kernel.log.SysLog;

/**
 * The SubscriptionHandlerServlet handles two use cases:
 * <ul>
 *   <li>It monitors object modifications by scanning the audit entries. If the modified object has a 
 *   matching topic, all subscriptions for this topic are handled by executing the configured workflow. 
 *   <li>It monitors timers and executes the assigned workflows.
 * </ul>
 */  
public class SubscriptionHandlerServlet 
    extends HttpServlet {

    /* (non-Javadoc)
     * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
     */
    public void init(
        ServletConfig config
    ) throws ServletException {

        super.init(config);
        // data connection
        try {
            this.pmf = Utils.getPersistenceManagerFactory();
        }
        catch(Exception e) {
            throw new ServletException("can not get connection to data provider", e);
        }
    }

    /**
     * @param auditEntry
     * @return
     */
    private Workflows.EventType getEventType(
        AuditEntry auditEntry
    ) {
      return auditEntry instanceof ObjectRemovalAuditEntry ? 
        	Workflows.EventType.OBJECT_REMOVAL : 
        		auditEntry instanceof ObjectCreationAuditEntry ? 
        			Workflows.EventType.OBJECT_CREATION : 
        				auditEntry instanceof ObjectModificationAuditEntry ? 
        					Workflows.EventType.OBJECT_REPLACEMENT : 
        						Workflows.EventType.NONE;
    }

    /**
     * @param subscription
     * @param eventType
     * @return
     */
    private boolean subscriptionAcceptsEventType(
        Subscription subscription,
        Short eventType
    ) {
        if(
            (subscription.getEventType() == null) || 
            (subscription.getEventType().isEmpty())
        ) {
            return true;
        }
        for(
            Iterator<Short> i = subscription.getEventType().iterator();
            i.hasNext();
        ) {
            Short e = i.next();
            if((e != null) && (eventType.compareTo(e) == 0)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * @param filterName
     * @param filterValue
     * @param message
     * @return
     */
    @SuppressWarnings("unchecked")
    protected boolean testFilterValue(
        String filterName,
        String filterValue,
        Object message
    ) {
        // Filter name and value must be defined
        if((filterName != null) && (filterName.length() > 0)) { 
            Object messageValue = null;
            if(message instanceof RefObject) {
                try {
                    messageValue = ((RefObject)message).refGetValue(filterName);
                }
                catch(Exception e) {
                	SysLog.warning("Can not get filter value", e.getMessage());
                }
            }
            else {
                String messageAsString = message.toString();
                String indexedFilterName = filterName + ":\n0: ";
                int pos = -1;
                if((pos = messageAsString.indexOf(indexedFilterName)) >= 0) {
                    int start = pos + indexedFilterName.length();
                    int end = messageAsString.indexOf(
                        "\n", 
                        start
                    );
                    if(end > start) {
                        messageValue = messageAsString.substring(start, end);
                    }
                }                
            }
            Collection<Object> messageValues = Collections.emptyList();
            if(messageValue instanceof Collection) {
                messageValues = (Collection<Object>)messageValue;
            }
            else if(messageValue != null) {
            	messageValues = Arrays.asList(messageValue);
            }
            boolean matches = false;
            boolean negate = false;
            if(filterValue != null && filterValue.startsWith("!")) {
            	filterValue = filterValue.substring(1);
            	negate = true;
            }
            try {
	            for(Object v: messageValues) {
	            	boolean isEqual =  filterValue == null ?
	            		v == filterValue :
	            		v instanceof RefObject ? 
	            			((RefObject)v).refMofId().equals(filterValue) : 
	            			v.toString().equals(filterValue);		            			
	            	matches |= negate ? !isEqual : isEqual;
	            }
            } 
            catch(Exception e) {
            	SysLog.detail(e.getMessage(), e.getCause());
            	SysLog.warning("Can not get filter value", Arrays.asList(filterName, e.getMessage()));            	
            }
            return matches;
        }
        return true;
    }
    
    /**
     * @param subscription
     * @param auditEntry
     * @return
     */
    public boolean subscriptionAcceptsMessage(
        Subscription subscription,
        AuditEntry auditEntry
    ) {
        // Verify active flag and event type
        Workflows.EventType eventType = this.getEventType(auditEntry); 
        if(!this.subscriptionAcceptsEventType(subscription, eventType.getValue())) {
            return false;
        }
        /**
         * Check security. 
         * <ul>
         *   <li>If auditee is composite to a user's home only accept if user homes of 
         *       the subscription and the auditee are identical.
         *   <li>If the owner of the subscription has read-permission for auditee.
         * </ul>
         */
        Object auditee = null; 
        if(auditEntry.getAuditee() != null) {
            Path auditeeIdentity = new Path(auditEntry.getAuditee());        
            Path userHomeIdentity = new Path(subscription.refMofId()).getParent().getParent();
            // Check identity of user homes
            if(
                (auditeeIdentity.size() >= PATH_PATTERN_USER_HOME.size()) &&
                auditeeIdentity.getPrefix(PATH_PATTERN_USER_HOME.size()).isLike(PATH_PATTERN_USER_HOME)
            ) {
                if(!auditeeIdentity.startsWith(userHomeIdentity)) {
                    return false;
                }
            }
            // Retrieve auditee in the security context of the home's principal. 
            // This validates availability and read-permissions.
            String principalName = userHomeIdentity.getBase();
            PersistenceManager pm = this.pmf.getPersistenceManager(
                principalName,
                null
            );
            if(auditEntry instanceof ObjectModificationAuditEntry) {
                try {
                    auditee = pm.getObjectById(
                        new Path(auditEntry.getAuditee())
                    );
                }
                catch(Exception e) {
                	SysLog.detail(e.getMessage(), e.getCause());
                }
            }
            else if(auditEntry instanceof ObjectRemovalAuditEntry) {
                auditee = ((ObjectRemovalAuditEntry)auditEntry).getBeforeImage();
            }
            else if(auditEntry instanceof ObjectCreationAuditEntry) {
                try {
                    auditee = pm.getObjectById(
                        new Path(auditEntry.getAuditee())
                    );
                }
                catch(Exception e) {
                	SysLog.detail(e.getMessage(), e.getCause());
                }
            }   
        }
        if(auditee == null) return false;
        
        // Prepare filter names and filter values
        List<String> filterNames = new ArrayList<String>();
        List<Set<String>> filterValues = new ArrayList<Set<String>>();
        if((subscription.getFilterName0() != null) && (subscription.getFilterName0().length() > 0)) {
            filterNames.add(subscription.getFilterName0());
            filterValues.add(subscription.getFilterValue0());
        }
        if((subscription.getFilterName1() != null) && (subscription.getFilterName1().length() > 0)) {
            filterNames.add(subscription.getFilterName1());
            filterValues.add(subscription.getFilterValue1());
        }
        if((subscription.getFilterName2() != null) && (subscription.getFilterName2().length() > 0)) {
            filterNames.add(subscription.getFilterName2());
            filterValues.add(subscription.getFilterValue2());
        }
        if((subscription.getFilterName3() != null) && (subscription.getFilterName3().length() > 0)) {
            filterNames.add(subscription.getFilterName3());
            filterValues.add(subscription.getFilterValue3());
        }
        if((subscription.getFilterName4() != null) && (subscription.getFilterName4().length() > 0)) {
            filterNames.add(subscription.getFilterName4());
            filterValues.add(subscription.getFilterValue4());
        }
        // Test values
        boolean acceptsMessage = true;
        for(int i = 0; i < filterNames.size(); i++) {     
            boolean acceptsValues = false;
            for(Iterator<String> j = filterValues.get(i).iterator(); j.hasNext(); ) {
                acceptsValues |= this.testFilterValue(
                    filterNames.get(i),
                    j.next(),
                    auditee
                );
            }
            acceptsMessage &= acceptsValues;
            if(!acceptsMessage) break;
        }
        return acceptsMessage;
    }
        
    /**
     * @param providerName
     * @param segmentName
     * @param topic
     * @param objectXri
     * @return
     */
    public boolean topicAcceptsObject(
        String providerName,
        String segmentName,
        Topic topic,
        String objectXri
    ) {
        String topicPatternXri = topic.getTopicPathPattern();
        if(topicPatternXri != null) {
            Path topicPattern = new Path(topicPatternXri);
            Path objectPath = new Path(objectXri);
            if(topicPattern.size() < 7) {
                return false;
            }
            else {
                return 
                    objectPath.isLike(topicPattern) &&
                    objectPath.get(2).equals(providerName) &&
                    objectPath.get(4).equals(segmentName);
            }
        }
        else {
            return false;
        }
    }        
    
    /**
     * @param providerName
     * @param segmentName
     * @param workflowSegment
     * @param userHomeSegment
     * @param auditEntry
     * @return
     * @throws ServiceException
     */
    private List<Subscription> findSubscriptions(
        String providerName,
        String segmentName,
        org.opencrx.kernel.workflow1.jmi1.Segment workflowSegment,
        org.opencrx.kernel.home1.jmi1.Segment userHomeSegment,
        AuditEntry auditEntry
    ) throws ServiceException {
    	PersistenceManager pm = JDOHelper.getPersistenceManager(workflowSegment);
        // Find topics matching the auditee (modified, created or removed object)
        List<Topic> matchingTopics = new ArrayList<Topic>();
        Collection<Topic> topics = workflowSegment.getTopic();
        for(Topic topic: topics) {
            if(this.topicAcceptsObject(providerName, segmentName, topic, auditEntry.getAuditee())) {
                matchingTopics.add(topic);
            }            
        }
        // Find subscriptions for this topic
        List<Subscription> matchingSubscriptions = null;
        if(!matchingTopics.isEmpty()) {
            matchingSubscriptions = new ArrayList<Subscription>();
        	SubscriptionQuery query = (SubscriptionQuery)PersistenceHelper.newQuery(
        		pm.getExtent(Subscription.class),
                new Path("xri://@openmdx*org.opencrx.kernel.home1/provider").getDescendant(providerName, "segment", segmentName, "userHome", ":*", "subscription", ":*")  
        	);
            query.thereExistsTopic().elementOf(matchingTopics);
            query.isActive().isTrue();
            Collection<Subscription> subscriptions = userHomeSegment.getExtent(query);
            for(Subscription subscription: subscriptions) {
                if(this.subscriptionAcceptsMessage(subscription, auditEntry)) {
                    matchingSubscriptions.add(subscription);
                }
            }
        }
        return matchingSubscriptions;
    }

    /**
     * Execute workflows. A workflow instance is created for each
     * executed workflow. Synchronous workflows are executed immediately 
     * if startedOn and lastActivityOn are set. Pending workflow instances,
     * i.e. asynchronous and non-successful synchronous workflows are handled 
     * by the WorkflowControllerServlet.
     * 
     * @param pmUser
     * @param triggeredBy
     * @param targetObject
     * @param wfProcesses
     * @throws ServiceException
     */
    protected void executeWorkflows(
    	UserHome userHome,
    	Path targetObjectIdentity,
    	Path triggeredByIdentity,
    	Workflows.EventType triggeredByEventType,
    	Collection<WfProcess> wfProcesses
    ) throws ServiceException {
    	PersistenceManager pm = JDOHelper.getPersistenceManager(userHome);
    	PersistenceManager pmUser = null;
    	try {
	        pmUser = pm.getPersistenceManagerFactory().getPersistenceManager(
	        	userHome.refGetPath().getBase(), 
	        	null
	        );
            ContextCapable targetObject = null;
            try {
            	targetObject = (ContextCapable)pmUser.getObjectById(targetObjectIdentity);
            } catch(Exception e) {}
            ContextCapable triggeredBy = null;
            try {
            	triggeredBy = (ContextCapable)pmUser.getObjectById(triggeredByIdentity);
            } catch(Exception e) {}
            // In case user has no access to target object (NO_PERMISSION, ...) ignore subscription
            if(targetObject instanceof BasicObject && triggeredBy instanceof BasicObject) {
		        for(WfProcess wfProcess: wfProcesses) {
		            try {
		                MessageDigest md = MessageDigest.getInstance("MD5");
		                md.update(targetObjectIdentity.toXRI().getBytes("UTF-8"));
		                md.update(wfProcess.refMofId().getBytes("UTF-8"));                                        
		                ExecuteWorkflowParams params = Utils.getBasePackage(pmUser).createExecuteWorkflowParams(
		                    null, // startedAt
		                    (BasicObject)targetObject, // targetObject
		                    (BasicObject)pmUser.getObjectById(triggeredBy.refGetPath()), // triggeredBy
		                    Base64.encode(md.digest()).replace('/', '-'), // triggeredByEventId
		                    new Integer(triggeredByEventType.getValue()), // triggeredByEventType
		                    (WfProcess)pmUser.getObjectById(wfProcess.refGetPath())
		                );
		                try {
		                    pmUser.currentTransaction().begin();
		                    ((UserHome)pmUser.getObjectById(
		                    	userHome.refGetPath())
		                    ).executeWorkflow(params);
		                    // executeWorkflow touches userHome in separate uow. Prevent concurrent modification exception                                            
		                    pmUser.refresh(userHome);
		                    pmUser.currentTransaction().commit();
		                }
		                catch(Exception e) {
		                	ServiceException e0 = new ServiceException(e);
		                	SysLog.warning("Execution of workflow FAILED", "action=" + (wfProcess == null ? null : wfProcess.getName()) + "; home=" + (userHome == null ? null : userHome.refMofId()) + "; cause=" + (e0.getCause() == null ? null : e0.getCause().getMessage()));
		                	if(e0.getExceptionCode() == BasicException.Code.NOT_FOUND) {
		                		e0.log(); // log at WARNING level
		                	} else {
		                		SysLog.detail(e.getMessage(), e.getCause());
		                	}
		                    try {
		                        pmUser.currentTransaction().rollback();
		                    } catch(Exception e1) {}
		                }
		            }
		            catch(NoSuchAlgorithmException e) {
		                new ServiceException(e).log();
		            }
		            catch (UnsupportedEncodingException e) {
		                new ServiceException(e).log();
		            }
		        }
	        }
    	} finally {
    		if(pmUser != null) {
    			pmUser.close();
    		}
    	}
    }

    /**
     * @param providerName
     * @param segmentName
     * @param pm
     * @param workflowSegment
     * @param userHomeSegment
     * @param auditEntries
     * @throws ServiceException
     */
    private void handleSubscriptions(
        String providerName,
        String segmentName,
        PersistenceManager pm,
        org.opencrx.kernel.workflow1.jmi1.Segment workflowSegment,
        org.opencrx.kernel.home1.jmi1.Segment userHomeSegment,
        List<AuditEntry> auditEntries
    ) throws ServiceException {
        
        List<String> auditEntryXris = new ArrayList<String>();
        for(AuditEntry auditEntry: auditEntries) {
            auditEntryXris.add(auditEntry.refMofId());
            if(auditEntryXris.size() > BATCH_SIZE) break;
        }
        for(String auditEntryXri: auditEntryXris) {
            AuditEntry auditEntry = null;
            try {
                auditEntry = (AuditEntry)pm.getObjectById(new Path(auditEntryXri));
            } 
            catch(Exception e) {
            	SysLog.warning("Can not access audit entry", Arrays.asList(new String[]{auditEntryXri, e.getMessage()}));
            	SysLog.detail(e.getMessage(), e.getCause());
            }
            if(auditEntry != null) {
                TestAndSetVisitedByResult markAsVisistedReply = null;
                try {
                    TestAndSetVisitedByParams params = Utils.getBasePackage(pm).createTestAndSetVisitedByParams(
                        VISITOR_ID
                    );
                    markAsVisistedReply = auditEntry.testAndSetVisitedBy(params);
                }
                catch(Exception e) {
                	SysLog.error("Can not invoke markAsVisited", e.getMessage());
                    ServiceException e0 = new ServiceException(e);
                    SysLog.error(e0.getMessage(), e0.getCause());
                }
                if(
                    (markAsVisistedReply != null) &&
                    (markAsVisistedReply.getVisitStatus() == 0)
                ) {
                    List<Subscription> subscriptions = this.findSubscriptions(
                        providerName,
                        segmentName,
                        workflowSegment,
                        userHomeSegment,
                        auditEntry
                    );
                    if(subscriptions != null) {
                        for(Subscription subscription: subscriptions) {
                        	Path userHomeIdentity = subscription.refGetPath().getParent().getParent();
                            UserHome userHome = (UserHome)pm.getObjectById(userHomeIdentity);
                            org.opencrx.security.realm1.jmi1.User user = userHome.getOwningUser();
                            boolean userIsDisabled = false;
                            // Invalid NULLs on the DB may throw a NullPointer. Ignore.
                            try {
                                userIsDisabled = user.isDisabled();
                            } catch(Exception e) {}
                            if(!userIsDisabled) {
                                Collection<WfProcess> actions = subscription.getTopic().getPerformAction();
                                this.executeWorkflows(
                                	userHome, 
                                	new Path(auditEntry.getAuditee()),
                                	subscription.refGetPath(), 
                                	this.getEventType(auditEntry),
                                	actions
                                );
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * @param id
     * @param providerName
     * @param segmentName
     * @param req
     * @param res
     * @throws IOException
     */
    public void handleSubscriptions(
        String id,
        String providerName,
        String segmentName,
        HttpServletRequest req, 
        HttpServletResponse res        
    ) throws IOException {
        System.out.println(new Date().toString() + ": " + WORKFLOW_NAME + "[Subscriptions] " + providerName + "/" + segmentName);
        try {
            PersistenceManager pm = this.pmf.getPersistenceManager(
                SecurityKeys.ADMIN_PRINCIPAL + SecurityKeys.ID_SEPARATOR + segmentName,
                null
            );
            Workflows.getInstance().initWorkflows(
                pm,
                providerName,
                segmentName                
            );            
            // Get auditees
            List<Auditee> auditSegments = new ArrayList<Auditee>();
            auditSegments.add(Accounts.getInstance().getAccountSegment(pm, providerName, segmentName));
            auditSegments.add(Activities.getInstance().getActivitySegment(pm, providerName, segmentName));
            auditSegments.add(Buildings.getInstance().getBuildingSegment(pm, providerName, segmentName));
            auditSegments.add(Contracts.getInstance().getContractSegment(pm, providerName, segmentName));
            auditSegments.add(Depots.getInstance().getDepotSegment(pm, providerName, segmentName));
            auditSegments.add(Documents.getInstance().getDocumentSegment(pm, providerName, segmentName));
            auditSegments.add(Forecasts.getInstance().getForecastSegment(pm, providerName, segmentName));
            auditSegments.add(Models.getInstance().getModelSegment(pm, providerName, segmentName));
            auditSegments.add(Products.getInstance().getProductSegment(pm, providerName, segmentName));
            auditSegments.add(UserHomes.getInstance().getUserHomeSegment(pm, providerName, segmentName));
                                
            // Workflow segment
            org.opencrx.kernel.workflow1.jmi1.Segment workflowSegment = Workflows.getInstance().getWorkflowSegment(pm, providerName, segmentName);
            // User home segment
            org.opencrx.kernel.home1.jmi1.Segment userHomeSegment = UserHomes.getInstance().getUserHomeSegment(pm, providerName, segmentName);
            // Iterate all auditees and check for new audit entries
            for(Auditee auditee: auditSegments) {
                AuditEntryQuery query = (AuditEntryQuery)pm.newQuery(AuditEntry.class);
                // Not visited elements are marked with VISITOR_ID:-
                // Visited elements are marked with VISITOR_ID:<time stamp of visit>
                query.thereExistsVisitedBy().equalTo(
                    VISITOR_ID + ":-"
                );
                query.orderByCreatedAt().ascending();
                try {
                    List<AuditEntry> auditEntries = auditee.getAudit(query);
                    this.handleSubscriptions(   
                        providerName,
                        segmentName,
                        pm,
                        workflowSegment,
                        userHomeSegment,
                        auditEntries
                    );
                }
                catch(Exception e) {
                    new ServiceException(e).log();                    
                    System.out.println(new Date() + ": " + WORKFLOW_NAME + " " + providerName + "/" + segmentName + ": exception occured " + e.getMessage() + ". Continuing");                    
                }
            }
            try {
                pm.close();
            } 
            catch(Exception e) {}
        }
        catch(Exception e) {
            new ServiceException(e).log();
            System.out.println(new Date() + ": " + WORKFLOW_NAME + " " + providerName + "/" + segmentName + ": exception occured " + e.getMessage() + ". Continuing");
        }        
    }
    
    /**
     * @param id
     * @param providerName
     * @param segmentName
     * @param req
     * @param res
     * @throws IOException
     */
    public void handleTimers(
        String id,
        String providerName,
        String segmentName,
        HttpServletRequest req, 
        HttpServletResponse res        
    ) throws IOException {
        
        System.out.println(new Date().toString() + ": " + WORKFLOW_NAME + "[Timers] " + providerName + "/" + segmentName);

        PersistenceManager pmAdmin = null;
        try {
            pmAdmin = this.pmf.getPersistenceManager(
                SecurityKeys.ADMIN_PRINCIPAL + SecurityKeys.ID_SEPARATOR + segmentName,
                null
            );
            // Get all timers with no trigger since trigger_interval_minutes until min(trigger_end_at, trigger_start_at + trigger_repeat * trigger_interval_minutes)
            final String[] TIMER_CLAUSES = {

            	// Use fn timestampadd (JDBC, SQL-92)
            	"(current_timestamp BETWEEN " +
            	"{fn timestampadd(SQL_TSI_MINUTE, trigger_interval_minutes, last_trigger_at)} AND " + 
            	"LEAST(timer_end_at, {fn timestampadd(SQL_TSI_MINUTE, trigger_repeat * trigger_interval_minutes, timer_start_at)}))",

            	// Use fn timestampadd, use CASE instead of LEAST (SQL Server, ...)
            	"(current_timestamp BETWEEN " +
            	"{fn timestampadd(SQL_TSI_MINUTE, trigger_interval_minutes, last_trigger_at)} AND " + 
            	"CASE WHEN timer_end_at < {fn timestampadd(SQL_TSI_MINUTE, trigger_repeat * trigger_interval_minutes, timer_start_at)} THEN timer_end_at ELSE {fn timestampadd(SQL_TSI_MINUTE, trigger_repeat * trigger_interval_minutes, timer_start_at)} END)",

            	// Use + ... MINUTES instead of fn timestampadd  (DB2, ...)
            	"(current_timestamp BETWEEN " +
            	"last_trigger_at + trigger_interval_minutes MINUTES AND " + 
            	"LEAST(timer_end_at, timer_start_at + (trigger_repeat * trigger_interval_minutes) MINUTES))",

            	// Use numtodsinterval instead of fn timestampadd (Oracle, ...)
            	"(current_timestamp BETWEEN " +
            	"last_trigger_at + numtodsinterval(trigger_interval_minutes, 'minute') AND " + 
            	"LEAST(timer_end_at, timer_start_at + numtodsinterval(trigger_repeat * trigger_interval_minutes, 'minute')))",

            };
            List<Path> matchingTimerIdentities = new ArrayList<Path>();
            Exception queryError = null;
            timerClause: for(String clause: TIMER_CLAUSES) {
	    		// Timers have path pattern xri://@openmdx*org.opencrx.kernel.home1/provider/:*/segment/:*/userHome/:*/timer/:*
	            TimerQuery timerQuery = (TimerQuery)PersistenceHelper.newQuery(
	            	pmAdmin.getExtent(Timer.class), 
	            	new Path("xri://@openmdx*org.opencrx.kernel.home1").getDescendant("provider", providerName, "segment", segmentName, "userHome", ":*", "timer", ":*")
	            );
	            timerQuery.forAllDisabled().isFalse();
	            timerQuery.timerState().equalTo(TimerState.OPEN.getValue());
	            org.openmdx.base.query.Extension timerQueryExtension = PersistenceHelper.newQueryExtension(timerQuery);
	            timerQueryExtension.setClause(clause);
	            org.opencrx.kernel.home1.jmi1.Segment userHomeSegment = UserHomes.getInstance().getUserHomeSegment(pmAdmin, providerName, segmentName);
	            Collection<Timer> timers = userHomeSegment.getExtent(timerQuery);
	            try {
		            int count = 0;
		            for(Timer timer: timers) {
		            	matchingTimerIdentities.add(timer.refGetPath());
		            	count++;
		            	if(count > BATCH_SIZE) break;
		            }
		            queryError = null;
		            break;
	            } catch(Exception e) {
	            	ServiceException e0 = new ServiceException(e);
	            	SysLog.detail(e0.getMessage(), e0.getCause());            	
	            	queryError = e;
	            	continue timerClause;
	            }
            }
            if(queryError != null)  {
            	SysLog.log(Level.WARNING, "Unable to retrieve pending timers. For more info see log at level " + Level.FINE);
            } else {
	            for(Path timerIdentity: matchingTimerIdentities) {
	            	PersistenceManager pm = null;
	            	try {
	            		pm = this.pmf.getPersistenceManager(
	                        timerIdentity.get(6), // userId
	                        null
	                    );
	            		Timer timer = (Timer)pm.getObjectById(timerIdentity);
	            		pm.currentTransaction().begin();
	            		timer.setLastTriggerAt(new Date());
	            		// In case the timer triggers exactly one time it can be closed now.
	            		if(timer.getTriggerRepeat() != null &&  timer.getTriggerRepeat() == 1) {
	            			timer.setTimerState((short)UserHomes.TimerState.CLOSED.getValue());
	            		}
	            		pm.currentTransaction().commit();
	                	Path userHomeIdentity = timer.refGetPath().getParent().getParent();
	                    UserHome userHome = (UserHome)pm.getObjectById(userHomeIdentity);            		
	            		this.executeWorkflows(
	            			userHome, 
	            			timer.getTarget() == null ? null : timer.getTarget().refGetPath(), // targetObject
	            			timerIdentity, // triggerBy
	            			Workflows.EventType.TIMER, // triggerByEvent
	            			timer.<WfProcess>getAction() // actions
	            		);
	            	} catch(Exception e) {
	            		new ServiceException(e).log();
	            		try {
	            			pm.currentTransaction().rollback();
	            		} catch(Exception e0) {}
	            	} finally {
	            		try {
	            			pm.close();
	            		} catch(Exception e) {}
	            	}
	            }
            }
        } catch(Exception e) {
            new ServiceException(e).log();
            System.out.println(new Date() + ": " + WORKFLOW_NAME + " " + providerName + "/" + segmentName + ": exception occured " + e.getMessage() + ". Continuing");
            try {
            	pmAdmin.currentTransaction().rollback();
            } catch(Exception e0) {}
        } finally {
            try {
                pmAdmin.close();
            } catch(Exception e) {}
        }
    }

    /**
     * @param req
     * @param res
     * @throws ServletException
     * @throws IOException
     */
    protected void handleRequest(
        HttpServletRequest req, 
        HttpServletResponse res
    ) throws ServletException, IOException {
        if(System.currentTimeMillis() > this.startedAt + 180000L) {
            String segmentName = req.getParameter("segment");
            String providerName = req.getParameter("provider");
            String id = providerName + "/" + segmentName;
            // Run
            if(COMMAND_EXECUTE.equals(req.getPathInfo())) {
                if(!runningSegments.containsKey(id)) {
	                try {
	                    runningSegments.put(
	                    	id,
	                    	Thread.currentThread()
	                    );
	                    this.handleTimers(
	                    	id,
	                    	providerName,
	                    	segmentName,
	                    	req,
	                    	res
	                    );
	                    this.handleSubscriptions(
	                        id,
	                        providerName,
	                        segmentName,
	                        req,
	                        res
	                    );
	                }
	                catch(Exception e) {
	                    new ServiceException(e).log();
	                }
	                finally {
	                    runningSegments.remove(id);
	                }
            	}
            	else if(
            		!runningSegments.get(id).isAlive() || 
            		runningSegments.get(id).isInterrupted()
            	) {
	            	Thread t = runningSegments.get(id);
            		System.out.println(new Date() + ": " + WORKFLOW_NAME + " " + providerName + "/" + segmentName + ": workflow " + t.getId() + " is alive=" + t.isAlive() + "; interrupted=" + t.isInterrupted() + ". Skipping execution.");
            	}            	
            }
        }
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doGet(
        HttpServletRequest req, 
        HttpServletResponse res
    ) throws ServletException, IOException {
        res.setStatus(HttpServletResponse.SC_OK);
        res.flushBuffer();
        this.handleRequest(
            req,
            res
        );
    }
        
    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doPost(
        HttpServletRequest req, 
        HttpServletResponse res
    ) throws ServletException, IOException {
        res.setStatus(HttpServletResponse.SC_OK);
        res.flushBuffer();
        this.handleRequest(
            req,
            res
        );
    }

    //-----------------------------------------------------------------------
    // Members
    //-----------------------------------------------------------------------
    private static final long serialVersionUID = 7074135054692868453L;
    private static final int BATCH_SIZE = 50;
    
    private static final String WORKFLOW_NAME = "SubscriptionHandler";    
    private static final Path PATH_PATTERN_USER_HOME = new Path("xri://@openmdx*org.opencrx.kernel.home1").getDescendant("provider", ":*", "segment", ":*", "userHome", ":*");
    private static final String COMMAND_EXECUTE = "/execute";
    private static final String VISITOR_ID = "SubscriptionHandler";
    private static final Map<String,Thread> runningSegments = new ConcurrentHashMap<String,Thread>();

    private PersistenceManagerFactory pmf = null;
    private long startedAt = System.currentTimeMillis();
    
}

//--- End of File -----------------------------------------------------------
