-- Seed data for `cart` table (safe inserts)
-- This script tries to insert cart rows by looking up existing users/books.
-- If the referenced user/book does not exist, the INSERT will add 0 rows.

-- Example 1: add the first 2 books to user 'h1@hutech.edu.vn'
INSERT INTO `cart` (`user_id`, `book_id`, `quantity`, `unit_price`)
SELECT u.id, b.id, 1, b.price
FROM `user` u
JOIN `book` b ON b.id IN (1, 2)
WHERE u.email = 'h1@hutech.edu.vn'
ON DUPLICATE KEY UPDATE
  `quantity` = VALUES(`quantity`),
  `unit_price` = VALUES(`unit_price`);

-- Example 2: add any 1 book (smallest id) to user 'balapxa123@gmail.com'
INSERT INTO `cart` (`user_id`, `book_id`, `quantity`, `unit_price`)
SELECT u.id, b.id, 2, b.price
FROM `user` u
JOIN `book` b
WHERE u.email = 'balapxa123@gmail.com'
ORDER BY b.id
LIMIT 1
ON DUPLICATE KEY UPDATE
  `quantity` = VALUES(`quantity`),
  `unit_price` = VALUES(`unit_price`);

