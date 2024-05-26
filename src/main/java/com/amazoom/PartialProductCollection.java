package com.amazoom;


import com.j256.ormlite.field.DatabaseField;

/**
 * This class models a product wrapper class, which holds both the quantity and product type in the collection.
 */
public abstract class PartialProductCollection extends Updatable {
    @DatabaseField(uniqueCombo = true, foreign = true, foreignAutoRefresh = true)
    private Product product;

    @DatabaseField
    private int quantity;

    public Product getProduct() {
        return this.product;
    }

    public int getQuantity() {
        return this.quantity;
    }

    public void updateQuantity(int quantity) {
        this.quantity = quantity;
        this.update();
    }

    PartialProductCollection() {}

    PartialProductCollection(Product product) {
        this.product = product;
        this.quantity = 1;
    }

    PartialProductCollection(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }
}