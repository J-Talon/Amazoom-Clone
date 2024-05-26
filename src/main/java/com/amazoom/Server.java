package com.amazoom;

import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.sql.SQLException;

@SpringBootApplication
public class Server {
    final public static ConnectionSource db;

    static {
        try {
            // Open database connection
            db = new JdbcConnectionSource("jdbc:sqlite:amazoom.sqlite");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public static void main(String[] args) {
        SpringApplication.run(Server.class, args);
    }
}