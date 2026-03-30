package fit.hutech.spring.services;

import fit.hutech.spring.entities.Book;
import fit.hutech.spring.entities.Invoice;
import fit.hutech.spring.entities.StockMovement;
import fit.hutech.spring.entities.StockMovementType;
import fit.hutech.spring.entities.User;
import fit.hutech.spring.repositories.IStockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private final IStockMovementRepository stockMovementRepository;

    @Transactional
    public StockMovement recordMovement(Book book,
                                        User user,
                                        Invoice invoice,
                                        StockMovementType movementType,
                                        int previousStock,
                                        int newStock,
                                        String note) {
        int quantityChange = newStock - previousStock;
        StockMovement movement = StockMovement.builder()
                .book(book)
                .user(user)
                .invoice(invoice)
                .movementType(movementType)
                .previousStock(previousStock)
                .newStock(newStock)
                .quantityChange(quantityChange)
                .note(note)
                .build();
        return stockMovementRepository.save(movement);
    }

    public StockMovement recordOpeningStock(Book book, User user, int openingStock, String note) {
        return recordMovement(book, user, null, StockMovementType.IMPORT, 0, openingStock, note);
    }

    public List<StockMovement> getRecentMovements() {
        return stockMovementRepository.findTop200ByOrderByCreatedAtDesc();
    }
}
