# parseWorks Plugin System

This document describes the plugin system for the parseWorks library, which allows users to extend the library with custom parsers and functionality.

## Overview

The parseWorks plugin system provides a modular way to extend the library with custom parsers and functionality. Plugins can be loaded dynamically at runtime, allowing users to add new parsers without modifying the core library.

The plugin system is designed to be:

- **Modular**: Plugins are self-contained and can be added or removed independently.
- **Discoverable**: Plugins can be discovered automatically using Java's ServiceLoader mechanism.
- **Extensible**: The plugin system can be extended to support different types of plugins.
- **Type-safe**: The plugin system uses Java's generics to ensure type safety.

## Plugin Architecture

The plugin system consists of the following components:

- **ParserPlugin**: The base interface for all plugins.
- **ParserPluginRegistry**: A registry for managing plugins.
- **ParserProvider**: An interface for plugins that provide parsers.
- **AbstractParserProvider**: An abstract base class for implementing parser providers.

### ParserPlugin Interface

The `ParserPlugin` interface defines the contract for all plugins in the parseWorks library. It includes methods for getting plugin information and lifecycle management.

```java
public interface ParserPlugin {
    String getId();
    String getName();
    String getVersion();
    void initialize();
    void shutdown();
}
```

### ParserPluginRegistry Class

The `ParserPluginRegistry` class manages the registration, discovery, and loading of plugins. It provides methods for registering, unregistering, and accessing plugins.

```java
public class ParserPluginRegistry {
    public static ParserPluginRegistry getInstance();
    public void loadPlugins();
    public void registerPlugin(ParserPlugin plugin);
    public boolean unregisterPlugin(String pluginId);
    public Optional<ParserPlugin> getPlugin(String pluginId);
    public Collection<ParserPlugin> getAllPlugins();
    public <T extends ParserPlugin> Collection<T> getPluginsOfType(Class<T> pluginType);
    public void shutdown();
}
```

### ParserProvider Interface

The `ParserProvider` interface extends `ParserPlugin` and adds methods for getting parsers provided by the plugin.

```java
public interface ParserProvider extends ParserPlugin {
    Map<String, Parser<?, ?>> getParsers();
    Parser<?, ?> getParser(String name);
    <I, O> Collection<Parser<I, O>> getParsersOfType(Class<I> inputType, Class<O> outputType);
}
```

### AbstractParserProvider Class

The `AbstractParserProvider` class provides a default implementation of the `ParserProvider` interface. It maintains a map of parsers and provides implementations for the methods defined in the interface.

```java
public abstract class AbstractParserProvider implements ParserProvider {
    protected void registerParser(String name, Parser<?, ?> parser);
    protected boolean unregisterParser(String name);
    protected void clearParsers();

    @Override
    public Map<String, Parser<?, ?>> getParsers();

    @Override
    public Parser<?, ?> getParser(String name);

    @Override
    public <I, O> Collection<Parser<I, O>> getParsersOfType(Class<I> inputType, Class<O> outputType);

    @Override
    public void shutdown();
}
```

## Creating Plugins

To create a plugin for the parseWorks library, follow these steps:

1. Create a class that implements the `ParserPlugin` interface or extends the `AbstractParserProvider` class.
2. Implement the required methods to provide plugin information and functionality.
3. Register the plugin with the `ParserPluginRegistry`.

### Example: Creating a Parser Provider

Here's an example of creating a parser provider that provides a JSON parser:

```java
public class JsonParserProvider extends AbstractParserProvider {
    @Override
    public String getId() {
        return "io.github.parseworks.json";
    }

    @Override
    public String getName() {
        return "JSON Parser Provider";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public void initialize() {
        // Create and register JSON parsers
        Parser<Character, Object> jsonParser = createJsonParser();
        registerParser("jsonParser", jsonParser);
    }

    private Parser<Character, Object> createJsonParser() {
        // Implementation of JSON parser
        // ...
    }
}
```

### Registering Plugins for Auto-Discovery

To register a plugin for auto-discovery using Java's ServiceLoader mechanism, create a file named `META-INF/services/io.github.parseworks.plugin.ParserPlugin` in your JAR file, with each line containing the fully qualified name of a plugin implementation.

For example:

```
io.github.parseworks.plugin.examples.JsonParserProvider
```

## Using Plugins

To use plugins in your application, follow these steps:

1. Get the `ParserPluginManager` instance.
2. Load all available plugins or register specific plugins.
3. Get the parsers you need by name or type.
4. Use the parsers in your application.

### Example: Using Plugins with ParserPluginManager

The `ParserPluginManager` provides a high-level API for accessing parsers provided by plugins. It's the recommended way to use plugins in your application.

```java
// Get the manager instance
ParserPluginManager manager = ParserPluginManager.getInstance();

// Load all available plugins
manager.loadPlugins();

// Get a parser by name
Optional<Parser<Character, Object>> jsonParser = manager.getParser("jsonParser");

// Use the parser
jsonParser.ifPresent(parser -> {
Object result = parser.parse("{\"name\":\"John\",\"age\":30}").get();
// Process the result...
});

// Get parsers of a specific type
Collection<Parser<Character, String>> stringParsers =
        manager.getParsersOfType(Character.class, String.class);
```

### Example: Using Plugins with ParserPluginRegistry

You can also use the `ParserPluginRegistry` directly if you need more control over plugin management.

```java
// Get the registry instance
ParserPluginRegistry registry = ParserPluginRegistry.getInstance();

// Load all available plugins
registry.loadPlugins();

// Get a plugin by ID
Optional<ParserPlugin> plugin = registry.getPlugin("io.github.parseworks.json");

// Get a parser from a parser provider
if (plugin.isPresent() && plugin.get() instanceof ParserProvider) {
    ParserProvider provider = (ParserProvider) plugin.get();
    Parser<Character, Object> jsonParser = (Parser<Character, Object>) provider.getParser("jsonParser");

    // Use the parser
    Object result = jsonParser.parse("{\"name\":\"John\",\"age\":30}").get();
}
```

## Best Practices

When creating and using plugins, follow these best practices:

- **Use unique plugin IDs**: Use a unique ID for your plugin, preferably based on your package name.
- **Provide clear documentation**: Document your plugin's functionality and usage.
- **Handle errors gracefully**: Ensure your plugin handles errors gracefully and provides meaningful error messages.
- **Clean up resources**: Implement the `shutdown` method to clean up resources when your plugin is unregistered.
- **Use type safety**: Use Java's generics to ensure type safety in your parsers.
- **Test thoroughly**: Test your plugin thoroughly to ensure it works correctly with the parseWorks library.

## Conclusion

The parseWorks plugin system provides a flexible and extensible way to add custom parsers and functionality to the parseWorks library. By following the guidelines in this document, you can create and use plugins to extend the library's capabilities.