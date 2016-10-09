/*
 * Copyright (c) 2016 Erik Håkansson, http://squark.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.squark.nestedjarclassloader;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class NestedJarClassLoader extends ClassLoader {

    private static final String DEFAULT_MODULE_NAME = "default";
    private final Map<String, Module> modules = new LinkedHashMap<>();

    public NestedJarClassLoader(@Nullable ClassLoader parent)
    {
        super(parent);
        Thread.currentThread().setContextClassLoader(this);
    }

    public void addURLs(URL... urls) throws IOException {
        if (urls == null) {
            return;
        }
        addURLs(null, urls);
    }

    public void addURLs(String module, URL... urls) throws IOException {
        if (urls == null) {
            return;
        }
        if (module == null) {
            module = DEFAULT_MODULE_NAME;
        }
        addResources(module, urls);
    }

    private void addResources(String moduleName, URL... urls) throws IOException {
        Module module = getModule(moduleName);
        module.addResources(urls);
    }

    private Module getModule(String moduleName) throws IOException {
        Module module = modules.get(moduleName);
        if (module == null) {
            module = new Module(moduleName, this);
            modules.put(moduleName, module);
        }
        return module;
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> found = null;
        if (name.startsWith("io.squark.nestedjarclassloader")) {
            found = super.loadClass(name, resolve);
        }
        if (name.startsWith("java.")) {
            found = getSystemClassLoader().loadClass(name);
        }
        if (found == null) {
            found = findModuleClass(name, resolve);
        }
        if (found == null) {
            try {
                found = super.loadClass(name, resolve);
            } catch (NullPointerException e) {
                //Do nothing
            }
        }
        if (found == null) {
            found = getSystemClassLoader().loadClass(name);
        }
        if (found != null) {
            return found;
        }
        throw new ClassNotFoundException(name);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        //Just return null here and handle throwing exception in loadClass.
        return null;
    }

    @Override
    public URL findResource(String name) {
        for (Module module : modules.values()) {
            Optional<URL> foundResource = module.findLocalResource(name);
            if (foundResource.isPresent()) {
                return foundResource.get();
            }
        }
        return null;
    }

    @Override
    public Enumeration<URL> findResources(String name) {
        Set<URL> combinedResources = null;
        for (Module module : modules.values()) {
            Set<URL> foundResources = module.findLocalResources(name);
            if (foundResources.size() > 0) {
                if (combinedResources == null) {
                    combinedResources = new LinkedHashSet<>();
                }
                combinedResources.addAll(foundResources);
            }
        }
        if (combinedResources != null) {
            return Collections.enumeration(combinedResources);
        }
        return Collections.emptyEnumeration();
    }

    private Class<?> findModuleClass(String name, boolean resolve) throws ClassNotFoundException {
        for (Module module : modules.values()) {
            Class<?> found = module.findLocalClass(name, resolve);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    @SuppressWarnings("unused")
    public void unloadModule(String loggingModuleName) {
        if (modules.containsKey(loggingModuleName)) {
            Module unloaded = modules.remove(loggingModuleName);
            unloaded.cleanUp();
        }
    }
}
