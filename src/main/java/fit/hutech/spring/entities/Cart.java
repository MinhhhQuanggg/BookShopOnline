package fit.hutech.spring.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cart")
@IdClass(Cart.CartId.class)
public class Cart {
    @Id
    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    @ToString.Exclude
    private User user;

    @Id
    @ManyToOne
    @JoinColumn(name = "book_id", referencedColumnName = "id", nullable = false)
    @ToString.Exclude
    private Book book;

    @Column(name = "quantity", nullable = false)
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 10, message = "Quantity must be at most 10")
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 0)
    @DecimalMin(value = "0", inclusive = true, message = "Unit price must be at least 0")
    @DecimalMax(value = "1000000000", inclusive = true, message = "Unit price must be at most 1000000000")
    private BigDecimal unitPrice;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @jakarta.persistence.PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (quantity == null) quantity = 1;
        if (unitPrice == null && book != null && book.getPrice() != null) {
            unitPrice = BigDecimal.valueOf(book.getPrice());
        }
    }

    @jakarta.persistence.PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public static class CartId implements Serializable {
        private Long user;
        private Long book;

        public CartId() {}

        public CartId(Long user, Long book) {
            this.user = user;
            this.book = book;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CartId cartId = (CartId) o;
            return Objects.equals(user, cartId.user) && Objects.equals(book, cartId.book);
        }

        @Override
        public int hashCode() {
            return Objects.hash(user, book);
        }
    }
}
