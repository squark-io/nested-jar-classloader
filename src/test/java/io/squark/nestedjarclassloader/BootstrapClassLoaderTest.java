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

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URL;

/**
 * yggdrasil
 * <p>
 * Created by Erik Håkansson on 2016-11-01.
 * Copyright 2016
 */
public class BootstrapClassLoaderTest {
    @Test
    public void addURLs() throws Exception {
        URL[] urls = new URL[] {new File(".").toURI().toURL()};
        BootstrapClassLoader bootstrapClassLoader = new BootstrapClassLoader(urls);
        Assert.assertArrayEquals(urls, bootstrapClassLoader.getURLs());
        URL[] urls2 = new URL[]{new File("test1").toURI().toURL(), new File("test2").toURI().toURL()};
        bootstrapClassLoader.addURLs(urls2);
        Assert.assertArrayEquals(ArrayUtils.addAll(urls, urls2), bootstrapClassLoader.getURLs());
    }

    @Test
    public void addURL() throws Exception {
        URL url1 = new File(".").toURI().toURL();
        BootstrapClassLoader bootstrapClassLoader = new BootstrapClassLoader(url1);
        Assert.assertArrayEquals(new URL[] {url1}, bootstrapClassLoader.getURLs());
        URL url2 = new File("test").toURI().toURL();
        bootstrapClassLoader.addURL(url2);
        Assert.assertArrayEquals(new URL[] {url1, url2}, bootstrapClassLoader.getURLs());
    }

}