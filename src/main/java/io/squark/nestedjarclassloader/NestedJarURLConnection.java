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

import com.google.common.io.FileBackedOutputStream;
import sun.net.www.ParseUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

public class NestedJarURLConnection extends URLConnection implements AutoCloseable {

    private final boolean isDirectory;
    private URL jarFileURL;
    private String entryName;
    private boolean connected;
    private String subEntryName;
    private String file;
    private FileBackedOutputStream entryOutputStream;
    private FileBackedOutputStream subEntryOutputStream;

    /**
     * Constructs a URL connection to the specified URL. A connection to
     * the object referenced by the URL is not created.
     *
     * @param url the specified URL.
     */
    public NestedJarURLConnection(URL url, boolean connect, boolean isDirectory) throws IOException {
        super(url);
        this.isDirectory = isDirectory;
        parseSpecs(url);
        if (connect) {
            connect();
        }
    }

    /**
     * Modified from java.net.JarURLConnection
     *
     * @param url URL to parse
     * @throws MalformedURLException
     */
    private void parseSpecs(URL url) throws MalformedURLException {
        String spec = url.getFile();

        if (spec.startsWith("jar:")) {
            spec = spec.substring(4, spec.length());
        }

        int separator = spec.indexOf("!/");

        jarFileURL = new URL(spec.substring(0, separator++));
        entryName = null;

        /* if ! is the last letter of the innerURL, entryName is null */
        if (++separator != spec.length()) {
            entryName = spec.substring(separator, spec.length());
            entryName = ParseUtil.decode(entryName);
            int subEntrySeparator = entryName.indexOf("!/");
            if (subEntrySeparator != -1) {
                subEntryName = entryName.substring(subEntrySeparator + 2, entryName.length());
                entryName = entryName.substring(0, subEntrySeparator);
            }
        }
    }

    @Override
    public void connect() throws IOException {
        file = jarFileURL.getFile();
        if (entryName != null) {
            JarFile jarFile = new JarFile(file);
            InputStream is = jarFile.getInputStream(jarFile.getEntry(entryName));
            int len;
            byte[] b = new byte[2048];
            entryOutputStream = new FileBackedOutputStream((int) Runtime.getRuntime().freeMemory() / 2, true);

            while ((len = is.read(b)) > 0) {
                entryOutputStream.write(b, 0, len);
            }

            is.close();
            if (subEntryName != null) {
                JarInputStream entryInputStream =
                    new JarInputStream(entryOutputStream.asByteSource().openBufferedStream());
                JarEntry subEntry;
                while ((subEntry = entryInputStream.getNextJarEntry()) != null) {
                    if (subEntry.getName().equals(subEntryName)) {
                        subEntryOutputStream =
                          new FileBackedOutputStream((int) Runtime.getRuntime().freeMemory() / 2, true);
                        if (subEntry.isDirectory()) {
                            JarInputStream newEntryInputStream =
                              new JarInputStream(entryOutputStream.asByteSource().openBufferedStream());
                            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(subEntryOutputStream);
                            JarEntry file;
                            while ((file = newEntryInputStream.getNextJarEntry()) != null) {
                                if (file.getName().startsWith(subEntryName)) {
                                    Path path;
                                    if ((path = Paths.get(file.getName())).getNameCount() - Paths.get(subEntryName).getNameCount() == 1) {
                                        outputStreamWriter.append(path.getFileName().toString()).append('\n');
                                    }
                                }
                            }
                            outputStreamWriter.close();
                            newEntryInputStream.close();
                        } else {
                            b = new byte[2048];
                            while ((len = entryInputStream.read(b)) > 0) {
                                subEntryOutputStream.write(b, 0, len);
                            }
                            break;
                        }
                    }
                }
                entryInputStream.close();
                entryOutputStream.reset();
                entryOutputStream.close();
                entryOutputStream = null;
            }
        }
        connected = true;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (!connected) {
            connect();
        }
        if (subEntryName != null) {
            if (subEntryOutputStream != null) {
                return subEntryOutputStream.asByteSource().openBufferedStream();
            } else {
                throw new IOException("Failed to load " + subEntryName);
            }
        } else if (entryName != null) {
            if (entryOutputStream != null) {
                return entryOutputStream.asByteSource().openBufferedStream();
            } else {
                throw new IOException("Failed to load " + entryName);
            }
        } else {
            return new JarInputStream(new FileInputStream(file));
        }
    }

    @Override
    public void close() throws IOException {
        if (this.subEntryOutputStream != null) {
            this.subEntryOutputStream.reset();
            this.subEntryOutputStream.close();
            this.subEntryOutputStream = null;
        }
        if (this.entryOutputStream != null) {
            this.entryOutputStream.reset();
            this.entryOutputStream.close();
            this.entryOutputStream = null;
        }
    }
}
