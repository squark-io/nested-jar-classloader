package io.hakansson.dynamicjar.nestedjarclassloader.exception;

/**
 * dynamicjar
 * <p>
 * Created by Erik Håkansson on 2016-06-23.
 * Copyright 2016
 */
public class NestedJarClassLoaderException extends Exception {
    public NestedJarClassLoaderException() {
    }

    public NestedJarClassLoaderException(String message) {
        super(message);
    }

    public NestedJarClassLoaderException(String message, Throwable cause) {
        super(message, cause);
    }

    public NestedJarClassLoaderException(Throwable cause) {
        super(cause);
    }

    public NestedJarClassLoaderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
