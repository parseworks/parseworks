package io.github.parseworks.plugin;

import io.github.parseworks.Parser;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Abstract base class for parser providers.
 * <p>
 * This class provides a default implementation of the {@link ParserProvider} interface.
 * It maintains a map of parsers and provides implementations for the methods defined
 * in the interface.
 * <p>
 * Subclasses should override the {@link #initialize()} method to register their parsers
 * using the {@link #registerParser(String, Parser)} method.
 * <p>
 * Example usage:
 * <pre>{@code
 * public class MyParserProvider extends AbstractParserProvider {
 *     @Override
 *     public String getId() {
 *         return "com.example.myplugin";
 *     }
 *     
 *     @Override
 *     public String getName() {
 *         return "My Parser Provider";
 *     }
 *     
 *     @Override
 *     public String getVersion() {
 *         return "1.0.0";
 *     }
 *     
 *     @Override
 *     public void initialize() {
 *         // Register parsers
 *         registerParser("myParser", myParser);
 *         registerParser("anotherParser", anotherParser);
 *     }
 * }
 * }</pre>
 *
 * @author parseWorks
 * @see ParserProvider
 * @see Parser
 */
public abstract class AbstractParserProvider implements ParserProvider {
    
    private final Map<String, Parser<?, ?>> parsers = new HashMap<>();
    
    /**
     * Registers a parser with this provider.
     * <p>
     * This method should be called from the {@link #initialize()} method to register
     * parsers provided by this plugin.
     *
     * @param name the name of the parser
     * @param parser the parser instance
     * @throws IllegalArgumentException if the name or parser is null, or if a parser
     *         with the given name is already registered
     */
    protected void registerParser(String name, Parser<?, ?> parser) {
        if (name == null) {
            throw new IllegalArgumentException("Parser name cannot be null");
        }
        if (parser == null) {
            throw new IllegalArgumentException("Parser cannot be null");
        }
        if (parsers.containsKey(name)) {
            throw new IllegalArgumentException("Parser with name '" + name + "' is already registered");
        }
        
        parsers.put(name, parser);
    }
    
    /**
     * Unregisters a parser from this provider.
     *
     * @param name the name of the parser to unregister
     * @return true if the parser was unregistered, false if no parser with the given name was found
     */
    protected boolean unregisterParser(String name) {
        return parsers.remove(name) != null;
    }
    
    /**
     * Clears all registered parsers.
     */
    protected void clearParsers() {
        parsers.clear();
    }
    
    @Override
    public Map<String, Parser<?, ?>> getParsers() {
        return Collections.unmodifiableMap(parsers);
    }
    
    @Override
    public Parser<?, ?> getParser(String name) {
        return parsers.get(name);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <I, O> Collection<Parser<I, O>> getParsersOfType(Class<I> inputType, Class<O> outputType) {
        return parsers.values().stream()
                .filter(parser -> {
                    // This is a simplification; in a real implementation, we would need to
                    // check the generic types of the parser, which is challenging in Java
                    // due to type erasure. For now, we'll just return all parsers.
                    return true;
                })
                .map(parser -> (Parser<I, O>) parser)
                .collect(Collectors.toUnmodifiableList());
    }
    
    @Override
    public void shutdown() {
        clearParsers();
    }
}