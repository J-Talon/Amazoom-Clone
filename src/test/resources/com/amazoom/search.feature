Feature: search
  In order to allow users to browse the catalog
  As a storefront
  I need to provide a search mechanism

  Background:
    Given the Product table
      | name           | price  |
      | Nike shoes     | 10.99 |
      | Adidas shoes   | 11.99 |
      | random product | 12.99 |
      # IDs are 1, 2, 3

  Scenario: Search for a products that have "shoes" in its name
    When I search by name with the input "shoes"
    Then the search results should match
      | name         | price  |
      | Adidas shoes | 11.99  |
      | Nike shoes   | 10.99  |

  Scenario: only admin can search by ID
    When I search by ID with the input "1"
    Then I should see the "error" page

  Scenario Outline: Search for a products that have the correct ID
    Given I create a User with the properties
      | username | test_admin         |
      | password | test1234           |
      | name     | Test Admin         |
      | address  | 123 Example Street |
      | admin    | true               |
    When I login with the username "test_admin" and the password "test1234"
    And I search by ID with the input "<id>"
    Then the search results should match
      | name           | price   |
      | <name>         | <price> |

    Scenarios:
      | id | name           | price  |
      | 1  | Nike shoes     | 10.99  |
      | 2  | Adidas shoes   | 11.99  |
      | 3  | random product | 12.99  |
