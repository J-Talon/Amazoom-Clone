Feature: cart
  In order to allow users to buy multiple products together
  As a storefront
  I need to provide a cart for users to add products to

  Background:
    Given the User table
      | username          | password | name              | address            | admin |
      | test_user         | test1234 | Test User         | 123 Example Street | false |
      | test_admin        | test1234 | Test Admin        | 456 Example Street | true  |
      | test_unauthorized | test1234 | Test Unauthorized | 789 Example Street | false |
    And the Product table
      | name            | price |
      | Test Product 1  | 10.00 |
      | Test Product 2  | 20.00 |
      # IDs are 1, 2
    When I login with the username "test_user" and the password "test1234"
    And I add 1 product #1 to #test_user's cart
    And I add 2 product #2s to #test_user's cart
    And I logout

  Scenario: add to cart
    Then Cart #test_user should match
      | id | quantity |
      | 1  | 1        |
      | 2  | 2        |

  Scenario: remove item from cart
    Given I login with the username "test_user" and the password "test1234"
    When I remove 1 product #2 from my cart
    Then Cart #test_user should match
      | id | quantity |
      | 1  | 1        |
      | 2  | 1        |

  Scenario: view cart
    Given I login with the username "test_user" and the password "test1234"
    When I view Cart #test_user
    Then I should see the "cart" page
    And the Cart should match
      | id | quantity |
      | 1  | 1        |
      | 2  | 2        |
    And the Cart total should be 50.00

  Scenario Outline: cannot access other user's cart
    When I <action>
    Then I should see the "error" page

    Scenarios:
      | action                                     |
      | view Cart #test_user                       |
      | add 1 product #1 to #test_user's cart      |
      | remove 1 product #1 from #test_user's cart |

  Scenario Outline: admin can access any user's cart
    Given I login with the username "test_admin" and the password "test1234"
    When I <action> #test_user's cart
    Then I should see the "redirect:/cart?username=test_user" page

    Scenarios:
      | action                   |
      | add 1 product #1 to      |
      | remove 1 product #1 from |

  Scenario: admin can view any user's cart
    Given I login with the username "test_admin" and the password "test1234"
    When I view Cart #test_user
    Then I should see the "cart" page
    And the Cart should match
      | id | quantity |
      | 1  | 1        |
      | 2  | 2        |

  Scenario: checkout
    Given I login with the username "test_user" and the password "test1234"
    When I checkout
    Then I should see the "success" page
    And my cart should be empty

  Scenario: checkout with no items
    Given I login with the username "test_user" and the password "test1234"
    When I remove 1 product #1 from my cart
    And I remove 2 product #2s from my cart
    When I checkout
    Then I should see the "error" page

  Scenario: checkout with no user
    Given I checkout
    Then I should see the "error" page

  Scenario: cannot checkout another user
    Given I checkout #test_unauthorized
    Then I should see the "error" page
    Given I login with the username "test_user" and the password "test1234"
    When I checkout #test_unauthorized
    Then I should see the "error" page

  Scenario: admin can checkout another user
    Given I login with the username "test_admin" and the password "test1234"
    And I add 1 product #1 to #test_unauthorized's cart
    Then I should see the "redirect:/cart?username=test_unauthorized" page
    When I checkout #test_unauthorized
    Then I should see the "success" page
    And Cart #test_unauthorized should be empty