package io.github.parseworks.plugin;

import io.github.parseworks.Parser;

import java.util.Collection;
import java.util.Map;

/**
 * Interface for plugins that provide parsers.
 * <p>
 * This interface extends {@link ParserPlugin} and adds methods to get parsers
 * provided by the plugin. Implementations of this interface should provide
 * one or more parsers that can be used in the parseWorks library.
 * <p>
 * Example usage:
 * <pre>{@code
 * // Get the registry instance
 * ParserPluginRegistry registry = ParserPluginRegistry.getInstance();
 * 
 * // Get all parser providers
 * Collection<ParserProvider> providers = registry.getPluginsOfType(ParserProvider.class);
 * 
 * // Get parsers from a specific provider
 * for (ParserProvider provider : providers) {
 *     Map<String, Parser<?, ?>> parsers = provider.getParsers();
 *     // Use the parsers...
 * }
 * }</pre>
 *
 * @author parseWorks
 * @see ParserPlugin
 * @see Parser
 */
public interface ParserProvider extends ParserPlugin {
    
    /**
     * Returns all parsers provided by this plugin.
     * <p>
     * The returned map contains parser names as keys and parser instances as values.
     * The parser names should be unique within the plugin and should be descriptive
     * of the parser's purpose.
     *
     * @return a map of parser names to parser instances
     */
    Map<String, Parser<?, ?>> getParsers();
    
    /**
     * Returns a parser by name.
     * <p>
     * If no parser with the given name is found, this method should return null.
     *
     * @param name the name of the parser to retrieve
     * @return the parser with the given name, or null if no such parser exists
     */
    Parser<?, ?> getParser(String name);
    
    /**
     * Returns all parsers of a specific input and output type.
     * <p>
     * This method filters the parsers provided by this plugin to include only
     * those that match the specified input and output types.
     *
     * @param <I> the input type
     * @param <O> the output type
     * @param inputType the class object representing the input type
     * @param outputType the class object representing the output type
     * @return a collection of parsers that match the specified types
     */
    <I, O> Collection<Parser<I, O>> getParsersOfType(Class<I> inputType, Class<O> outputType);
}