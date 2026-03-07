package com.alphaautoleak.jnvm.asm;

/**
 * Bootstrap method argument type marker
 */
public enum ArgType {
    STRING,       // regular string
    INTEGER,      // int
    LONG,         // long
    FLOAT,        // float
    DOUBLE,       // double
    METHOD_TYPE,  // MethodType descriptor string
    METHOD_HANDLE // MethodHandle reference
}
