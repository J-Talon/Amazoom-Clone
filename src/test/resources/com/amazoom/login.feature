Feature: login
  In order to identify users
  As a storefront
  I need to provide a mechanism for users to login

  Background:
    Given the User table
      | username  | password | name       | address            |
      | test_user | test1234 | Test User  | 123 Example Street |

  Scenario: logged in
    When I login with the username "test_user" and the password "test1234"
    Then I should be logged in as
      | username | test_user          |
      | password | test1234           |
      | name     | Test User          |
      | address  | 123 Example Street |

  Scenario: bad password
    When I login with the username "test_user" and the password "bad_password"
    Then I should see the "error" page
    And I should not be logged in

  Scenario: logout
    When I login with the username "test_user" and the password "test1234"
    When I logout
    Then I should not be logged in

  Scenario: login twice causes error
    When I login with the username "test_user" and the password "test1234"
    And I login with the username "test_user" and the password "test1234"
    Then I should see the "error" page

  Scenario: sign up
    When I sign up with the information
      | username | test_user2         |
      | password | test1234           |
      | name     | Test User          |
      | address  | 123 Example Street |
    And I login with the username "test_user2" and the password "test1234"
    Then I should be logged in as
      | username | test_user2         |
      | password | test1234           |
      | name     | Test User          |
      | address  | 123 Example Street |

  Scenario Outline: missing information
    When I sign up with the information
      | username | <username>         |
      | password | <password>         |
      | name     | <name>             |
      | address  | <address>          |
    Then I should see the "error" page

    Scenarios:
        | username   | password | name      | address            |
        |            |          |           |                    |
        |            | test1234 | Test User | 123 Example Street |
        | test_user2 |          | Test User | 123 Example Street |
        | test_user2 | test1234 |           | 123 Example Street |
        | test_user2 | test1234 | Test User |                    |

  Scenario: sign up with existing username
    When I sign up with the information
      | username | test_user          |
      | password | test1234           |
      | name     | Test User          |
      | address  | 123 Example Street |
    Then I should see the "error" page