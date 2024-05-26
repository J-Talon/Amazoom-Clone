Feature: editing
  In order to modify the system
  As an administrator
  I need to be able to edit system components

  Background:
    Given the Product table
      | name           | price |
      | Test Product 1 | 10.00 |
      | Test Product 2 | 20.00 |
      # IDs are 1, 2
    And the User table
      | username          | password | name              | address            | admin |
      | test_user         | test1234 | Test User         | 123 Example Street | false |
      | test_admin        | test1234 | Test Admin        | 456 Example Street | true  |

  Scenario: admin can access editor
    Given I login with the username "test_admin" and the password "test1234"
    When I open the editor for Product #1
    Then I should see the "editor" page

  Scenario: non-admin cannot access editor for other users
    When I open the editor for User #test_user
    Then I should see the "error" page
    Given I login with the username "test_user" and the password "test1234"
    When I open the editor for User #test_admin
    Then I should see the "error" page
    When I open the editor for User #test_user
    Then I should see the "editor" page
    When I open the user editor
    Then I should see the "editor" page

  Scenario Outline: only admin can edit (except user's own profile)
    When I edit <toEdit> to have the properties
      | name    | <changedName>  |
      | <field> | <changedValue> |
    Then I should see the "error" page
    And <toEdit> should match
      | name    | <originalName> |
      | <field> | <originalValue>|
      # --------------------
    Given I login with the username "test_user" and the password "test1234"
    When I edit <toEdit> to have the properties
      | name    | <changedName>  |
      | <field> | <changedValue> |
    Then I should see the "error" page
    And <toEdit> should match
      | name    | <originalName> |
      | <field> | <originalValue>|
      # --------------------
    Given I logout
    And I login with the username "test_admin" and the password "test1234"
    When I edit <toEdit> to have the properties
      | name    | <changedName>  |
      | <field> | <changedValue> |
    Then I should see the "success" page
    And <toEdit> should match
      | name    | <changedName>  |
      | <field> | <changedValue> |

    Scenarios:
      | toEdit           | originalName   | changedName     | field    | originalValue | changedValue |
      | Product #1       | Test Product 1 | Changed Product | price    | 10.00         | 100.00       |
      | User #test_admin | Test Admin     | Changed Admin   | password | test1234      | changed1234  |


  Scenario: user can edit own data
    Given I login with the username "test_user" and the password "test1234"
    When I edit my user profile to have the properties
      | name    | Changed User       |
      | address | 789 Example Street |
    Then I should see the "success" page
    And I should be logged in as
        | username | test_user          |
        | password | test1234           |
        | name     | Changed User       |
        | address  | 789 Example Street |


  Scenario: admin can delete
    Given I login with the username "test_admin" and the password "test1234"
    When I delete Product #1
    Then I should see the "success" page
    And Product #1 should not exist
    And Product #2 should exist

  Scenario: non-admin cannot delete products or other users
    Given I login with the username "test_user" and the password "test1234"
    When I delete User #test_admin
    Then I should see the "error" page
    And User #test_user should exist
    And User #test_admin should exist

  Scenario: non-admin can delete own user
    Given I login with the username "test_user" and the password "test1234"
    When I delete my user profile
    Then I should see the "success" page
    And User #test_user should not exist
    And User #test_admin should exist