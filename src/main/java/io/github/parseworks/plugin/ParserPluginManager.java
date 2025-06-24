package io.github.parseworks.plugin;

import io.github.parseworks.Parser;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manager for parser plugins in the parseWorks library.
 * <p>
 * This class provides a high-level API for accessing parsers provided by plugins.
 * It delegates to the {@link ParserPluginRegistry} for plugin management and provides
 * methods for getting parsers by name or type.
 * <p>
 * Example usage:
 * <pre>{@code
 * // Get the manager instance
 * ParserPluginManager manager = ParserPluginManager.getInstance();
 * 
 * // Load all available plugins
 * manager.loadPlugins();
 * 
 * // Get a parser by name
 * Optional<Parser<Character, Object>> jsonParser = manager.getParser("json");
 * 
 * // Use the parser
 * jsonParser.ifPresent(parser -> {
 *     Object result = parser.parse("{\"name\":\"John\",\"age\":30}").get();
 *     // Process the result...
 * });
 * }</pre>
 *
 * @author parseWorks
 * @see ParserPluginRegistry
 * @see ParserProvider
 * @see Parser
 */
public class ParserPluginManager {
    
    private static final ParserPluginManager INSTANCE = new ParserPluginManager();
    
    private final ParserPluginRegistry registry;
    private final Map<String, Parser<?, ?>> parserCache = new ConcurrentHashMap<>();
    
    /**
     * Private constructor to enforce singleton pattern.
     */
    private ParserPluginManager() {
        this.registry = ParserPluginRegistry.getInstance();
    }
    
    /**
     * Returns the singleton instance of the manager.
     *
     * @return the singleton instance of the manager
     */
    public static ParserPluginManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * Loads all available plugins using the registry.
     * <p>
     * This method delegates to {@link ParserPluginRegistry#loadPlugins()}.
     */
    public void loadPlugins() {
        registry.loadPlugins();
        refreshParserCache();
    }
    
    /**
     * Registers a plugin with the registry.
     * <p>
     * This method delegates to {@link ParserPluginRegistry#registerPlugin(ParserPlugin)}.
     *
     * @param plugin the plugin to register
     * @throws IllegalArgumentException if the plugin is null
     */
    public void registerPlugin(ParserPlugin plugin) {
        registry.registerPlugin(plugin);
        refreshParserCache();
    }
    
    /**
     * Unregisters a plugin from the registry.
     * <p>
     * This method delegates to {@link ParserPluginRegistry#unregisterPlugin(String)}.
     *
     * @param pluginId the ID of the plugin to unregister
     * @return true if the plugin was unregistered, false if no plugin with the given ID was found
     */
    public boolean unregisterPlugin(String pluginId) {
        boolean result = registry.unregisterPlugin(pluginId);
        if (result) {
            refreshParserCache();
        }
        return result;
    }
    
    /**
     * Returns a parser by name.
     * <p>
     * This method searches all registered parser providers for a parser with the given name.
     * If multiple parsers with the same name are found, the first one is returned.
     *
     * @param <I> the input type of the parser
     * @param <O> the output type of the parser
     * @param name the name of the parser to retrieve
     * @return an Optional containing the parser, or empty if no parser with the given name was found
     */
    @SuppressWarnings("unchecked")
    public <I, O> Optional<Parser<I, O>> getParser(String name) {
        return Optional.ofNullable((Parser<I, O>) parserCache.get(name));
    }
    
    /**
     * Returns all parsers of a specific input and output type.
     * <p>
     * This method searches all registered parser providers for parsers that match the specified
     * input and output types.
     *
     * @param <I> the input type
     * @param <O> the output type
     * @param inputType the class object representing the input type
     * @param outputType the class object representing the output type
     * @return a collection of parsers that match the specified types
     */
    public <I, O> Collection<Parser<I, O>> getParsersOfType(Class<I> inputType, Class<O> outputType) {
        return registry.getPluginsOfType(ParserProvider.class).stream()
                .flatMap(provider -> provider.getParsersOfType(inputType, outputType).stream())
                .collect(Collectors.toUnmodifiableList());
    }
    
    /**
     * Returns all registered parsers.
     *
     * @return an unmodifiable map of parser names to parser instances
     */
    public Map<String, Parser<?, ?>> getAllParsers() {
        return Collections.unmodifiableMap(parserCache);
    }
    
    /**
     * Shuts down all registered plugins and clears the registry.
     * <p>
     * This method delegates to {@link ParserPluginRegistry#shutdown()}.
     */
    public void shutdown() {
        registry.shutdown();
        parserCache.clear();
    }
    
    /**
     * Refreshes the parser cache by collecting parsers from all registered parser providers.
     */
    private void refreshParserCache() {
        parserCache.clear();
        
        for (ParserProvider provider : registry.getPluginsOfType(ParserProvider.class)) {
            for (Map.Entry<String, Parser<?, ?>> entry : provider.getParsers().entrySet()) {
                parserCache.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }
    }
}