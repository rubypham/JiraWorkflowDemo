package com.rubyphamit.plugins.workflow;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.workflow.WorkflowFunctionUtils;
import com.atlassian.jira.workflow.condition.AbstractJiraCondition;
import com.opensymphony.module.propertyset.PropertySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.StringTokenizer;

public class ParentIssueBlockingCondition extends AbstractJiraCondition
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ParentIssueBlockingCondition.class);

    public boolean passesCondition(Map transientVars, Map args, PropertySet ps)
    {
        Issue subTask = (Issue) transientVars.get(WorkflowFunctionUtils.ORIGINAL_ISSUE_KEY);

        // Retrieve the parent issue
        Issue parentIssue = ComponentAccessor.getIssueManager().getIssueObject(subTask.getParentId());

        if (parentIssue == null)
        {
            return false;
        }

        // Comma separated list of status ids
        String statuses = (String) args.get("statuses");
        StringTokenizer st = new StringTokenizer(statuses, ",");

        // Check if the parent issue is associated with one the specified statuses.
        while(st.hasMoreTokens())
        {
            String statusId = st.nextToken();

            if (parentIssue.getStatusObject().getId().equals(statusId))
            {
                return true;
            }
        }
        return false;
    }
}
