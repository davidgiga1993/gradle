/*
 * Copyright 2012 the original author or authors.
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
package org.gradle.nativecode.toolchain.internal.gpp;

import org.gradle.api.Transformer;
import org.gradle.api.internal.tasks.compile.Compiler;
import org.gradle.internal.Factory;
import org.gradle.internal.os.OperatingSystem;
import org.gradle.nativecode.base.internal.*;
import org.gradle.nativecode.language.asm.internal.AssembleSpec;
import org.gradle.nativecode.language.c.internal.CCompileSpec;
import org.gradle.nativecode.language.cpp.internal.CppCompileSpec;
import org.gradle.nativecode.toolchain.internal.CommandLineTool;
import org.gradle.nativecode.toolchain.internal.gpp.version.GppVersionDeterminer;
import org.gradle.process.internal.ExecAction;
import org.gradle.util.GUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * Compiler adapter for GCC.
 */
public class GppToolChain extends AbstractToolChain {

    private static final Logger LOGGER = LoggerFactory.getLogger(GppToolChain.class);

    public static final String DEFAULT_NAME = "gcc";

    private final ToolRegistry executables;
    private final Factory<ExecAction> execActionFactory;
    private final Transformer<String, File> versionDeterminer;

    private String version;

    public GppToolChain(String name, OperatingSystem operatingSystem, Factory<ExecAction> execActionFactory) {
        super(name, operatingSystem);
        this.executables = new ToolRegistry(operatingSystem);
        this.execActionFactory = execActionFactory;
        this.versionDeterminer = new GppVersionDeterminer();
    }

    @Override
    protected String getTypeName() {
        return "GNU G++";
    }

    @Override
    protected void checkAvailable(ToolChainAvailability availability) {
        for (Tool key : Tool.values()) {
            availability.mustExist(key.getToolName(), executables.locate(key));
        }
        determineVersion();
        if (version == null) {
            availability.unavailable("Could not determine G++ version");
        }
    }

    public <T extends BinaryToolSpec> Compiler<T> createCppCompiler() {
        checkAvailable();
        CommandLineTool<CppCompileSpec> commandLineTool = commandLineTool(Tool.CPP_COMPILER);
        return (Compiler<T>) new CppCompiler(commandLineTool, canUseCommandFile(version));
    }

    public <T extends BinaryToolSpec> Compiler<T> createCCompiler() {
        checkAvailable();
        CommandLineTool<CCompileSpec> commandLineTool = commandLineTool(Tool.C_COMPILER);
        return (Compiler<T>) new CCompiler(commandLineTool, canUseCommandFile(version));
    }

    public <T extends BinaryToolSpec> Compiler<T> createAssembler() {
        checkAvailable();
        CommandLineTool<AssembleSpec> commandLineTool = commandLineTool(Tool.ASSEMBLER);
        return (Compiler<T>) new Assembler(commandLineTool);
    }

    public <T extends LinkerSpec> Compiler<T> createLinker() {
        checkAvailable();
        CommandLineTool<LinkerSpec> commandLineTool = commandLineTool(Tool.LINKER);
        return (Compiler<T>) new GppLinker(commandLineTool, canUseCommandFile(version));
    }

    public <T extends StaticLibraryArchiverSpec> Compiler<T> createStaticLibraryArchiver() {
        checkAvailable();
        CommandLineTool<StaticLibraryArchiverSpec> commandLineTool = commandLineTool(Tool.STATIC_LIB_ARCHIVER);
        return (Compiler<T>) new ArStaticLibraryArchiver(commandLineTool);
    }

    private <T extends BinaryToolSpec> CommandLineTool<T> commandLineTool(Tool key) {
        CommandLineTool<T> commandLineTool = new CommandLineTool<T>(key.getToolName(), executables.locate(key), execActionFactory);
        commandLineTool.withPath(getPath());
        return commandLineTool;
    }

    private void determineVersion() {
        version = determineVersion(executables.locate(Tool.CPP_COMPILER));
        if (version == null) {
            LOGGER.info("Did not find {} on system", Tool.CPP_COMPILER.getToolName());
        } else {
            LOGGER.info("Found {} with version {}", Tool.CPP_COMPILER.getToolName(), version);
        }
    }

    private String determineVersion(File executable) {
        return executable == null ? null : versionDeterminer.transform(executable);
    }

