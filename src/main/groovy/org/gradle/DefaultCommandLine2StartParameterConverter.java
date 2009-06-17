/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.artifacts.ProjectDependenciesBuildInstruction;
import org.gradle.api.initialization.Settings;
import org.gradle.api.logging.LogLevel;
import org.gradle.execution.BuiltInTasksBuildExecuter;
import org.gradle.util.GUtil;
import org.gradle.util.WrapUtil;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Hans Dockter
 */
public class DefaultCommandLine2StartParameterConverter implements CommandLine2StartParameterConverter {
    public static final String GRADLE_HOME_PROPERTY_KEY = "gradle.home";
    public static final String GRADLE_USER_HOME_PROPERTY_KEY = "gradle.user.home";
    public static final String DEFAULT_GRADLE_USER_HOME = System.getProperty("user.home") + "/.gradle";
    public final static String DEFAULT_PLUGIN_PROPERTIES = "plugin.properties";
    public final static String IMPORTS_FILE_NAME = "gradle-imports";
    public final static String NL = System.getProperty("line.separator");

    private static final String NO_SEARCH_UPWARDS = "u";
    private static final String PROJECT_DIR = "p";
    private static final String PROJECT_DEPENDENCY_TASK_NAMES = "A";
    private static final String NO_PROJECT_DEPENDENCY_REBUILD = "a";
    private static final String PLUGIN_PROPERTIES_FILE = "l";
    private static final String DEFAULT_IMPORT_FILE = "K";
    private static final String BUILD_FILE = "b";
    private static final String SETTINGS_FILE = "c";
    private static final String TASKS = "t";
    private static final String PROPERTIES = "r";
    private static final String DEPENDENCIES = "n";
    public static final String DEBUG = "d";
    private static final String INFO = "i";
    private static final String QUIET = "q";
    public static final String FULL_STACKTRACE = "f";
    public static final String STACKTRACE = "s";
    private static final String SYSTEM_PROP = "D";
    private static final String PROJECT_PROP = "P";
    private static final String NO_DEFAULT_IMPORTS = "I";
    private static final String GRADLE_USER_HOME = "g";
    private static final String EMBEDDED_SCRIPT = "e";
    private static final String VERSION = "v";
    private static final String CACHE = "C";
    private static final String DRY_RUN = "m";
    private static final String HELP = "h";

    OptionParser parser = new OptionParser() {
        {
            acceptsAll(WrapUtil.toList(NO_DEFAULT_IMPORTS, "no-imports"), "Disable usage of default imports for build script files.");
            acceptsAll(WrapUtil.toList(NO_SEARCH_UPWARDS, "no-search-upward"),
                    String.format("Don't search in parent folders for a %s file.", Settings.DEFAULT_SETTINGS_FILE));
            acceptsAll(WrapUtil.toList(CACHE, "cache"),
                    "Specifies how compiled build scripts should be cached. Possible values are: 'rebuild', 'off', 'on'. Default value is 'on'").withRequiredArg().ofType(String.class);
            acceptsAll(WrapUtil.toList(VERSION, "version"), "Print version info.");
            acceptsAll(WrapUtil.toList(DEBUG, "debug"), "Log in debug mode (includes normal stacktrace).");
            acceptsAll(WrapUtil.toList(QUIET, "quiet"), "Log errors only.");
            acceptsAll(WrapUtil.toList(DRY_RUN, "dry-run"), "Runs the builds with all task actions disabled.");
            acceptsAll(WrapUtil.toList(INFO, "info"), "Set log level to info.");
            acceptsAll(WrapUtil.toList(STACKTRACE, "stacktrace"), "Print out the stacktrace also for user exceptions (e.g. compile error).");
            acceptsAll(WrapUtil.toList(FULL_STACKTRACE, "full-stacktrace"), "Print out the full (very verbose) stacktrace for any exceptions.");
            acceptsAll(WrapUtil.toList(TASKS, "tasks"), "Show list of all available tasks and their dependencies.");
            acceptsAll(WrapUtil.toList(PROPERTIES, "properties"), "Show list of all available project properties.");
            acceptsAll(WrapUtil.toList(DEPENDENCIES, "dependencies"), "Show list of all project dependencies.");
            acceptsAll(WrapUtil.toList(PROJECT_DIR, "project-dir"), "Specifies the start directory for Gradle. Defaults to current directory.").withRequiredArg().ofType(String.class);
            acceptsAll(WrapUtil.toList(GRADLE_USER_HOME, "gradle-user-home"), "Specifies the gradle user home directory.").withRequiredArg().ofType(String.class);
            acceptsAll(WrapUtil.toList(PLUGIN_PROPERTIES_FILE, "plugin-properties-file"), "Specifies the plugin.properties file.").withRequiredArg().ofType(String.class);
            acceptsAll(WrapUtil.toList(DEFAULT_IMPORT_FILE, "default-import-file"), "Specifies the default import file.").withRequiredArg().ofType(String.class);
            acceptsAll(WrapUtil.toList(SETTINGS_FILE, "settings-file"), "Specifies the settings file.").withRequiredArg().ofType(String.class);
            acceptsAll(WrapUtil.toList(BUILD_FILE, "build-file"), "Specifies the build file.").withRequiredArg().ofType(String.class);
            acceptsAll(WrapUtil.toList(SYSTEM_PROP, "system-prop"), "Set system property of the JVM (e.g. -Dmyprop=myvalue).").withRequiredArg().ofType(String.class);
            acceptsAll(WrapUtil.toList(PROJECT_PROP, "project-prop"), "Set project property for the build script (e.g. -Pmyprop=myvalue).").withRequiredArg().ofType(String.class);
            acceptsAll(WrapUtil.toList(EMBEDDED_SCRIPT, "embedded"), "Specify an embedded build script.").withRequiredArg().ofType(String.class);
            acceptsAll(WrapUtil.toList(PROJECT_DEPENDENCY_TASK_NAMES, "dep-tasks"), "Specify additional tasks for building project dependencies.").withRequiredArg().ofType(String.class);
            acceptsAll(WrapUtil.toList(NO_PROJECT_DEPENDENCY_REBUILD, "no-rebuild"), "Do not rebuild project dependencies.");
            acceptsAll(WrapUtil.toList(HELP, "?", "help"), "Shows this help message");
        }
    };

