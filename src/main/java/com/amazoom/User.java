package com.amazoom;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.j256.ormlite.table.TableUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.naming.AuthenticationException;
import javax.naming.NameNotFoundException;
import java.sql.SQLException;
import java.util.List;

@Controller
@DatabaseTable
public class User extends Editable<String> {
    static {
        try {
            TableUtils.createTableIfNotExists(Server.db, User.class);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @DatabaseField(id = true)
    private String username;

    @DatabaseField(canBeNull = false)
    private String password;

    @DatabaseField(canBeNull = false)
    private String name;

    @DatabaseField(canBeNull = false)
    private String address;

    @DatabaseField
    private boolean admin = false;

    @DatabaseField(foreign = true, canBeNull = false)
    private Cart cart;

    /** ONLY FOR USE BY ORMLITE, DO NOT USE THIS CONSTRUCTOR DIRECTLY */
    User() {}

    User(String username, String password, String name, String address) {
        this(username, password, name, address, false);
    }

    User(String username, String password, String name, String address, Boolean admin) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.address = address;
        this.admin = admin;
        this.cart = new Cart(this);

        try {
            User.dao().create(this);
        } catch(SQLException e) {
            throw new RuntimeException(e);
        }
    }

    static public Dao<User, String> dao() throws SQLException {
        return Updatable.dao(User.class);
    }

    @Override
    protected void update() {
        try {
            User.dao().update(this);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // -- GETTERS --
    public String getUsername() { return this.username; }
    public String getPassword() { return this.password; }
    public String getName() { return this.name; }
    public String getAddress() { return this.address; }
    public boolean isAdmin() { return this.admin; }
    public Cart getCart() {
        try {
            Cart.dao().refresh(this.cart);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return this.cart;
    }

    // -- SETTERS (will force a database update) --
    public void updatePassword(String password) {
        this.password = password;
        this.update();
    }

    public void updateName(String name) {
        this.name = name;
        this.update();
    }

    public void updateAddress(String address) {
        this.address = address;
        this.update();
    }

    public void updateAdmin(boolean newAdmin) {
        this.admin = newAdmin;
        this.update();
    }

    // -- WEB RESPONDERS --

    @GetMapping("/user")
    public static String user(
            @RequestParam(name = "username", required = false) String username,
            Model model,
            HttpSession session
    ) {
        // Check that this is the user's own page or that the user is an admin
        User loggedIn = (User) session.getAttribute("authenticated");
        if (loggedIn != null && username == null)
            username = loggedIn.getUsername();
        if (loggedIn == null || (!loggedIn.getUsername().equals(username) && !loggedIn.isAdmin())) {
            model.addAttribute("error", "You are not authorized to view this page");
            return "error";
        }

        // Retrieve the User object specified by `username` from the database
        User user;
        try {
            user = User.dao().queryForId(username);
            if (user == null)
                throw new NameNotFoundException("User not found");
        } catch (NameNotFoundException|SQLException e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }

        // Make the User object available to the view through the model
        model.addAttribute("user", user);

        // Return the view name
        return "user";
    }

    @GetMapping("/users")
    public static String users(Model model, HttpSession session) {
        // Check that the user is an admin
        User loggedIn = (User) session.getAttribute("authenticated");
        if (loggedIn == null || !loggedIn.isAdmin()) {
            model.addAttribute("error", "You are not authorized to view this page");
            return "error";
        }

        // Retrieve all users from the database
        List<User> users;
        try {
            users = User.dao().queryForAll();
        } catch (SQLException e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }

        // Make the list of users available to the view through the model
        model.addAttribute("users", users);

        // Return the view name
        return "users";
    }

    @GetMapping("/login")
    public static String login(Model model, HttpSession session) {
        if (session.getAttribute("authenticated") != null) {
            model.addAttribute("error", "You are already logged in!");
        }
        return "login";
    }

    /**
     * Given a username and a password, determine if the credentials are correct.
     * Side effects: if the credentials are correct, the `authenticated` field of the session will be set to the User
     *               object.
     *
     * @return True if the credentials are correct, False otherwise
     */
    @GetMapping("/authenticate")
    public static String authenticate(
            @RequestParam(name = "username") String username,
            @RequestParam(name = "password") String password,
            Model model,
            HttpSession session
    ) {
        // Check if the user is logged in ...
        if (session.getAttribute("authenticated") != null) {
            // ... and if they are, return an error
            model.addAttribute("error", "You are already logged in!");
            return "error";
        }

        // ... if they aren't, check if the credentials are correct ...
        User user;
        try {
            user = User.dao().queryForId(username);
            if (user == null || !user.getPassword().equals(password))
                throw new AuthenticationException( "could not authenticate - invalid username or password.");
        } catch (SQLException|AuthenticationException e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }

        // ... if they are, set the session's authenticated field to the user and return a success message
        model.addAttribute("message", "You are now logged in");
        session.setAttribute("authenticated", user);

        return "success";
    }

    @GetMapping("/logout")
    public static String logout(Model model, HttpSession session) {
        session.removeAttribute("authenticated");
        model.addAttribute("message", "You are now logged out");
        return "success";
    }

    @GetMapping("/signup")
    public static String signup(Model model, HttpSession session) {
        if (session.getAttribute("authenticated") != null) {
            model.addAttribute("error", "You are already logged in!");
            return "error";
        }
        return "signup";
    }

    @GetMapping("/doSignup")
    public static String doSignup(
            @RequestParam(name = "username") String username,
            @RequestParam(name = "password") String password,
            @RequestParam(name = "name") String name,
            @RequestParam(name = "address") String address,
            Model model,
            HttpSession session
    ) {
        if (username.equals("") || password.equals("") || name.equals("") || address.equals("")) {
            model.addAttribute("error", "Please fill out all fields");
            return "error";
        }

        if (session.getAttribute("authenticated") != null) {
            model.addAttribute("error", "You are already logged in!");
            return "error";
        }

        try {
            if (User.dao().queryForId(username) != null) {
                model.addAttribute("error", "Username already exists");
                return "error";
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        new User(username, password, name, address);
        model.addAttribute("message", "You are now signed up, please log in");
        return "success";
    }

    @Override
    @GetMapping("/user/editor")
    public String editor(
            @RequestParam(name = "username", required = false) String username,
            Model model,
            HttpSession session
    ) {
        // Get the logged-in user from the session data
        User loggedIn = (User) session.getAttribute("authenticated");

        // Set the username if we can and weren't given an argument for it
        if (username == null && loggedIn != null)
            username = loggedIn.getUsername();

        if (loggedIn == null || (!loggedIn.getUsername().equals(username) && !loggedIn.isAdmin())) {
            model.addAttribute("error", "You are not authorized to view this page");
            return "error";
        }

        // Retrieve the User object specified by `username` from the database
        User user;
        try {
            user = User.dao().queryForId(username);
            if (user == null)
                throw new NameNotFoundException("User not found");
        } catch (NameNotFoundException|SQLException e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }

        // Make the username and object fields available to the view through the model
        model.addAttribute("id", username);
        model.addAttribute("triples", List.of(
                new EditInfo("Username", user.getUsername(), false),
                new EditInfo("Password", user.getPassword(), true),
                new EditInfo("Name", user.getName(), true),
                new EditInfo("Address", user.getAddress(), true)
        ));

        // Return the view name
        return "editor";
    }

    @Override
    @GetMapping("/user/edit")
    public String edit(
            @RequestParam(name = "id", required = false) String username,
            @RequestParam(name = "key") List<String> keys,
            @RequestParam(name = "value") List<String> newValues,
            Model model,
            HttpSession session
    ) {
        // Get the logged-in user from the session data
        User loggedIn = (User) session.getAttribute("authenticated");

        // Set the username if we can and weren't given an argument for it
        if (username == null && loggedIn != null)
            username = loggedIn.getUsername();

        if (loggedIn == null || (!loggedIn.getUsername().equals(username) && !loggedIn.isAdmin())) {
            model.addAttribute("error", "You are not authorized to view this page");
            return "error";
        }

        if (keys.size() != newValues.size()) {
            model.addAttribute("error", "Invalid request");
            return "error";
        }

        // Get the User object specified by `username` from the database
        User user;
        try {
            user = User.dao().queryForId(username);
            if (user == null)
                throw new NameNotFoundException("User not found");
        } catch (NameNotFoundException|SQLException e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }

        // Update the user's fields
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String newValue = newValues.get(i);
            switch (key) {
                case "name" -> user.updateName(newValue);
                case "address" -> user.updateAddress(newValue);
                case "password" -> user.updatePassword(newValue);
                default -> {
                    model.addAttribute("error", "Invalid request");
                    return "error";
                }
            }
        }

        if (loggedIn.getUsername().equals(username))
            session.setAttribute("authenticated", user);

        model.addAttribute("message", "User updated");
        return "success";
    }

    @Override
    @GetMapping("/user/delete")
    public String delete(@RequestParam(name = "username", required = false) String username, Model model, HttpSession session) {
        // Get the logged-in user from the session data
        User loggedIn = (User) session.getAttribute("authenticated");

        // Set the username if we can and weren't given an argument for it
        if (username == null && loggedIn != null)
            username = loggedIn.getUsername();

        if (loggedIn == null || (!loggedIn.getUsername().equals(username) && !loggedIn.isAdmin())) {
            model.addAttribute("error", "You are not authorized to view this page");
            return "error";
        }

        if (loggedIn.getUsername().equals(username)) {
            session.removeAttribute("authenticated");
        }

        return Editable.delete(User.class, username, model, session);
    }
}
