INSERT INTO categories (name) VALUES ('Electronics'), ('Books'), ('Clothing'), ('Home');

INSERT INTO products (name, description, price, category_id) VALUES
  ('Wireless Headphones', 'Over-ear noise-cancelling headphones', 79.99, 1),
  ('USB-C Hub', '7-in-1 USB-C hub with HDMI and PD charging', 34.99, 1),
  ('Mechanical Keyboard', 'Compact TKL with tactile switches', 129.99, 1),
  ('Clean Code', 'Robert C. Martin guide to writing maintainable code', 39.99, 2),
  ('The Pragmatic Programmer', 'Hunt & Thomas — timeless engineering practices', 44.99, 2),
  ('Atomic Habits', 'James Clear on building lasting habits', 18.99, 2),
  ('Classic White T-Shirt', '100% organic cotton, unisex fit', 19.99, 3),
  ('Slim-Fit Chinos', 'Lightweight stretch chinos in khaki', 49.99, 3),
  ('Pullover Hoodie', 'Fleece-lined, kangaroo pocket', 59.99, 3),
  ('Ceramic Coffee Mug', '400ml matte ceramic, microwave safe', 14.99, 4),
  ('Bamboo Cutting Board', 'Large bamboo board with juice groove', 24.99, 4),
  ('LED Desk Lamp', 'Touch-dimmer, USB charging port, 3 colour temps', 39.99, 4);
