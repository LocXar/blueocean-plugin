package io.jenkins.blueocean.service.embedded.rest;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Item;
import hudson.model.ItemGroup;
import io.jenkins.blueocean.rest.Reachable;
import io.jenkins.blueocean.rest.model.BluePipeline;
import io.jenkins.blueocean.rest.model.Resource;
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

    /**
     * Finds a blue ocean API model object that pairs up with the given {@code target},
     * by looking at the intermediate core model object {@code context} that is an ancestor
     * of {@code target}.
     *
     * If this {@link BluePipelineFactory} understands how to map {@code context} to
     * {@link BluePipeline} (as in {@code getPipeline(item,parent)!=null}), then the resolve
     * method should also apply the same logic to map {@code context} and then recursively
     * resolve {@code target}.
     *
     * @param context
     *      This is always an ancestor of target (including target==context)
     * @param parent
     *      The parent object of the blue ocean API model object that pairs up with the 'context' parameter
     * @param target
     *      The core model object that we are trying to map to {@link Resource}
     *
     * @return
     *      null if this implementation doesn't know how to map {@code context} to a blue ocean API model object.
     *      Otherwise return the BO API model object that pairs up with {@code target}
     */
    public abstract Resource resolve(Item context, Reachable parent, Item target);

    public static ExtensionList<BluePipelineFactory> all(){
        return ExtensionList.lookup(BluePipelineFactory.class);
    }

    /**
     * Given a Job in Jenkins, map that to corresponding blue ocean API object,
     * for example so that you can get its URL.
     */
    public static Resource resolve(Item item) {
        for (BluePipelineFactory f : all()) {
            Resource r = f.resolve(findNextStep(Jenkins.getInstance(), item), OrganizationImpl.INSTANCE.getPipelines(), item);
            if (r!=null)    return r;
        }
        return null;
    }

    /**
     * Returns the immediate child of 'context' that is also the ancestor of 'target'
     */
    protected static Item findNextStep(ItemGroup context, Item target) {
        Item i = null;
        while (context!=target) {
            i = target;
            if (target.getParent() instanceof Item) {
                target = (Item) target.getParent();
            } else {
                throw new AssertionError("context was supposed to be a parent of target");
            }
        }
        return i;
    }
}
