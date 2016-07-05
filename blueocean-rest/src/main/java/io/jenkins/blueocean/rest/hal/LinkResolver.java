package io.jenkins.blueocean.rest.hal;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class LinkResolver {
    public abstract Link resolve(Object modelObject);
}
