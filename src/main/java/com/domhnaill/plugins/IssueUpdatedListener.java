package com.domhnaill.plugins;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.event.ChangeListener;

import com.atlassian.jira.component.*;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.core.util.map.EasyMap;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.mail.Email;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.mail.MailException;
import com.atlassian.mail.MailFactory;
import com.atlassian.mail.server.SMTPMailServer;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.opensymphony.workflow.util.IsUserOwnerCondition;

import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Simple JIRA listener using the atlassian-event library and demonstrating
 * plugin lifecycle integration.
 */
public class IssueUpdatedListener implements InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(IssueUpdatedListener.class);

    private final EventPublisher eventPublisher;
    private final IssueService issueService;
    private final JiraAuthenticationContext authenticationContext;
    private IssueService.UpdateValidationResult updateValidationResult;
    private final WebResourceManager webResourceManager;
    

    /**
     * Constructor.
     * @param eventPublisher injected {@code EventPublisher} implementation.
     */
    public IssueUpdatedListener(EventPublisher eventPublisher, WebResourceManager webResourceManager, IssueService issueService, JiraAuthenticationContext authenticationContext ) {
        this.eventPublisher = eventPublisher;
        this.webResourceManager = webResourceManager;
        this.issueService = issueService;
        this.authenticationContext = authenticationContext;
    }

    /**
     * Called when the plugin has been enabled.
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        // register ourselves with the EventPublisher
        eventPublisher.register(this);
    }

    /**
     * Called when the plugin is being disabled or removed.
     * @throws Exception
     */
    @Override
    public void destroy() throws Exception {
        // unregister ourselves with the EventPublisher
        eventPublisher.unregister(this);
    }

    /**
     * Receives any {@code IssueEvent}s sent by JIRA.
     * @param issueEvent the IssueEvent passed to us
     */
    @EventListener
    public void onIssueEvent(IssueEvent issueEvent) {

    	ApplicationProperties ap = ComponentAccessor.getApplicationProperties();
    	Long eventTypeId = issueEvent.getEventTypeId();
        Issue issue = issueEvent.getIssue();
        String author = issueEvent.getUser().getName();
        String emailAddress = issueEvent.getUser().getEmailAddress();
 
        // if it's an event we're interested in, log it
        if (eventTypeId.equals(EventType.ISSUE_UPDATED_ID) & issue.getPriorityObject().getName().equals("Blocker")) {
            //log.info("Issue {} has priority of {}.", issue.getKey(), issue.getPriorityObject().getName());
        	List<GenericValue> changeItems = null;
        	
        	try {
        		GenericValue changeLog = issueEvent.getChangeLog();
        		changeItems = changeLog.internalDelegator.findByAnd("ChangeItem", EasyMap.build("group",changeLog.get("id")));
        		SMTPMailServer mailServer = MailFactory.getServerManager().getDefaultSMTPMailServer();
        		
        		Email email = new Email(emailAddress);
        		email.setMimeType("text/html");
        		email.setEncoding("utf-8");
        		email.setBody("Priority Updated on " + issue.getKey() + ", Please Edit Comment.");
        		try {
					mailServer.send(email);
				} catch (MailException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	} catch (GenericEntityException e){
        		System.out.println(e.getMessage());
        	}
        	
        	log.info("number of changes: {}",changeItems.size());
        	
        	// Check if the priority field was changed and trigger action for that.
        	for (int i = 0; i < changeItems.size(); i++ ){
        		GenericValue item = changeItems.get(i);
        		if (item.containsValue("priority")){
        			try {
        				ComponentAccessor.getCommentManager().create(issue, author, "PRIORITY UPDATED TO BLOCKER, PLEASE EDIT ME", true);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
        			log.info("Priority was changed!!!!");
        			log.info("Item is : {}", item);
        		}
        	}
        } 
    }
}
