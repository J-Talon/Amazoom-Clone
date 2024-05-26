Feature: product
  In order to buy things
  As a customer
  I need to be able to interact with individual products

  Scenario: create and access a product
    Given I create a Product with the properties
      | name  | Test Product |
      | price | 10.99       |
    Then Product #1 should match
      | name  | Test Product |
      | price | 10.99        |

  Scenario: view product
    Given I create a Product with the properties
      | name  | Test Product |
      | price | 10.99        |
    When I view Product #1
    Then the Product should match
      | name  | Test Product |
      | price | 10.99        |

  Scenario: products
    Given the Product table
      | name | price |
      | Foo  | 10    |
      | Bar  | 20    |
      | Baz  | 30    |
    When I view all Products
    Then the Product list should exactly match
      | name | price |
      | Foo  | 10    |
      | Bar  | 20    |
      | Baz  | 30    |

  Rule: admin actions
    Background:
      Given I create a User with the properties
        | username | test_admin         |
        | password | test1234           |
        | name     | Test Admin         |
        | address  | 123 Example Street |
        | admin    | true               |
      And I login with the username "test_admin" and the password "test1234"

    Scenario: edit a product
      Given I create a Product with the properties
        | name  | Test Product |
        | price | 10.99        |
      When I edit Product #1 to have the properties
        | name  | New Product Name |
      Then Product #1 should match
        | name  | New Product Name |
        | price | 10.99            |

    Scenario: delete a product
      Given I create a Product with the properties
        | name  | Test Product |
        | price | 10.99        |
      When I delete Product #1
      Then Product #1 should not exist
