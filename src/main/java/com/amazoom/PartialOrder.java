package com.amazoom;


import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

@DatabaseTable
public class PartialOrder extends PartialProductCollection {
    static {
        try {
            TableUtils.createTableIfNotExists(Server.db, PartialOrder.class);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** This is the owning Order of the PartialOrder */
    @DatabaseField(canBeNull = false, foreign = true, uniqueCombo = true)
    private Order parent;

    //For ORMLite
    public PartialOrder(){

    }

    public PartialOrder(Product product, int quantity, Order parent) {
        super(product, quantity);
        this.parent = parent;
        try {
            PartialOrder.dao().create(this);
        } catch(SQLException e) {
            throw new RuntimeException(e);
        }
    }

    static public Dao<PartialOrder, Integer> dao() throws SQLException {
        return Updatable.dao(PartialOrder.class);
    }

    @Override
    public void update() {
        try {
            PartialOrder.dao(PartialOrder.class).update(this);
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
