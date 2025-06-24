package io.github.parseworks.plugin;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry for parser plugins in the parseWorks library.
 * <p>
 * This class manages the registration, discovery, and loading of parser plugins.
 * It uses Java's ServiceLoader mechanism to discover plugins and provides methods
 * to register, unregister, and access plugins.
 * <p>
 * Example usage:
 * <pre>{@code
 * // Get the registry instance
 * ParserPluginRegistry registry = ParserPluginRegistry.getInstance();
 * 
 * // Load all available plugins
 * registry.loadPlugins();
 * 
 * // Get a plugin by ID
 * Optional<ParserPlugin> plugin = registry.getPlugin("com.example.myplugin");
 * 
 * // Register a custom plugin
 * MyCustomPlugin customPlugin = new MyCustomPlugin();
 * registry.registerPlugin(customPlugin);
 * }</pre>
 *
 * @author parseWorks
 * @see ParserPlugin
 */
public class ParserPluginRegistry {

    private static final ParserPluginRegistry INSTANCE = new ParserPluginRegistry();

    private final Map<String, ParserPlugin> plugins = new ConcurrentHashMap<>();

    /**
     * Private constructor to enforce singleton pattern.
     */
    private ParserPluginRegistry() {
        // Private constructor to enforce singleton pattern
    }

    /**
     * Returns the singleton instance of the registry.
     *
     * @return the singleton instance of the registry
     */
    public static ParserPluginRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Loads all available plugins using Java's ServiceLoader mechanism.
     * <p>
     * This method discovers plugins that implement the {@link ParserPlugin} interface
     * and are registered via the Java Service Provider Interface (SPI).
     * <p>
     * To register a plugin for discovery, create a file named
     * {@code META-INF/services/io.github.parseworks.plugin.ParserPlugin} in your JAR
     * file, with each line containing the fully qualified name of a plugin implementation.
     */
    public void loadPlugins() {
        // Use the thread's context class loader to find services in test environments
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        ServiceLoader<ParserPlugin> serviceLoader = ServiceLoader.load(ParserPlugin.class, contextClassLoader);

        for (ParserPlugin plugin : serviceLoader) {
            registerPlugin(plugin);
        }
    }

    /**
     * Registers a plugin with the registry.
     * <p>
     * If a plugin with the same ID is already registered, it will be replaced.
     *
     * @param plugin the plugin to register
     * @throws IllegalArgumentException if the plugin is null
     */
    public void registerPlugin(ParserPlugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }

        String pluginId = plugin.getId();
        ParserPlugin existingPlugin = plugins.put(pluginId, plugin);

        if (existingPlugin != null) {
            existingPlugin.shutdown();
        }

        plugin.initialize();
    }

    /**
     * Unregisters a plugin from the registry.
     *
     * @param pluginId the ID of the plugin to unregister
     * @return true if the plugin was unregistered, false if no plugin with the given ID was found
     */
    public boolean unregisterPlugin(String pluginId) {
        ParserPlugin plugin = plugins.remove(pluginId);

        if (plugin != null) {
            plugin.shutdown();
            return true;
        }

        return false;
    }

    /**
     * Returns a plugin by its ID.
     *
     * @param pluginId the ID of the plugin to retrieve
     * @return an Optional containing the plugin, or empty if no plugin with the given ID was found
     */
    public Optional<ParserPlugin> getPlugin(String pluginId) {
        return Optional.ofNullable(plugins.get(pluginId));
    }

    /**
     * Returns all registered plugins.
     *
     * @return an unmodifiable collection of all registered plugins
     */
    public Collection<ParserPlugin> getAllPlugins() {
        return Collections.unmodifiableCollection(plugins.values());
    }

    /**
     * Returns all registered plugins of a specific type.
     *
     * @param <T> the type of plugin to retrieve
     * @param pluginType the class object representing the plugin type
     * @return an unmodifiable collection of all registered plugins of the specified type
     */
    public <T extends ParserPlugin> Collection<T> getPluginsOfType(Class<T> pluginType) {
        return plugins.values().stream()
                .filter(pluginType::isInstance)
                .map(pluginType::cast)
                .toList();
    }

    /**
     * Shuts down all registered plugins and clears the registry.
     */
    public void shutdown() {
        plugins.values().forEach(ParserPlugin::shutdown);
        plugins.clear();
    }
}
