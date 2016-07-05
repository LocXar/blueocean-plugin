package io.jenkins.blueocean.rest.model;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Item;
import io.jenkins.blueocean.rest.Reachable;
import jenkins.model.Jenkins;

/**
 * Factory that gives instance of {@link BluePipeline}
 *
 * It's useful for example in cases where a plugin that has custom project and they want to serve
 * extra meta-data thru BluePipeline, would provide implementation of their BluePipeline and and implementation
 * of BluePipelineFactory.
 *
 * @author Vivek Pandey
 */
public abstract class BluePipelineFactory implements ExtensionPoint {
    public abstract BluePipeline getPipeline(Item item, Reachable parent);

    /*
        invariants:
            context is ancestor of target
     */
    public abstract Resource resolve(Item context, Reachable parent, Item target);

    public static ExtensionList<BluePipelineFactory> all(){
        return ExtensionList.lookup(BluePipelineFactory.class);
    }

    public static Resource resolve(Item item, Reachable parent) {
        for (BluePipelineFactory f : all()) {
            f.resolve(Jenkins.getInstance(),parent,item);
        }
    }
}