    private boolean canUseCommandFile(String version) {
        String[] components = version.split("\\.");
        int majorVersion;
        try {
            majorVersion = Integer.valueOf(components[0]);
        } catch (NumberFormatException e) {
            throw new IllegalStateException(String.format("Unable to determine major g++ version from version number %s.", version), e);
        }
        return majorVersion >= 4;
    }

    public List<File> getPath() {
        return executables.getPath();
    }

    // TODO:DAZ Resolve object to file
    public void path(File path) {
        executables.path(path);
    }

    public String getCppCompiler() {
        return executables.getExeName(Tool.CPP_COMPILER);
    }

    public void setCppCompiler(String name) {
        executables.setExeName(Tool.CPP_COMPILER, name);
    }

    public String getCCompiler() {
        return executables.getExeName(Tool.C_COMPILER);
    }

    public void setCCompiler(String name) {
        executables.setExeName(Tool.C_COMPILER, name);
    }

    public String getAssembler() {
        return executables.getExeName(Tool.ASSEMBLER);
    }

    public void setAssembler(String name) {
        executables.setExeName(Tool.ASSEMBLER, name);
    }

    public String getLinker() {
        return executables.getExeName(Tool.LINKER);
    }

    public void setLinker(String name) {
        executables.setExeName(Tool.LINKER, name);
    }

    public String getStaticLibArchiver() {
        return executables.getExeName(Tool.STATIC_LIB_ARCHIVER);
    }

    public void setStaticLibArchiver(String name) {
        executables.setExeName(Tool.STATIC_LIB_ARCHIVER, name);
    }

    private enum Tool {
        CPP_COMPILER("C++ compiler"),
        C_COMPILER("C compiler"),
        ASSEMBLER("Assembler"),
        LINKER("Linker"),
        STATIC_LIB_ARCHIVER("Static library archiver");

        private final String toolName;

        private Tool(String toolName) {
            this.toolName = toolName;
        }

        public String getToolName() {
            return toolName;
        }

        @Override
        public String toString() {
            return GUtil.toLowerCamelCase(name());
        }
    }

    private final class ToolRegistry {
        private final Map<Tool, String> executableNames = new HashMap<Tool, String>();
        private final Map<String, File> executables = new HashMap<String, File>();
        private final List<File> pathEntries = new ArrayList<File>();

        private final OperatingSystem operatingSystem;

        public ToolRegistry(OperatingSystem operatingSystem) {
            this.operatingSystem = operatingSystem;

            executableNames.put(Tool.CPP_COMPILER, "g++");
            executableNames.put(Tool.C_COMPILER, "gcc");
            executableNames.put(Tool.ASSEMBLER, "as");
            executableNames.put(Tool.LINKER, "g++");
            executableNames.put(Tool.STATIC_LIB_ARCHIVER, "ar");
        }

        public List<File> getPath() {
            return pathEntries;
        }

        public void path(File pathEntry) {
            pathEntries.add(pathEntry);
            executables.clear();
        }

        public String getExeName(Tool key) {
            return executableNames.get(key);
        }

        public void setExeName(Tool key, String name) {
            executableNames.put(key, name);
        }

        public File locate(Tool key) {
            String exeName = executableNames.get(key);
            if (executables.containsKey(exeName)) {
                return executables.get(exeName);
            }
            File exe = findExecutable(operatingSystem, exeName);
            executables.put(exeName, exe);
            return exe;
        }

        private File findExecutable(OperatingSystem operatingSystem, String exe) {
            List<String> candidates;
            if (operatingSystem.isWindows()) {
                // Under Cygwin, g++/gcc is a Cygwin symlink to either g++-3 or g++-4. We can't run g++ directly
                candidates = Arrays.asList(exe + "-4", exe + "-3", exe);
            } else {
                candidates = Arrays.asList(exe);
            }
            for (String candidate : candidates) {
                File executable = findInPath(candidate);
                if (executable != null) {
                    return executable;
                }
            }
            return null;
        }

        public File findInPath(String name) {
            String exeName = operatingSystem.getExecutableName(name);
            for (File pathEntry : pathEntries) {
                File candidate = new File(pathEntry, exeName);
                if (candidate.isFile()) {
                    return candidate;
                }
            }
            return operatingSystem.findInPath(name);
        }
    }
}