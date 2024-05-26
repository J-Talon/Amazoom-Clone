package com.amazoom;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import org.springframework.stereotype.Controller;

import java.sql.SQLException;
import java.util.Objects;

/**
 * Abstraction for all classes that can be updated within the database
 */
@Controller
abstract public class Updatable {
    /** When implemented, updates this object in the {@link Server#db database} */
    abstract protected void update();

    /** Returns the {@link com.j256.ormlite.dao.Dao} given a specific class */
    static protected <T, U> Dao<T, U> dao(Class<T>  clazz) throws SQLException {
        Dao<T, U> dao = DaoManager.lookupDao(Server.db, clazz);
        if (Objects.isNull(dao))
            dao = DaoManager.createDao(Server.db, clazz);

        return dao;
    }
}
