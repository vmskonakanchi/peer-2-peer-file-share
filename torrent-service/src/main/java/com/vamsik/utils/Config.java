package com.vamsik.utils;

public class Config {
    public static final boolean IS_DEBUG =
            "debug".equalsIgnoreCase(System.getProperty("app.env"));
}
