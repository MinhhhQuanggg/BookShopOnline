-- Cart table (persist user cart items)
-- Database: bookstore
-- MySQL 8+ (CHECK constraints are enforced in 8.0.16+)

CREATE TABLE IF NOT EXISTS `cart` (
    `user_id` BIGINT NOT NULL,
    `book_id` BIGINT NOT NULL,
    `quantity` INT NOT NULL DEFAULT 1,
    `unit_price` DECIMAL(12,0) NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (`user_id`, `book_id`),

    CONSTRAINT `fk_cart_user`
        FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT `fk_cart_book`
        FOREIGN KEY (`book_id`) REFERENCES `book` (`id`)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT `chk_cart_quantity`
        CHECK (`quantity` >= 1 AND `quantity` <= 10),

    CONSTRAINT `chk_cart_unit_price`
        CHECK (`unit_price` >= 0 AND `unit_price` <= 1000000000)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX `idx_cart_book_id` ON `cart` (`book_id`);
