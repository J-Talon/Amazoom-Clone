Feature: order
  In order to allow users to see what they have purchased
  As a storefront
  I need to have a mechanism to display a user's orders

  Background:
    Given the Product table
      | name            | price |
      | Test Product 1  | 10.00 |
      | Test Product 2  | 20.00 |
      # IDs are 1, 2
    And the User table
      | username          | password | name              | address            | admin |
      | test_user         | test1234 | Test User         | 123 Example Street | false |
      | test_admin        | test1234 | Test Admin        | 456 Example Street | true  |
      | test_unauthorized | test1234 | Test Unauthorized | 789 Example Street | false |
    When I login with the username "test_user" and the password "test1234"
    And I add 1 product #1 to #test_user's cart
    And I checkout #test_user
    And I add 2 product #2s to #test_user's cart
    And I checkout #test_user
    And I logout

  Scenario Outline: view order
    Given I login with the username "<username>" and the password "test1234"
    When I view Order #<n>
    Then I should see the "order" page
    And the Order total should be <total>
    And the Order should match
      | id  | quantity |
      | <n> | <n>      |

    Scenarios:
      | username   | n | total |
      | test_user  | 1 | 10.00 |
      | test_user  | 2 | 40.00 |
      | test_admin | 1 | 10.00 |
      | test_admin | 2 | 40.00 |

  Scenario Outline: cannot view non-existent order
    When I logout
    And I login with the username "<username>" and the password "test1234"
    When I view Order #3
    Then I should see the "error" page

    Scenarios:
        | username          |
        | test_user         |
        | test_admin        |
        | test_unauthorized |

  Scenario Outline: cancel order
    Given I login with the username "<username>" and the password "test1234"
    When I cancel order #<n>
    Then I should see the "success" page

    Scenarios:
      | username   | n |
      | test_user  | 1 |
      | test_user  | 2 |
      | test_admin | 1 |
      | test_admin | 2 |

  Scenario Outline: view orders
    Given I login with the username "<username>" and the password "test1234"
    When I view #test_user's orders
    Then I should see the "orders" page
    And the orders should match
      | id |
      | 1  |
      | 2  |

    Scenarios:
        | username          |
        | test_user         |
        | test_admin        |

  Scenario Outline: cannot access other's order(s)
    When I logout
    And I <action>
    Then I should see the "error" page
    And I login with the username "test_unauthorized" and the password "test1234"
    And I <action>
    Then I should see the "error" page

    Scenarios:
      | action                         |
      | view Order #1                  |
      | view Order #2                  |
      | cancel order #1                |
      | cancel order #2                |
      | view user #test_user's orders  |
      | view user #test_admin's orders |