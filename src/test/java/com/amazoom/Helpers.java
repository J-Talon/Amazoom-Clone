package com.amazoom;

public class Helpers {
    public static boolean checkField(Object parent, String field, Object expected) throws Exception {
        var actual = parent.getClass()
                .getMethod("get" + field.substring(0, 1).toUpperCase() + field.substring(1))
                .invoke(parent);
        var actualClass = actual.getClass();
        if (actualClass == expected.getClass()) {
            return actual.equals(expected);
        } else {
            return actual.equals(actualClass.getMethod("valueOf", expected.getClass()).invoke(null, expected));
        }
    }
}
