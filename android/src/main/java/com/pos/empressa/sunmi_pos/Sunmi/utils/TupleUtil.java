package com.pos.empressa.sunmi_pos.Sunmi.utils;

public final class TupleUtil {

    private TupleUtil() {
        throw new AssertionError();
    }

    public static <A, B> Tuple<A, B> tuple(A a, B b) {
        return new Tuple<>(a, b);
    }

}
