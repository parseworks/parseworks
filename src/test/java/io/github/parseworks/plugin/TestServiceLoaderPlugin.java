package io.github.parseworks.plugin;

import io.github.parseworks.Parser;
import io.github.parseworks.Result;

/**
 * A test plugin for the ServiceLoader mechanism.
 */
public class TestServiceLoaderPlugin extends AbstractParserProvider {

    public TestServiceLoaderPlugin() {
        // The initialize() method will be called by ParserPluginRegistry.registerPlugin()
    }

    @Override
    public String getId() {
        return "io.github.parseworks.test.serviceloader";
    }

    @Override
    public String getName() {
        return "Service Loader Test Plugin";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public void initialize() {
        registerParser("serviceLoaderTestParser", createServiceLoaderTestParser());
    }

    private Parser<Character, String> createServiceLoaderTestParser() {
        return new Parser<>(in -> {
            StringBuilder sb = new StringBuilder();

            // Read characters until end of input
            while (!in.isEof()) {
                sb.append(in.current());
                in = in.next();
            }

            // Convert to uppercase
            String result = sb.toString().toUpperCase();

            return Result.success(in, result);
        });
    }
}
