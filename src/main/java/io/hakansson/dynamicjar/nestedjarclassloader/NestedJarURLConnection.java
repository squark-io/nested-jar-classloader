package io.hakansson.dynamicjar.nestedjarclassloader;

import sun.net.www.ParseUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

/**
 * dynamicjar
 * <p>
 * Created by Erik Håkansson on 2016-04-26.
 * Copyright 2016
 */
public class NestedJarURLConnection extends URLConnection implements AutoCloseable {

    private URL jarFileURL;
    private String entryName;
    private boolean connected;
    private String subEntryName;
    private String file;
    private byte[] entryBytes;
    private byte[] subEntryBytes;


    /**
     * Constructs a URL connection to the specified URL. A connection to
     * the object referenced by the URL is not created.
     *
     * @param url the specified URL.
     */
    public NestedJarURLConnection(URL url) throws MalformedURLException {
        super(url);
        parseSpecs(url);
    }

    /**
     * Modified from java.net.JarURLConnection
     *
     * @param url URL to parse
     * @throws MalformedURLException
     */
    private void parseSpecs(URL url) throws MalformedURLException {
        String spec = url.getFile();

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
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            while ((len = is.read(b)) > 0) {
                byteArrayOutputStream.write(b, 0, len);
            }
            entryBytes = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.close();
            is.close();
            if (subEntryName != null && entryBytes != null) {
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(entryBytes);
                JarInputStream entryInputStream = new JarInputStream(byteArrayInputStream);
                JarEntry subEntry;
                while ((subEntry = entryInputStream.getNextJarEntry()) != null) {
                    if (subEntry.getName().equals(subEntryName)) {
                        byteArrayOutputStream = new ByteArrayOutputStream();
                        b = new byte[2048];
                        while ((len = entryInputStream.read(b)) > 0) {
                            byteArrayOutputStream.write(b, 0, len);
                        }
                        subEntryBytes = byteArrayOutputStream.toByteArray();
                        byteArrayOutputStream.close();
                        break;
                    }
                }
                entryInputStream.close();
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
            if (subEntryBytes != null) {
                return new ByteArrayInputStream(subEntryBytes);
            } else {
                throw new IOException("Failed to load " + subEntryName);
            }
        } else if (entryName != null) {
            if (entryBytes != null) {
                return new ByteArrayInputStream(entryBytes);
            } else {
                throw new IOException("Failed to load " + entryName);
            }
        } else {
            return new JarInputStream(new FileInputStream(file));
        }
    }

    @Override
    public void close() throws Exception {
        this.subEntryBytes = null;
        this.entryBytes = null;
    }
}
