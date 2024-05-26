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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 @author Rock, Jiang
  Models a cart object for the system.
 */
@Controller
@DatabaseTable
public class Cart extends ProductCollection<PartialCart> {
    static {
        try {
            TableUtils.createTableIfNotExists(Server.db, Cart.class);
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * We will use a username as the id in the SQL tables.
     * When we need to get the cart's owning user, then we can use a lookup Dao to get it from the database.
     */
    @DatabaseField(id = true)
    private String username;

    public Cart(User user) throws RuntimeException {

        // Set the username to the `User`'s username
        this.username = user.getUsername();

        try {
            //instantiate the partialCollection
            this.parts = Cart.dao().getEmptyForeignCollection("parts");
            Cart.dao().create(this);
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** DO NOT USE THIS CONSTRUCTOR. It is only for ORMLite */
    public Cart() {}

    // -- GETTERS --
    public String getUsername() {
        return this.username;
    }

    /**
     * Check if a product is in the cart by ID
     *
     * @return the product if it is in the cart, null otherwise
     */
    private PartialCart contains(Product product) {
        Map<String, Object> fieldValues = new HashMap<>();
        fieldValues.put("parent_id", this.username);
        fieldValues.put("product_id", product.getId());
        List<PartialCart> partialMatching;
        try {
            partialMatching = PartialCart.dao().queryForFieldValues(fieldValues);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return partialMatching.isEmpty() ? null : partialMatching.get(0);
    }

    /** Add one `product` to the collection */
    public void add(Product product) {
        add(product, 1);
    }

    /** Add `quantity` `product`s to the collection */
    public void add(Product product, int quantity) {
        // Check if this product is already in the cart ...
        PartialCart partial = this.contains(product);

        // ... if not, then add it to the cart ...
        if (partial == null) {
            new PartialCart(product, quantity, this);
            this.update();
        } else
            // ... if so, then update the quantity
            partial.updateQuantity(partial.getQuantity() + quantity);
    }

    /**
     * Remove `quantity` `product`s from the collection
     *
     * @param product The product to remove.
     *                This product must be in the collection or an error will be thrown.
     * @param quantity The quantity of `product`s to remove.
     *                 If this quantity is greater than or equal to the quantity in the collection,
     *                 then all `product`s will be removed.
     */
    public void remove(Product product, int quantity) {
        // Check if the product is in the cart, if not then throw an error
        PartialCart partial = this.contains(product);
        if (partial == null)
            throw new RuntimeException("cannot remove a product that is not in the cart");

        // Check if `quantity` is greater than or equal to the quantity in the cart ...
        if (quantity >= partial.getQuantity()) {
            // ... if so, remove the product from the cart ...
            this.remove(product);
        } else {
            // ... if not, then update the quantity
            partial.updateQuantity(partial.getQuantity() - quantity);
        }
    }

    /**
     * Remove `product` from the collection
     *
     * @param product The product to remove.
     *                This product must be in the collection or an error will be thrown.
     */
    public void remove(Product product) {
        // Check if the product is in the cart, if not then throw an error
        PartialCart partial = this.contains(product);
        if (partial == null)
            throw new RuntimeException("cannot remove a product that is not in the cart");

        // Remove the product from the cart
        this.parts.remove(partial);
        try {
            PartialCart.dao().delete(partial);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        this.update();
    }

    @Override
    protected void update() {
        try {
            Cart.dao().update(this);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Dao<Cart, String> dao() throws SQLException {
        return Updatable.dao(Cart.class);
    }

    /** Empties the cart. */
    public void clear() {
        for (PartialCart partial : this.parts)
            this.remove(partial.getProduct(), partial.getQuantity());
    }

    // -- WEB RESPONDERS --

    @GetMapping("/cart")
    public static String cart(
            @RequestParam(name = "username", required = false) String username,
            Model model,
            HttpSession session
    ) {
        model.addAttribute("admin", false);
        User user = (User) session.getAttribute("authenticated");

        if (user == null) {
            model.addAttribute("error", "You must be logged in to view a cart.");
            return "error";
        } else if (username != null && !username.equals(user.getUsername())) {
            try {
                if (!user.isAdmin())
                    throw new AuthenticationException("You must be an admin to view another user's cart.");
                else
                    model.addAttribute("admin", true);
                user = User.dao().queryForId(username);
            } catch (Exception e) {
                model.addAttribute("error", e.getMessage());
                return "error";
            }
        }

        model.addAttribute("cart", user.getCart());
        model.addAttribute("isCart", true);
        return "cart";
    }

    private static String editCart(String action, int productID, Integer quantity, String username, Model model, HttpSession session) {
        // Get the user from the session
        User user = (User) session.getAttribute("authenticated");
        if (user == null) {
            model.addAttribute("error", "You must be logged in to modify a cart.");
            return "error";
        }

        // Check if username is defined (and doesn't match the current user)
        if (username != null && !username.equals(user.getUsername())) {
            // If so then verify that acting user is an admin
            try {
                if (!user.isAdmin())
                    throw new AuthenticationException("You must be an admin to modify another user's cart.");
                else
                    model.addAttribute("admin", true);
                user = User.dao().queryForId(username);
            } catch (Exception e) {
                model.addAttribute("error", e.getMessage());
                return "error";
            }
        }

        // Get the product
        Product product;
        try {
            product = Product.dao().queryForId(productID);
            if (product == null)
                throw new NameNotFoundException("Product not found.");
        } catch (SQLException | NameNotFoundException e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }

        // Get the cart
        Cart cart = user.getCart();

        // Add the product to the cart (report errors if they occur)
        try {
            if (quantity == null)
                Cart.class.getMethod(action, Product.class).invoke(cart, product);
            else
                Cart.class.getMethod(action, Product.class, int.class).invoke(cart, product, quantity);
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }

        // Redirect to the cart
        return "redirect:/cart" + (username == null ? "" : "?username=" + username);
    }

    @GetMapping("/addToCart")
    public static String addToCart(
            @RequestParam(name = "id") int productID,
            @RequestParam(name = "quantity", required = false) Integer quantity,
            @RequestParam(name = "username", required = false) String username,
            Model model,
            HttpSession session
    ) {
        return editCart("add", productID, quantity, username, model, session);
    }

    @GetMapping("/removeFromCart")
    public static String removeFromCart(
            @RequestParam(name = "id") int productID,
            @RequestParam(name = "quantity", required = false) Integer quantity,
            @RequestParam(name = "username", required = false) String username,
            Model model,
            HttpSession session
    ) {
        return editCart("remove", productID, quantity, username, model, session);
    }

    /** Creates an order object, empties the cart, and commits the order into the orders table in the database */
    @GetMapping("/checkout")
    public static String checkout(
            @RequestParam(name = "username", required = false) String username,
            Model model,
            HttpSession session) {
        // Get the user from the session
        User user = (User) session.getAttribute("authenticated");

        // Return an error if the user is not logged in
        if (user == null) {
            model.addAttribute("error", "You must be logged in to checkout.");
            return "error";
        }

        // Check if username is defined (and doesn't match the current user)
        if (username != null && !username.equals(user.getUsername())) {
            // If so then verify that acting user is an admin
            try {
                if (!user.isAdmin())
                    throw new NameNotFoundException("You must be an admin to checkout another user's cart.");
                else
                    model.addAttribute("admin", true);
                user = User.dao().queryForId(username);
            } catch (Exception e) {
                model.addAttribute("error", e.getMessage());
                return "error";
            }
        }

        // Return an error if the cart is empty
        if (user.getCart().getParts().isEmpty()) {
            model.addAttribute("error", "You cannot checkout with an empty cart.");
            return "error";
        }

        // Create an order from the cart
        Order order = new Order(user);

        // Return the checkout page
        model.addAttribute("message", "Order #" + order.getId() + " has been placed!");
        return "success";
    }
}

