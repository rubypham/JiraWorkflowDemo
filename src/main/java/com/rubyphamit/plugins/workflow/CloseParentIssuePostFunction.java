package com.rubyphamit.plugins.workflow;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.workflow.JiraWorkflow;
import com.atlassian.jira.workflow.WorkflowManager;
import com.atlassian.jira.workflow.function.issue.AbstractJiraFunctionProvider;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.WorkflowException;
import com.opensymphony.workflow.loader.ActionDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/*
This is the post-function class that gets executed at the end of the transition.
Any parameters that were saved in your factory class will be available in the transientVars Map.
 */

public class CloseParentIssuePostFunction extends AbstractJiraFunctionProvider{
    private static final Logger LOGGER = LoggerFactory.getLogger(CloseParentIssuePostFunction.class);

    private final WorkflowManager workflowManager;
    private final SubTaskManager subTaskManager;
    private final JiraAuthenticationContext authenticationContext;
    private final Status closedStatus;

    public CloseParentIssuePostFunction(ConstantsManager constantsManager, WorkflowManager workflowManager, SubTaskManager subTaskManager, JiraAuthenticationContext authenticationContext) {
        this.workflowManager = workflowManager;
        this.subTaskManager = subTaskManager;
        this.authenticationContext = authenticationContext;
        closedStatus = constantsManager.getStatus(Integer.toString(IssueFieldConstants.CLOSED_STATUS_ID));
    }

    // Move the specified issue to the 'CLOSED' status.
    private void closeIssue(Issue issue) throws WorkflowException
    {

        Status currentStatus = issue.getStatusObject();
        JiraWorkflow workflow = workflowManager.getWorkflow(issue);
        List<ActionDescriptor> actions = workflow.getLinkedStep(currentStatus).getActions();
        // look for the closed transition
        ActionDescriptor closeAction = null;
        for (ActionDescriptor descriptor : actions)
        {
            if (descriptor.getUnconditionalResult().getStatus().equals(closedStatus.getName()))
            {
                closeAction = descriptor;
                break;
            }
        }
        if (closeAction != null)
        {
            ApplicationUser currentUser =  authenticationContext.getLoggedInUser();
            IssueService issueService = ComponentAccessor.getIssueService();
            IssueInputParameters parameters = issueService.newIssueInputParameters();
            parameters.setRetainExistingValuesWhenParameterNotProvided(true);
            IssueService.TransitionValidationResult validationResult =  issueService.validateTransition(currentUser, issue.getId(), closeAction.getId(), parameters);
            IssueService.IssueResult result = issueService.transition(currentUser, validationResult);
        }
    }


    public void execute(Map transientVars, Map args, PropertySet ps)throws WorkflowException{

        // Retrieve the sub-task
        MutableIssue subTask=getIssue(transientVars);
        // Retrieve the parent issue
        MutableIssue parentIssue = ComponentAccessor.getIssueManager().getIssueObject(subTask.getParentId());

        // Ensure that the parent issue is not already closed
        if (IssueFieldConstants.CLOSED_STATUS_ID == Integer.parseInt(parentIssue.getStatusObject().getId()))
        {
            return;
        }

        // Check that ALL OTHER sub-tasks are closed
        Collection<Issue> subTasks = subTaskManager.getSubTaskObjects(parentIssue);

        for (Iterator<Issue> iterator = subTasks.iterator(); iterator.hasNext();)
        {
            Issue associatedSubTask =  iterator.next();
            if (!subTask.getKey().equals(associatedSubTask.getKey()))
            {
                // If other associated sub-task is still open - do not continue
                if (IssueFieldConstants.CLOSED_STATUS_ID != Integer.parseInt(associatedSubTask.getStatusObject().getId()))
                {
                    return;
                }
            }
        }

        // All sub-tasks are now closed - close the parent issue
        try
        {
            closeIssue(parentIssue);
        }
        catch (WorkflowException e)
        {
            LOGGER.error("Error occurred while closing the issue: " + parentIssue.getString("key") + ": " + e, e);
            e.printStackTrace();
        }
    }
}