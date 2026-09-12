package com.rnk.loader;

/**
 * Common interface representing a generic loader implementation.
 * Each loader library should provide an implementation of this API.
 */
public interface LoaderApi {
    /**
     * Initialize the loader (e.g. set up classpaths, hooks, etc.)
     */
    void initialize();

    /**
     * Return true if this loader supports the given Minecraft version string.
     */
    boolean supportsVersion(String version);

    /**
     * Inject a mod by name or path into the running environment.
     */
    void injectMod(String modIdentifier);
}