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

import io.squark.yggdrasil.logging.api.InternalLoggerBinder;
import org.apache.commons.collections4.MultiMapUtils;
import org.apache.commons.collections4.SetValuedMap;
import org.slf4j.Logger;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

class Module extends ClassLoader {

    private final SetValuedMap<String, URL> resources = MultiMapUtils.newSetValuedHashMap();
    private final Map<String, byte[]> byteCache = new HashMap<>();
    private final Map<String, Class<?>> classes = new HashMap<>();
    private Logger logger = InternalLoggerBinder.getLogger(Module.class);
    private String name;

    Module(String name, NestedJarClassLoader parent) throws IOException {
        super(parent);
        this.name = name;
    }

    public void addResources(URL... urls) throws IOException {
        for (URL url : urls) {
            addResource0(url);
        }
    }

    private void addResource0(URL url) throws IOException {
        if (url.getPath().endsWith(".jar")) {
            logger.debug("Adding jar " + url.getPath());
            InputStream urlStream = url.openStream();
            BufferedInputStream bufferedInputStream = new BufferedInputStream(urlStream);
            JarInputStream jarInputStream = new JarInputStream(bufferedInputStream);
            JarEntry jarEntry;

            while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
                if (resources.containsKey(jarEntry.getName())) {
                    logger.trace(
                            "Already have resource " + jarEntry.getName() + ". If different versions, unexpected behaviour " +
                                    "might occur. Available in " + resources.get(jarEntry.getName()));
                }

                String spec;
                if (url.getProtocol().equals("jar")) {
                    spec = url.getPath();
                } else {
                    spec = url.getProtocol() + ":" + url.getPath();
                }
                URL contentUrl = new URL(null, "jar:" + spec + "!/" + jarEntry.getName(), new NestedJarURLStreamHandler(false));
                resources.put(jarEntry.getName(), contentUrl);
                addClassIfClass(jarInputStream, jarEntry.getName());
                logger.trace("Added resource " + jarEntry.getName() + " to ClassLoader");
                if (jarEntry.getName().endsWith(".jar")) {
                    addResource0(contentUrl);
                }
            }
            jarInputStream.close();
            bufferedInputStream.close();
            urlStream.close();
        } else if (url.getPath().endsWith(".class")) {
            throw new IllegalStateException("Cannot add classes directly");
        } else {
            try {
                addDirectory(new File(url.toURI()));
            } catch (URISyntaxException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private String resourceToClassName(String slashed) {
        return slashed.substring(0, slashed.lastIndexOf(".class")).replace("/", ".");
    }

    protected void addToByteCache(String className, byte[] classBytes) {
        byteCache.put(className, classBytes);
    }

    private void addClassIfClass(InputStream inputStream, String relativePath) throws IOException {
        if (relativePath.endsWith(".class")) {
            int len;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] b = new byte[2048];

            while ((len = inputStream.read(b)) > 0) {
                out.write(b, 0, len);
            }
            out.close();
            byte[] classBytes = out.toByteArray();
            String className = resourceToClassName(relativePath);
            addToByteCache(className, classBytes);
        }
    }

    private void addDirectory(File directory) throws IOException {
        if (!directory.isDirectory()) {
            throw new IllegalStateException("Not a directory: " + directory);
        }
        File[] files = directory.listFiles();
        if (files == null) {
            throw new IllegalStateException("No files found in " + directory);
        }
        for (File file : files) {
            if (file.isDirectory()) {
                addDirectory(file);
            } else if (file.getName().endsWith(".jar")) {
                try {
                    addResource0(file.toURI().toURL());
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            } else {
                try {
                    String relativeName = directory.toURI().relativize(file.toURI()).getPath();
                    FileInputStream fileInputStream = new FileInputStream(file);
                    addClassIfClass(fileInputStream, relativeName);
                    resources.put(relativeName, file.toURI().toURL());
                    fileInputStream.close();
                } catch (MalformedURLException | FileNotFoundException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> found = findLoadedClass(name);
            if (found != null) {
                return found;
            }
            if (name.startsWith("io.squark.nestedjarclassloader")) {
                return ((NestedJarClassLoader) getParent()).loadClass(name, resolve);
            }
            found = findLocalClass(name, resolve);
            if (found == null) {
                //Will cause redundancy, but unavoidable for now.
                found = ((NestedJarClassLoader) getParent()).loadClass(name, resolve);
            }
            return found;
        }
    }

    public Class<?> findLocalClass(String className, boolean resolve) throws ClassNotFoundException {
        return getLoadedClass(className, resolve);
    }

    private Class<?> getLoadedClass(String className, boolean resolve) throws ClassNotFoundException
    {
        synchronized (getClassLoadingLock(className)) {

            Class<?> loadedClass = findLoadedClass(className);
            if (classes.containsKey(className)) {
                return classes.get(className);
            }
            if (byteCache.containsKey(className)) {
                definePackageForClass(className);
                byte[] classBytes = byteCache.get(className);

                if (loadedClass == null) {
                    //We got here without Exception, meaning class was filtered from proxying. Load normally:
                    try {
                        loadedClass = defineClass(className, classBytes, 0, classBytes.length,
                                this.getClass().getProtectionDomain());
                    } catch (NoClassDefFoundError | IncompatibleClassChangeError e) {
                        throw new ClassNotFoundException(className, e);
                    }
                }
                classes.put(className, loadedClass);
                if (resolve) {
                    resolveClass(loadedClass);
                }
                return loadedClass;
            } else {
                return null;
            }
        }
    }

    private void definePackageForClass(String className) {
        int i = className.lastIndexOf('.');
        if (i != -1) {
            String pkgname = className.substring(0, i);
            //Check if already defined:
            Package pkg = getPackage(pkgname);
            if (pkg == null) {
                definePackage(pkgname, null, null, null, null, null, null, null);
            }
        }
    }

    Optional<URL> findLocalResource(String name) {
        Set<URL> foundResources = findLocalResources(name);
        if (foundResources.size() > 0) {
            return Optional.of(foundResources.iterator().next());
        }
        return Optional.empty();
    }

    Set<URL> findLocalResources(String name) {
        return resources.get(name);
    }

    public void cleanUp() {
        resources.clear();
        classes.clear();
        byteCache.clear();
    }

    @Override
    public String toString() {
        return "Module{" + "name='" + name + '\'' + '}';
    }
}
