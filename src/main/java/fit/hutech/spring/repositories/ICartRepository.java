package fit.hutech.spring.repositories;

import fit.hutech.spring.entities.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ICartRepository extends JpaRepository<Cart, Cart.CartId> {
    List<Cart> findAllByUser_Id(Long userId);

    Optional<Cart> findByUser_IdAndBook_Id(Long userId, Long bookId);

    void deleteByUser_IdAndBook_Id(Long userId, Long bookId);

    void deleteByUser_Id(Long userId);

    void deleteByBook_Id(Long bookId);
}
