package fit.hutech.spring.repositories;

import fit.hutech.spring.entities.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IStockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findTop200ByOrderByCreatedAtDesc();
}
