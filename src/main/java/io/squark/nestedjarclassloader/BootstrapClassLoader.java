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

    public BootstrapClassLoader(URL[] baseUrls) throws Exception {
        super(baseUrls, null);
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
