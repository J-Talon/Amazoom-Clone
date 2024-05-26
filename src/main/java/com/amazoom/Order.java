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

import javax.management.InstanceNotFoundException;
import javax.naming.NameNotFoundException;
import java.sql.SQLException;
import java.util.HashMap;

/**
 @author Rock, Jiang
 Models an order object for the system.
 */
@Controller
@DatabaseTable(tableName = "orders")
public class Order extends ProductCollection<PartialOrder> {
    static {
        try {
            TableUtils.createTableIfNotExists(Server.db, Order.class);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(canBeNull = false, foreign = true)
    private User user;

    public Order(User user) {
        this.user = user;

        try {
            this.parts = Order.dao().getEmptyForeignCollection("parts");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        try {
            Order.dao().create(this);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        for (PartialCart partialCart : user.getCart().parts) {
            new PartialOrder(partialCart.getProduct(), partialCart.getQuantity(), this);
            user.getCart().remove(partialCart.getProduct());
        }

        this.update();
    }

    /** DO NOT USE THIS CONSTRUCTOR. It is only used by ORMLite */
    public Order() {}

    @Override
    protected void update() {
        try {
            Order.dao().update(this);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    static public Dao<Order, Integer> dao() throws SQLException {
        return Updatable.dao(Order.class);
    }



    // -- GETTERS --
    public int getId() {
        return id;
    }

    // -- WEB RESPONDERS --

    @GetMapping("/order")
    public static String order(
            @RequestParam(name="id") Integer id,
            Model model,
            HttpSession session
    ) {
        Order order;
        try {
            order = Order.dao().queryForId(id);
        } catch (SQLException e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
        if (order == null) {
            model.addAttribute("error", "Order not found");
            return "error";
        }

        User user = (User) session.getAttribute("authenticated");
        if (user == null || !(user.getUsername().equals(order.user.getUsername()) || user.isAdmin())) {
            model.addAttribute("error", "You are not authorized to view this order");
            return "error";
        }
        if (user.isAdmin())
            model.addAttribute("admin", true);

        model.addAttribute("order", order);
        return "order";
    }

    @GetMapping("/orders")
    public static String orders(
            @RequestParam(name = "username", required = false) String username,
            Model model,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("authenticated");
        if (username == null && user != null)
            username = user.getUsername();
        try {
            if (user == null || !(username.equals(user.getUsername()) || user.isAdmin()))
                throw new SecurityException("You are not authorized to view this page");
            else if (user.isAdmin()) {
                model.addAttribute("admin", true);
                user = User.dao().queryForId(username);
                if (user == null)
                    throw new NameNotFoundException("User not found");
            }


            // Find all orders for the user
            HashMap<String, Object> fields = new HashMap<>();
            fields.put("user_id", user.getUsername());
            model.addAttribute("orders", Order.dao().queryForFieldValues(fields));

            // Add the username to the model so the view can show different headers depending on whose orders are being shown
            model.addAttribute("username", username);

            return "orders";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    @GetMapping("/cancel")
    public static String cancel(@RequestParam(name="id") int id, Model model, HttpSession session) {
        try {
            Order order = Order.dao().queryForId(id);
            if (order == null)
                throw new InstanceNotFoundException("Order not found");

            User user = (User) session.getAttribute("authenticated");
            if (user == null || !(user.getUsername().equals(order.user.getUsername()) || user.isAdmin()))
                throw new SecurityException("You are not authorized to cancel this order");

            Order.dao().delete(order);

            return "success";
        } catch (SQLException | InstanceNotFoundException | SecurityException e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }
}
