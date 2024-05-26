package com.amazoom;


import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

@DatabaseTable
public class PartialCart extends PartialProductCollection {
    static {
        try {
            TableUtils.createTableIfNotExists(Server.db, PartialCart.class);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @DatabaseField(generatedId = true)
    private int id;

    /** The owning cart of this {@link PartialCart} */
    @DatabaseField(canBeNull = false, foreign = true, uniqueCombo = true)
    private Cart parent;

    /** DO NOT USE THIS CONSTRUCTOR. It is only for ORMLite */
    public PartialCart() { super(); }

    public PartialCart(Product product, int quantity, Cart parent) {
        super(product, quantity);
        this.parent = parent;

        try {
            PartialCart.dao().create(this);
        } catch(SQLException e) {
            throw new RuntimeException(e);
        }
    }

    static public Dao<PartialCart, Integer> dao() throws SQLException {
        return Updatable.dao(PartialCart.class);
    }

    @Override
    public void update() {
        try {
            PartialCart.dao().update(this);
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
