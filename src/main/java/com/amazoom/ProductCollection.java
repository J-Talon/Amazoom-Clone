package com.amazoom;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.ForeignCollectionField;

/**
 * This class represents a collection of product wrapper classes
 * @param <T> The type of product wrapper to store
 */
public abstract class ProductCollection<T extends PartialProductCollection> extends Updatable {
    @ForeignCollectionField
    protected ForeignCollection<T> parts;

    public ForeignCollection<T> getParts() {
        return this.parts;
    }

    final public float total() {
        float total = 0;
        for (PartialProductCollection part : this.parts) {
            total += part.getProduct().getPrice() * part.getQuantity();
        }
        return total;
    }
}
