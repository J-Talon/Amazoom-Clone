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

import javax.naming.NameNotFoundException;
import java.sql.SQLException;
import java.util.List;

@Controller
@DatabaseTable
public class Product extends Editable<Integer> {
    static {
        try {
            TableUtils.createTableIfNotExists(Server.db, Product.class);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(unique = true, canBeNull = false)
    private String name;

    @DatabaseField(canBeNull = false)
    private float price;

    /** ONLY FOR USE BY ORMLITE, DO NOT USE THIS CONSTRUCTOR DIRECTLY */
    Product() {}

    Product(String name, Float price) {
        this.name = name;
        this.price = price;

        try {
            Product.dao().create(this);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    static public Dao<Product, Integer> dao() throws SQLException {
        return Updatable.dao(Product.class);
    }

    @Override
    protected void update() {
        try {
            Product.dao().update(this);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // -- GETTERS --

    public int getId() { return id; }
    public String getName() { return name; }
    public float getPrice() { return price; }

    // -- SETTERS (will force a database update) --

    public void updateName(String name) {
        this.name = name;
        this.update();
    }

    public void updatePrice(Float price) {
        this.price = price;
        this.update();
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof Product) && ((Product)obj).getId() == id;
    }
    
    // -- WEB RESPONDERS --

    @GetMapping("/product")
    public static String product(
            @RequestParam(name = "id") Integer id,
            Model model,
            HttpSession session
    ) {
        try {
            model.addAttribute("product", Product.dao().queryForId(id));
            if (model.getAttribute("product") == null)
                throw new NameNotFoundException(String.format("Found no products with ID '%d'", id));
        } catch (SQLException|NameNotFoundException e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }

        User user = (User) session.getAttribute("authenticated");
        model.addAttribute("admin", user != null && user.isAdmin());

        return "product";
    }

    @GetMapping("/products")
    public static String products(Model model, HttpSession session) {
        // Retrieve all products from database, make available to the view through the model
        try {
            model.addAttribute("products", Product.dao().queryForAll());
        } catch (SQLException e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }

        return "products";
    }

    @Override
    @GetMapping("/product/editor")
    public String editor(@RequestParam(name = "id") Integer id, Model model, HttpSession session) {
        // Check that user is admin
        User user = (User) session.getAttribute("authenticated");
        if (user == null || !user.isAdmin()) {
            model.addAttribute("error", "You must be an admin to access this page");
            return "error";
        }

        // Get product from database, throw if it can't be found
        Product product;
        try {
            product = Product.dao().queryForId(id);
            if (product == null)
                throw new NameNotFoundException(String.format("Found no products with ID '%d'", id));
        } catch (SQLException|NameNotFoundException e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }

        // Make product ID and product fields available to the view through the model
        model.addAttribute("id", product.getId());
        model.addAttribute("triples", List.of(
                new EditInfo("id", ((Integer)product.getId()).toString(), false),
                new EditInfo("name", product.getName(), true),
                new EditInfo("price", ((Float)product.price).toString(), true)
        ));

        return "editor";
    }

    @Override
    @GetMapping("/product/edit")
    public String edit(
            @RequestParam(name = "id") Integer id,
            @RequestParam(name = "key") List<String> keys,
            @RequestParam(name = "value") List<String> newValues,
            Model model,
            HttpSession session
    ) {
        // Check that user is admin
        User user = (User) session.getAttribute("authenticated");
        if (user == null || !user.isAdmin()) {
            model.addAttribute("error", "You must be an admin to access this page");
            return "error";
        }

        // Get product from database, throw if it can't be found
        Product product;
        try {
            product = Product.dao().queryForId(id);
            if (product == null)
                throw new NameNotFoundException(String.format("Found no products with ID '%d'", id));
        } catch (SQLException|NameNotFoundException e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }

        // Update product fields
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String newValue = newValues.get(i);
            switch (key) {
                case "name" -> product.updateName(newValue);
                case "price" -> product.updatePrice(Float.parseFloat(newValue));
                case "id" -> {
                    model.addAttribute("error", "Cannot edit product ID");
                    return "error";
                }
                default -> {
                    model.addAttribute("error", String.format("Unknown key '%s'", key));
                    return "error";
                }
            }
        }

        // Redirect to success page
        model.addAttribute("message", "Product successfully updated");
        return "success";
    }

    @Override
    @GetMapping("/product/delete")
    public String delete(@RequestParam(name = "id") Integer id, Model model, HttpSession session) {
        if (session.getAttribute("authenticated") == null || !((User)session.getAttribute("authenticated")).isAdmin()) {
            model.addAttribute("error", "You must be an admin to access this page");
            return "error";
        }

        return Editable.delete(Product.class, id, model, session);
    }
}
