package com.amazoom;

import com.j256.ormlite.stmt.QueryBuilder;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.naming.AuthenticationException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Controller
public class Search {

    /** Definition of valid values for second argument to {@link Search#results} */
    enum By {
        NAME("NAME"), ID("ID");

        final public String string;

        By(String string) {
            this.string = string;
        }
    }

    @GetMapping("/")
    public static String search() {
        return "search";
    }

    @GetMapping("/results")
    public static String results(
            @RequestParam(name = "term") String term,
            @RequestParam(name = "by", defaultValue = "NAME") String searchBy,
            @RequestParam(name = "sortBy", defaultValue = "NAME") String sortBy,
            @RequestParam(name = "direction", defaultValue = "ASC") String sortDirection,
            Model model,
            HttpSession session
    ) {
        // Convert `searchBy` to `By` enum type to verify that it is valid
        By by = By.valueOf(searchBy.toUpperCase()); // .toUpperCase() to make case-insensitive

        // Throws if string is invalid (see `Sort` class below)
        Sort sort = new Sort(sortBy, sortDirection);

        // Define variable in which to store the query result
        List<Product> queryResult = new ArrayList<>();

        try {
            // Set sort params for queryBuilder
            QueryBuilder<Product, Integer> queryBuilder = Product.dao().queryBuilder().orderBy(sort.by, sort.direction);

            // Run the query
            User authenticated = (User) session.getAttribute("authenticated");
            switch (by) {
                case ID -> { // If `by` is `ID`, check that user is authenticated as admin
                    if (authenticated == null || !authenticated.isAdmin())
                        throw new AuthenticationException("unauthorized access");
                    else
                        queryResult = queryBuilder.where().eq(String.valueOf(by), term).query();
                }
                case NAME -> queryResult = queryBuilder.where().like(String.valueOf(by), "%" + term + "%").query();
            }
        } catch(SQLException|AuthenticationException e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }

        // Check if user is authenticated as admin and make that information available to the model
        User authenticated = (User) session.getAttribute("authenticated");
        model.addAttribute("admin", authenticated != null && authenticated.isAdmin());

        // Make the results available to the model and return the 'results' page
        model.addAttribute("results", queryResult);
        return "results";
    }
}

/**
 * Class that represents what column to use to sort {@link Search} results and what direction (ascending or
 * descending) they should be sorted in.
 *
 * @see Search#results
 */
class Sort {
    final public String by;
    final public boolean direction;

    final private static String invalidFormat = "invalid argument for '%s': '%s'";

    /**
     * Checks that arguments are valid and stores them if so.
     * Will convert `direction` to a boolean representation (ascending = true).
     *
     * @param by The database column to use to sort the {@link Search} results.
     *           Valid arguments are: "NAME", "PRICE" (case-insensitive)
     * @param direction The direction to sort the {@link Search} results in.
     *                  Valid arguments are: "ASC", "DSC" (case-insensitive)
     * @throws IllegalArgumentException if either argument is not valid
     */
    public Sort(String by, String direction) throws IllegalArgumentException {
        switch (by.toUpperCase()) {
            case "NAME", "PRICE" -> this.by = by;
            default -> throw new IllegalArgumentException(String.format(Sort.invalidFormat, "by", by));
        }
        switch (direction.toUpperCase()) {
            case "ASC", "DSC" -> this.direction = direction.equalsIgnoreCase("ASC");
            default -> throw new IllegalArgumentException(String.format(Sort.invalidFormat, "direction", direction));
        }
    }
}