    public StartParameter convert(String[] args) {
        StartParameter startParameter = new StartParameter();

        OptionSet options = parser.parse(args);

        if (options.has(HELP)) {
            startParameter.setShowHelp(true);
            return startParameter;
        }

        if (options.has(VERSION)) {
            startParameter.setShowVersion(true);
            return startParameter;
        }

        String gradleHome = System.getProperty(GRADLE_HOME_PROPERTY_KEY);
        if (!GUtil.isTrue(gradleHome)) {
            throw new CommandLineArgumentException("The gradle.home property is not set. Please set it and try again.");
        }
        startParameter.setGradleHomeDir(new File(gradleHome));

        if (options.has(NO_DEFAULT_IMPORTS)) {
            startParameter.setDefaultImportsFile(null);
        } else if (options.has(DEFAULT_IMPORT_FILE)) {
            startParameter.setDefaultImportsFile(new File(options.argumentOf(DEFAULT_IMPORT_FILE)));
        }

        if (options.has(SYSTEM_PROP)) {
            List<String> props = options.argumentsOf(SYSTEM_PROP);
            for (String keyValueExpression : props) {
                String[] elements = keyValueExpression.split("=");
                startParameter.getSystemPropertiesArgs().put(elements[0], elements.length == 1 ? "" : elements[1]);
            }
        }

        if (options.has(PROJECT_PROP)) {
            List<String> props = options.argumentsOf(PROJECT_PROP);
            for (String keyValueExpression : props) {
                String[] elements = keyValueExpression.split("=");
                startParameter.getProjectProperties().put(elements[0], elements.length == 1 ? "" : elements[1]);
            }
        }

        startParameter.setSearchUpwards(!options.has(NO_SEARCH_UPWARDS));

        if (options.has(PROJECT_DIR)) {
            startParameter.setProjectDir(new File(options.argumentOf(PROJECT_DIR)));
        }
        if (options.hasArgument(GRADLE_USER_HOME)) {
            startParameter.setGradleUserHomeDir(new File(options.argumentOf(GRADLE_USER_HOME)));
        }
        if (options.hasArgument(BUILD_FILE)) {
            startParameter.setBuildFile(new File(options.argumentOf(BUILD_FILE)));
        }
        if (options.hasArgument(SETTINGS_FILE)) {
            startParameter.setSettingsFile(new File(options.argumentOf(SETTINGS_FILE)));
        }
        if (options.hasArgument(PLUGIN_PROPERTIES_FILE)) {
            startParameter.setPluginPropertiesFile(new File(options.argumentOf(PLUGIN_PROPERTIES_FILE)));
        }

        if (options.has(CACHE)) {
            try {
                startParameter.setCacheUsage(CacheUsage.fromString(options.valueOf(CACHE).toString()));
            } catch (InvalidUserDataException e) {
                throw new CommandLineArgumentException(e.getMessage());
            }
        }

        if (options.has(EMBEDDED_SCRIPT)) {
            if (options.has(BUILD_FILE) || options.has(NO_SEARCH_UPWARDS) || options.has(SETTINGS_FILE)) {
                System.err.println(String.format("Error: The -%s option can't be used together with the -%s, -%s or -%s options.",
                        EMBEDDED_SCRIPT, BUILD_FILE, SETTINGS_FILE, NO_SEARCH_UPWARDS));
                throw new CommandLineArgumentException(String.format("Error: The -%s option can't be used together with the -%s, -%s or -%s options.",
                        EMBEDDED_SCRIPT, BUILD_FILE, SETTINGS_FILE, NO_SEARCH_UPWARDS));
            }
            startParameter.useEmbeddedBuildFile(options.argumentOf(EMBEDDED_SCRIPT));
        }

        if (options.has(TASKS) && options.has(PROPERTIES)) {
            throw new CommandLineArgumentException(String.format("Error: The -%s and -%s options cannot be used together.", TASKS, PROPERTIES));
        }

        if (options.has(PROJECT_DEPENDENCY_TASK_NAMES) && options.has(NO_PROJECT_DEPENDENCY_REBUILD)) {
            throw new CommandLineArgumentException(String.format("Error: The -%s and -%s options cannot be used together.", PROJECT_DEPENDENCY_TASK_NAMES,
                    NO_PROJECT_DEPENDENCY_REBUILD));
        } else if (options.has(NO_PROJECT_DEPENDENCY_REBUILD)) {
            startParameter.setProjectDependenciesBuildInstruction(new ProjectDependenciesBuildInstruction(null));
        } else if (options.has(PROJECT_DEPENDENCY_TASK_NAMES)) {
            List<String> normalizedTaskNames = new ArrayList<String>();
            for (Object o : options.valuesOf(PROJECT_DEPENDENCY_TASK_NAMES)) {
                String taskName = (String) o;
                normalizedTaskNames.add(taskName.trim());
            }
            startParameter.setProjectDependenciesBuildInstruction(new ProjectDependenciesBuildInstruction(
                    normalizedTaskNames
            ));
        }

        if (options.has(TASKS)) {
            startParameter.setBuildExecuter(new BuiltInTasksBuildExecuter(BuiltInTasksBuildExecuter.Options.TASKS));
        } else if (options.has(PROPERTIES)) {
            startParameter.setBuildExecuter(new BuiltInTasksBuildExecuter(BuiltInTasksBuildExecuter.Options.PROPERTIES));
        } else if (options.has(DEPENDENCIES)) {
            startParameter.setBuildExecuter(new BuiltInTasksBuildExecuter(BuiltInTasksBuildExecuter.Options.DEPENDENCIES));
        } else {
            startParameter.setTaskNames(options.nonOptionArguments());
        }

        if (options.has(DRY_RUN)) {
            startParameter.setDryRun(true);
        }

        startParameter.setLogLevel(getLogLevel(options));
        return startParameter;
    }

    public void showHelp(OutputStream out) throws IOException {
        parser.printHelpOn(out);
    }

    private LogLevel getLogLevel(OptionSet options) {
        LogLevel logLevel = null;
        if (options.has(QUIET)) {
            logLevel = LogLevel.QUIET;
        }
        if (options.has(INFO)) {
            quitWithErrorIfLogLevelAlreadyDefined(logLevel, INFO);
            logLevel = LogLevel.INFO;
        }
        if (options.has(DEBUG)) {
            quitWithErrorIfLogLevelAlreadyDefined(logLevel, DEBUG);
            logLevel = LogLevel.DEBUG;
        }
        if (logLevel == null) {
            logLevel = LogLevel.LIFECYCLE;
        }
        return logLevel;
    }

    private void quitWithErrorIfLogLevelAlreadyDefined(LogLevel logLevel, String option) {
        if (logLevel != null) {
            System.err.println(String.format("Error: The log level is already defined by another option. Therefore the option %s is invalid.",
                    option));
            throw new InvalidUserDataException();
        }
    }
}
