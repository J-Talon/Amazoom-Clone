package com.amazoom;

import com.j256.ormlite.dao.ForeignCollection;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.AfterAll;
import io.cucumber.java.Before;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.servlet.http.HttpSession;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.session.StandardSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.sqlite.JDBC;

import java.io.File;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class Stepdefs {
    protected static File savedDatabase;
    protected static File testDatabase;

    @BeforeAll
    public static void saveDatabase() {
        // Check if there is currently an `amazoom.sqlite` file in the current directory,
        // if so, move it so that we don't overwrite it
        Stepdefs.savedDatabase = new File("amazoom.sqlite");

        if (Stepdefs.savedDatabase.exists() && !Stepdefs.savedDatabase.isDirectory()) {
            // Create a new temporary `File` instance with the new path
            File tempDatabase = new File("amazoom.sqlite.saved");

            // Rename the file, throw error on failure
            if (!Stepdefs.savedDatabase.renameTo(tempDatabase))
                throw new RuntimeException("could not rename amazoom.sqlite");

            // Re-assign the `savedDatabase` static property to the `File` instance with updated path
            Stepdefs.savedDatabase = tempDatabase;
        }

        // Create a test database file that will get deleted when the tests are complete
        Stepdefs.testDatabase = new File("amazoom.sqlite");
    }


    private String result;

    private Model model;

    private HttpSession session;
    private HttpSession getSession() {
        // Makes sure the session is 'valid' whenever we need to use it
        ((StandardSession)session).setValid(true);
        return session;
    }

    @Before
    public void setup() {
        model = new ExtendedModelMap();
        StandardManager manager = new StandardManager();
        manager.setContext(new StandardContext());
        session = new StandardSession(manager);
    }

    @Before
    public void clearDatabaseTables() throws SQLException {
        // Open the connection the native Java way
        Connection connection = new JDBC().connect("jdbc:sqlite:amazoom.sqlite", new Properties());

        // Get the list of tables from the database
        var tables = connection.getMetaData().getTables(null, null, null, null);

        // Iterate over the list of tables, and delete all the rows in each table
        while (tables.next()) {
            String table = tables.getString("TABLE_NAME");
            if (!table.startsWith("sqlite_") || table.equals("sqlite_sequence"))
                connection.createStatement().execute("DELETE FROM " + table);
        }

        // Make sure the connection gets closed
        connection.close();
    }

    @Given("I login with the username {string} and the password {string}")
    public void iLoginWithTheUsernameAndThePassword(String username, String password) {
        result = User.authenticate(username, password, model, this.getSession());
    }

    @Given("I create a(n) {updatable} with the properties")
    public void iCreateAWithTheProperties(Class<? extends Updatable> updatable, Map<String, String> fields)
            throws Exception {
        // Convert the values in the map to an array of strings that we can later typecast if necessary
        String[] args = fields.values().toArray(String[]::new);

        // Iterate over all available constructors for the given type
        for ( var constructor : updatable.getDeclaredConstructors() ) {

            // Only continue if the number of arguments matches the number of fields
            if ( constructor.getParameterCount() == args.length ) {

                // Create an array of objects that have been typecast to the types of the constructor
                Object[] typedArgs = new Object[args.length];
                for (int i = 0; i < typedArgs.length; ++i) {
                    Class<?> type = constructor.getParameterTypes()[i];
                    typedArgs[i] = type.equals(String.class) ? args[i] :
                            type.getMethod("valueOf", String.class).invoke(null, args[i]);
                }

                // (Try to) create the object
                constructor.newInstance(typedArgs);

                // If we get here, we have successfully created the object
                return;
            }
        }

        throw new RuntimeException("no matching constructor found");
    }

    @Given("the {updatable} table")
    public void theUpdatableTable(Class<? extends Updatable> updatable, List<Map<String, String>> items)
            throws Exception {
        for ( Map<String, String> item : items )
            iCreateAWithTheProperties(updatable, item);
    }

    @When("I logout")
    public void iLogout() {
        result = User.logout(model, this.getSession());
    }

    @When("I search by {word} with the input {string}")
    public void iSearchByWithTheInput(String searchBy, String term) {
        result = Search.results(term, searchBy, "NAME", "ASC", model, this.getSession());
    }

    @When("I checkout")
    public void iCheckout() {
        result = Cart.checkout(null, this.model, this.getSession());
    }

    @When("I cancel order #{int}")
    public void iCancelOrder(int orderId) {
        result = Order.cancel(orderId, this.model, this.getSession());
    }

    @When("I remove {int} product #{int}(s) from my cart")
    public void iRemoveProductFromMyCart(int quantity, int id) throws SQLException {
        var product = Product.dao().queryForId(id);
        assert product != null;

        var cart = ((User) this.getSession().getAttribute("authenticated")).getCart();

        cart.remove(product, quantity);
    }

    @When("I remove {int} product #{int}(s) from #{word}'s cart")
    public void iRemoveProductFromCart(int quantity, int id, String username) {
        result = Cart.removeFromCart(id, quantity, username, this.model, this.getSession());
    }

    @When("I clear my cart")
    public void iClearMyCart() {
        var cart = ((User) this.getSession().getAttribute("authenticated")).getCart();
        cart.clear();
    }

    @When("I open the editor for {editable} #{id}")
    public <T> void iOpenTheEditorFor(Class<? extends Editable<T>> editable, T id) throws Exception {
        result = editable.getDeclaredConstructor().newInstance()
                .editor(id, this.model, this.getSession());
    }

    @When("I edit {editable} #{id} to have the properties")
    public <T> void iEditToHaveTheProperties(Class<? extends Editable<T>> editable, T id, Map<String, String> fields)
            throws Exception {
        result = editable.getDeclaredConstructor().newInstance()
                .edit(id, fields.keySet().stream().toList(), fields.values().stream().toList(), this.model, this.getSession());
    }

    @When("I add {int} product #{int}(s) to #{word}'s cart")
    public void iAddProductToCart(int quantity, int id, String username) {
        result = Cart.addToCart(id, quantity, username, this.model, this.getSession());
        System.out.println();
    }

    @When("I view {updatable} #{id}")
    public void iViewUpdatable(Class<? extends Updatable> updatable, Object id) throws Exception {
        // Extract the method to be called to 'view' the given class
        Method method;

        try {
            // Two possible types for first argument of the method, try String first, fallback to Integer if not
            method = updatable.getMethod(updatable.getSimpleName().toLowerCase(), String.class, Model.class, HttpSession.class);
        } catch (NoSuchMethodException ignored) {
            // If it STILL can't find the method it'll just throw the error from here
            method = updatable.getMethod(updatable.getSimpleName().toLowerCase(), Integer.class, Model.class, HttpSession.class);
        }

        // Invoke the method
        result = (String) method.invoke(null, id, this.model, this.getSession());
    }

    @When("I view all {updatable}s")
    public void iViewAllUpdatables(Class<? extends Updatable> updatable) throws Exception {
        result = (String) updatable.getMethod(updatable.getSimpleName().toLowerCase() + "s", Model.class, HttpSession.class)
                .invoke(null, this.model, this.getSession());
    }

    @When("I checkout #{word}")
    public void iCheckout(String username) {
        result = Cart.checkout(username, this.model, this.getSession());
    }

    @When("I view (user )#{word}'s orders")
    public void iViewOrders(String username) {
        result = Order.orders(username, this.model, this.getSession());
    }

    @When("I delete {editable} #{id}")
    public <T> void iDeleteUpdatable(Class<? extends Editable<T>> editable, T id) throws Exception {
        result = editable.getDeclaredConstructor().newInstance().delete(id, this.model, this.getSession());
    }

    @When("I sign up with the information")
    public void iSignUpWithTheInformation(Map<String, String> fields) {
        // Extract data from input datatable
        String username = fields.get("username");
        String password = fields.get("password");
        String name = fields.get("name");
        String address = fields.get("address");

        if (username == null)
            username = "";
        if (password == null)
            password = "";
        if (name == null)
            name = "";
        if (address == null)
            address = "";

        // Call the method
        result = User.doSignup(username, password, name, address, this.model, this.getSession());
    }

    @When("I open the user editor")
    public void iOpenTheUserEditor() {
        result = (new User()).editor(null, this.model, this.getSession());
    }

    @When("I edit my user profile to have the properties")
    public void iEditMyUserProfileToHaveTheProperties(Map<String, String> fields) throws Exception {
        iEditToHaveTheProperties(User.class, null, fields);
    }

    @When("I delete my user profile")
    public void iDeleteMyUserProfile() throws Exception {
        iDeleteUpdatable(User.class, null);
    }

    @Then("I should be logged in as")
    public void iShouldBeLoggedInAs(Map<String, String> expected) throws Exception {
        // Get the User object from the HTTP session, make sure it isn't null
        User actual = (User) this.getSession().getAttribute("authenticated");
        assert actual != null;

        // Check that expected and actual values match
        for ( Map.Entry<String, String> expectedEntry : expected.entrySet() )
            assert Helpers.checkField(actual, expectedEntry.getKey(), expectedEntry.getValue());
    }

    @Then("I should see the {string} page")
    public void iShouldSeeThePage(String page) {
        assert result.equals(page);
    }

    @Then("I should not be logged in")
    public void iShouldNotBeLoggedIn() {
        assert this.getSession().getAttribute("authenticated") == null;
    }

    @Then("the search results should match")
    public void theSearchResultsShouldMatch(List<Map<String, String>> expected) throws Exception {
        // Get the list of products from the model, make sure it isn't null
        List<?> actual = (List<?>) model.asMap().get("results");
        assert actual != null;

        // Check that the number of results matches the number of expected results
        assert actual.size() == expected.size();

        // Check that expected and actual values match
        for ( int i = 0; i < actual.size(); i++ ) {
            Product actualProduct = (Product) actual.get(i);
            for ( Map.Entry<String, String> expectedEntry : expected.get(i).entrySet() )
                assert Helpers.checkField(actualProduct, expectedEntry.getKey(), expectedEntry.getValue());
        }
    }

    @Then("my cart should match")
    public void myCartShouldMatch(List<Map<String, Integer>> expectedParts) {
        Cart cart = ((User) this.getSession().getAttribute("authenticated")).getCart();

        // Verify that the cart has the correct number of parts
        assert cart.parts.size() == expectedParts.size();

        // Verify that the cart has the correct parts
        Iterator<PartialCart> actualIterator = cart.parts.iterator();
        Iterator<Map<String, Integer>> expectedIterator = expectedParts.iterator();
        while (actualIterator.hasNext() && expectedIterator.hasNext()) {
            PartialCart actual = actualIterator.next();
            Map<String, Integer> expected = expectedIterator.next();

            // Verify that the product ID matches
            assert actual.getProduct().getId() == expected.get("id");

            // Verify that the quantity matches
            assert actual.getQuantity() == expected.get("quantity");
        }

        // Verify that both iterators are empty
        assert !actualIterator.hasNext() && !expectedIterator.hasNext();
    }

    @Then("my cart should be empty")
    public void myCartShouldBeEmpty() {
        assert ((User)this.getSession().getAttribute("authenticated")).getCart().getParts().size() == 0;
    }

    @Then("{updatable} #{id} should match")
    public void updatablesParamShouldBe(Class<? extends Updatable> updatable, Object id, DataTable expected)
            throws Exception {
        try {
            id = Integer.valueOf((String)id);
        } catch (NumberFormatException | ClassCastException ignored) {}

        // Get the object from the database by its id
        Updatable actual = Updatable.dao(updatable).queryForId(id);
        assert actual != null;

        // If `updatable` is a subclass of `ProductCollection`, then we treat the DataTable as a list of
        // product-quantity pairs
        if (ProductCollection.class.isAssignableFrom(updatable)) {
            var actualPartials = (ForeignCollection<?>) updatable.getMethod("getParts")
                    .invoke(actual);

            // Verify that there are as many collection entries as rows
            assert actualPartials.size() == expected.height() - 1;

            // Iterate over all the rows in the DataTable/items in the collection
            List<Map<String, Integer>> expectedList = expected.asMaps(String.class, Integer.class);
            for (int i = 0; i < expected.height() - 1; ++i) {
                // Get the product and quantity from the data table list
                Product expectedProduct = Product.dao().queryForId(expectedList.get(i).get("id"));
                int expectedQuantity = expectedList.get(i).get("quantity");

                // Get properly typecast value from collection
                PartialProductCollection actualPartial = (PartialProductCollection) actualPartials
                        .toArray(Object[]::new)[i];

                // Assert that expected values match actual values
                assert actualPartial.getProduct().getId() == expectedProduct.getId();
                assert actualPartial.getQuantity() == expectedQuantity;
            }
        } else { // Otherwise, treat datatable as a list of fields
            for (var entry : expected.asMap().entrySet())
                assert Helpers.checkField(actual, entry.getKey(), entry.getValue());
        }
    }

    @Then("{updatable} #{id} should exist")
    public void updatableShouldExist(Class<? extends Updatable> updatable, Object id) throws SQLException {
        assert Updatable.dao(updatable).queryForId(id) != null;
    }

    @Then("{updatable} #{id} should not exist")
    public void updatableShouldNotExist(Class<? extends Updatable> updatable, Object id) throws SQLException {
        assert Updatable.dao(updatable).queryForId(id) == null;
    }

    @Then("the {updatable} should match")
    public void theUpdatableShouldMatch(Class<? extends Updatable> updatable,
                                                DataTable expected) throws Exception {
        try {
            // Check if updatable is a subclass of ProductCollection (if it fails we execute the catch block)
            Class<? extends ProductCollection> productCollection = updatable.asSubclass(ProductCollection.class);

            // Get the product collection from the model, make sure it isn't null
            ProductCollection actual = (ProductCollection) model.getAttribute(productCollection.getSimpleName().toLowerCase());
            assert actual != null;

            // Check that the number of results matches the number of expected results
            assert actual.getParts().size() == expected.height() - 1;

            // Check that the fields of corresponding entries match
            Iterator actualIterator = actual.getParts().iterator();
            Iterator<Map<String, String>> expectedIterator = expected.asMaps(String.class, String.class).iterator();
            while (actualIterator.hasNext() && expectedIterator.hasNext()) {
                PartialProductCollection actualPartial = (PartialProductCollection) actualIterator.next();
                Map<String, String> expectedMap = expectedIterator.next();

                // Check that the product ID matches
                assert actualPartial.getProduct().getId() == Integer.parseInt(expectedMap.get("id"));

                // Check that the quantity matches
                assert actualPartial.getQuantity() == Integer.parseInt(expectedMap.get("quantity"));
            }

            // Check that both iterators are empty
            assert !actualIterator.hasNext() && !expectedIterator.hasNext();
        } catch (ClassCastException ignored) {
            // Get the updatable from the model, make sure it isn't null
            Updatable actual = (Updatable) model.getAttribute(updatable.getSimpleName().toLowerCase());
            assert actual != null;

            // Check that expected and actual values match
            for (Map.Entry<String, String> expectedEntry : expected.asMap().entrySet())
                assert Helpers.checkField(actual, expectedEntry.getKey(), expectedEntry.getValue());
        }
    }

    @Then("the orders should match")
    public void theOrdersShouldMatch(DataTable expectedTable) {
        // Get the list of orders from the model, make sure it isn't null
        List<?> actuals = (List<?>) this.model.getAttribute("orders");
        assert actuals != null;

        List<Integer> expectedList = expectedTable.rows(1).asList(Integer.class);


        // Check that the number of results matches the number of expected results
        assert actuals.size() == expectedList.size();

        // Check that expected and actual values match
        for (int i = 0; i < actuals.size(); ++i)
            assert ((Order) actuals.get(i)).getId() == expectedList.get(i);
    }

    @Then("{productCollection} #{id} should be empty")
    public void productCollectionShouldBeEmpty(Class<? extends ProductCollection> collection, Object id) throws SQLException {
        // Get the collection from the database
        ProductCollection actual = ProductCollection.dao(collection).queryForId(id);

        // Verify that the collection is empty
        assert actual != null;
        assert actual.getParts().isEmpty();
    }

    @Then("the {updatable} list should exactly match")
    public void theUpdatableListShouldExactlyMatch(Class<? extends Updatable> updatable, List<Map<String, String>> expected) throws Exception {
        // Get the list of products from the model, make sure it isn't null
        List<?> actual = (List<?>) model.getAttribute(updatable.getSimpleName().toLowerCase() + "s");
        assert actual != null;

        // Check that the number of results matches the number of expected results
        assert actual.size() == expected.size();

        // Check that expected and actual values match
        for ( int i = 0; i < actual.size(); i++ ) {
            Updatable actualUpdatable = (Updatable) actual.get(i);
            for ( Map.Entry<String, String> expectedEntry : expected.get(i).entrySet() )
                assert Helpers.checkField(actualUpdatable, expectedEntry.getKey(), expectedEntry.getValue());
        }
    }

    @Then("the {productCollection} total should be {float}")
    public void theProductCollectionTotalShouldBe(Class<? extends ProductCollection> collection, float expected) {
        // Get the collection from the model
        ProductCollection actual = (ProductCollection) model.getAttribute(collection.getSimpleName().toLowerCase());
        assert actual != null;

        // Check that the total matches
        assert actual.total() == expected;
    }

    @AfterAll
    public static void restoreDatabase() {
        if (!Stepdefs.testDatabase.delete())
            throw new RuntimeException("failed to delete test database");


        if (!Stepdefs.savedDatabase.getName().equals("amazoom.sqlite")) {
            if (!Stepdefs.savedDatabase.renameTo(new File("amazoom.sqlite")))
                throw new RuntimeException("failed to restore database file");
        }
    }
}