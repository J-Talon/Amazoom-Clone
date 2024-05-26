package com.amazoom;

import io.cucumber.java.ParameterType;

public class Types {
    /** Matches the name of a class that extends {@link Updatable}, converts it to a {@link Class} type */
    @ParameterType("[A-Z][A-z]+")
    public Class<? extends Updatable> updatable(String match) throws ClassNotFoundException {
        return Class.forName("com.amazoom." + match).asSubclass(Updatable.class);
    }

    /** Matches the name of a class that extends {@link ProductCollection}, converts it to a {@link Class} type */
    @ParameterType(value = "[A-Z][A-z]+")
    public Class<? extends ProductCollection> productCollection(String match) throws ClassNotFoundException {
        return Class.forName("com.amazoom." + match).asSubclass(ProductCollection.class);
    }

    /** Matches the name of a class that implements {@link Editable}, converts it to a {@link Class} type */
    @ParameterType(value = "[A-Z][A-z]+")
    public Class<? extends Editable> editable(String match) throws ClassNotFoundException {
        return Class.forName("com.amazoom." + match).asSubclass(Editable.class);
    }


    /** Matches an ID, converts it to an Integer if possible */
    @ParameterType(".*")
    public Object id(String match) {
        // Attempt to convert the ID to an integer, if we can't then leave it as a string
        try {
            return Integer.valueOf(match);
        } catch (NumberFormatException ignored) {}
        return match;
    }
}
