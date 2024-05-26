Feature: user
  In order to recognize users
  As an online storefront
  I need to understand users

  Background:
    Given the User table
      | username   | password | name       | address            | admin |
      | test_user  | test1234 | Test User  | 123 Example Street | false |
      | test_admin | test1234 | Test Admin | 456 Example Street | true  |

  Scenario: create and log in as a user
    Then User #test_user should match
      | password | test1234           |
      | name     | Test User          |
      | address  | 123 Example Street |

  Scenario: user can view their own profile
    When I login with the username "test_user" and the password "test1234"
    And I view User #test_user
    Then I should see the "user" page
    And the User should match
      | password | test1234           |
      | name     | Test User          |
      | address  | 123 Example Street |

  Scenario Outline: unauthenticated cannot view individual users
    When I view User #<user>
    Then I should see the "error" page

    Scenarios:
      | user       |
      | test_user  |
      | test_admin |

  Scenario Outline: only admin can view individual other users
    When I login with the username "<user>" and the password "test1234"
    And I view User #test_user
    Then I should see the "user" page
    When I view User #test_admin
    Then I should see the "<result>" page

    Scenarios:
      | user       | result |
      | test_user  | error  |
      | test_admin | user   |

  Scenario: view users
    Given I login with the username "test_admin" and the password "test1234"
    When I view all Users
    Then the User list should exactly match
      | username   | password | name       | address            |
      | test_user  | test1234 | Test User  | 123 Example Street |
      | test_admin | test1234 | Test Admin | 456 Example Street |

  Scenario: only admin can view all users
    When I view all Users
    Then I should see the "error" page
    When I login with the username "test_user" and the password "test1234"
    And I view all Users
    Then I should see the "error" page
