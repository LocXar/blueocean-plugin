package io.jenkins.blueocean.service.embedded;

import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Job;
import io.jenkins.blueocean.rest.hal.Link;
import io.jenkins.blueocean.rest.hal.LinkResolver;
import io.jenkins.blueocean.service.embedded.rest.OrganizationImpl;
import io.jenkins.blueocean.service.embedded.rest.PipelineContainerImpl;
import io.jenkins.blueocean.service.embedded.rest.PipelineImpl;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;

import static org.eclipse.jgit.lib.ObjectChecker.parent;

/**
 * @author Kohsuke Kawaguchi
 */
public class LinkResolverImpl extends LinkResolver {
    @Override
    public Link resolve(Object modelObject) {
        Link orgLink = new Link("/rest/organizations/" + OrganizationImpl.INSTANCE.getName());

        if (modelObject instanceof Job) {
            Job job = (Job) modelObject;
            ItemGroup<? extends Item> parent = job.getParent();
            if (parent instanceof WorkflowMultiBranchProject) {
                String multiBranchProjectName = ((WorkflowMultiBranchProject) parent).getName();
                return orgLink.rel("pipelines").rel(multiBranchProjectName).rel("branches").rel(job.getName());
            } else {
                return orgLink.rel("pipelines").rel(job.getName());
            }
        }

        if (modelObject instanceof Job) {
            Job job = (Job) modelObject;
            return new PipelineContainerImpl(job).getLink();
        }

        return null;

    }
}
