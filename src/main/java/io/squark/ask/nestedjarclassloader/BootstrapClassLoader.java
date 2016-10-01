package io.squark.ask.nestedjarclassloader;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Created by Erik Håkansson on 2016-06-28.
 * WirelessCar
 */
public class BootstrapClassLoader extends URLClassLoader {

    public BootstrapClassLoader(URL baseUrl) throws Exception {
        super(new URL[] {baseUrl}, null);
    }


    @SuppressWarnings("unused")
    public void addURLs(URL[] urls) {
        for (URL url : urls) {
            addURL(url);
        }
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }
}
