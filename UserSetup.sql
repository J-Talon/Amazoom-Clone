INSERT INTO user ( username, password, name, address, admin, cart_id )
VALUES ( "john_smith", "test1234", "John Smith", "123 Main St.", 0, "john_smith" );
INSERT INTO cart ( username ) VALUES ( "john_smith" );

INSERT INTO user ( username, password, name, address, admin, cart_id )
VALUES ( "jane_doe", "test1234", "Jane Doe", "456 Main St.", 1, "jane_doe" );
INSERT INTO cart ( username ) VALUES ( "jane_doe" );