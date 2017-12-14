package com.rubyphamit.plugins.workflow;

import com.atlassian.jira.issue.Issue;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.InvalidInputException;
import com.opensymphony.workflow.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class CloseIssueWorkflowValidator implements Validator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(CloseIssueWorkflowValidator.class);

    public void validate(Map transientVars, Map args, PropertySet ps) throws InvalidInputException
    {
        Issue issue = (Issue) transientVars.get("issue");
        // The issue must have a fixVersion otherwise you cannot close it
        if(null == issue.getFixVersions() || issue.getFixVersions().size() == 0)
        {
            throw new InvalidInputException("Issue must have a fix version");
        }
    }
}
