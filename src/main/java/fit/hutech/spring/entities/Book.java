package fit.hutech.spring.entities;

import fit.hutech.spring.validators.annotations.ValidCategoryId;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.Hibernate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "book")
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", length = 50, nullable = false)
    @Size(min = 1, max = 50, message = "Title must be between 1 and 50 characters")
    @NotBlank(message = "Title must not be blank")
    private String title;

    @Column(name = "author", length = 50, nullable = false)
    @Size(min = 1, max = 50, message = "Author must be between 1 and 50 characters")
    @NotBlank(message = "Author must not be blank")
    private String author;

    @Column(name = "price")
    @DecimalMin(value = "0", inclusive = true, message = "Price must be at least 0")
    @DecimalMax(value = "1000000000", inclusive = true, message = "Price must be at most 1000000000")
    private Double price;

    @Column(name = "stock")
    @Min(value = 0, message = "Stock must be at least 0")
    @Max(value = 1000000, message = "Stock must be at most 1000000")
    private Integer stock;

    @Column(name = "active")
    @NotNull(message = "Active status is required")
    @Builder.Default
    private Boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", referencedColumnName = "id")
    @ValidCategoryId
    @ToString.Exclude
    private Category category;

    @Builder.Default
    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<ItemInvoice> itemInvoices = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @ToString.Exclude
    private User user;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o))
            return false;
        Book book = (Book) o;
        return getId() != null && Objects.equals(getId(), book.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public boolean isActiveForSale() {
        return active == null || Boolean.TRUE.equals(active);
    }

    public int getStockSafe() {
        return stock == null ? Integer.MAX_VALUE : stock;
    }
}