package io.hakansson.dynamicjar.nestedjarclassloader;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * dynamicjar
 * <p>
 * Created by Erik Håkansson on 2016-04-26.
 * Copyright 2016
 */
public class NestedJarURLStreamHandler extends URLStreamHandler {

    private NestedJarURLConnection nestedJarURLConnection;

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        if (nestedJarURLConnection == null) {
            nestedJarURLConnection = new NestedJarURLConnection(url, true);
        }
        return nestedJarURLConnection;
    }
}
