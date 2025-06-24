package io.github.parseworks.plugin;

/**
 * Interface for parser plugins that can be loaded dynamically.
 * <p>
 * This interface defines the contract for parser plugins in the parseWorks library.
 * Implementations of this interface can provide custom parsers that extend the
 * functionality of the core library.
 * <p>
 * Plugin implementations should be registered with the {@link ParserPluginRegistry}
 * to make them available to the parseWorks library.
 *
 * @author parseWorks
 * @see ParserPluginRegistry
 */
public interface ParserPlugin {
    
    /**
     * Returns the unique identifier for this plugin.
     * <p>
     * The identifier should be unique across all plugins and should follow
     * the format of a Java package name (e.g., "com.example.myplugin").
     *
     * @return the unique identifier for this plugin
     */
    String getId();
    
    /**
     * Returns the name of this plugin.
     * <p>
     * The name is a human-readable identifier for the plugin and does not
     * need to be unique.
     *
     * @return the name of this plugin
     */
    String getName();
    
    /**
     * Returns the version of this plugin.
     * <p>
     * The version should follow semantic versioning (e.g., "1.0.0").
     *
     * @return the version of this plugin
     */
    String getVersion();
    
    /**
     * Initializes this plugin.
     * <p>
     * This method is called when the plugin is loaded by the {@link ParserPluginRegistry}.
     * It should perform any necessary initialization for the plugin.
     */
    void initialize();
    
    /**
     * Shuts down this plugin.
     * <p>
     * This method is called when the plugin is unloaded by the {@link ParserPluginRegistry}.
     * It should perform any necessary cleanup for the plugin.
     */
    void shutdown();
}