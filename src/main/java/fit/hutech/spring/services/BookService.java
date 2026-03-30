package fit.hutech.spring.services;

import fit.hutech.spring.entities.Book;
import fit.hutech.spring.repositories.IBookRepository;
import fit.hutech.spring.repositories.ICartRepository;
import fit.hutech.spring.repositories.IItemInvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookService {

    private final IBookRepository bookRepository;
    private final IItemInvoiceRepository itemInvoiceRepository;
    private final ICartRepository cartRepository;
    private final InventoryService inventoryService;

    public Page<Book> getBooksPage(Integer pageNo,
            Integer pageSize,
            String sortBy,
            String keyword,
            Long categoryId) {
        var pageable = PageRequest.of(pageNo, pageSize, Sort.by(sortBy));
        String trimmedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return bookRepository.searchBookWithCategory(trimmedKeyword, categoryId, pageable);
    }

    public List<Book> getAllBooks(Integer pageNo,
            Integer pageSize,
            String sortBy) {
        return getBooksPage(pageNo, pageSize, sortBy, null, null).getContent();
    }

    public Optional<Book> getBookById(Long id) {
        return bookRepository.findById(id);
    }

    public Book addBook(Book book, fit.hutech.spring.entities.User user) {
        if (book.getActive() == null) {
            book.setActive(true);
        }
        if (book.getStock() == null) {
            book.setStock(100);
        }
        book.setUser(user);
        Book saved = bookRepository.save(book);
        inventoryService.recordOpeningStock(saved, user, saved.getStockSafe(), "Kho ban Ä‘áº§u");
        return saved;
    }

    @Deprecated
    public Book addBook(Book book) {
        return addBook(book, null);
    }

    public Book updateBook(@NotNull Book book, fit.hutech.spring.entities.User user) {
        Book existingBook = bookRepository.findById(book.getId())
                .orElseThrow(() -> new IllegalArgumentException("Book not found"));
        int previousStock = existingBook.getStockSafe();

        existingBook.setTitle(book.getTitle());
        existingBook.setAuthor(book.getAuthor());
        existingBook.setPrice(book.getPrice());
        existingBook.setCategory(book.getCategory());
        if (book.getStock() != null) {
            existingBook.setStock(book.getStock());
        }
        if (book.getActive() != null) {
            existingBook.setActive(book.getActive());
        }
        Book saved = bookRepository.save(existingBook);
        int newStock = saved.getStockSafe();
        if (previousStock != newStock) {
            inventoryService.recordMovement(saved, user, null,
                    fit.hutech.spring.entities.StockMovementType.ADJUSTMENT,
                    previousStock,
                    newStock,
                    "Cập nhật tồn kho sách");
        }
        return saved;
    }

    public Book updateBook(@NotNull Book book) {
        return updateBook(book, null);
    }

    @Transactional
    public boolean deleteBookById(Long id) {
        boolean hasInvoices = itemInvoiceRepository.existsByBookId(id);

        if (hasInvoices) {
            return false;
        }

        cartRepository.deleteByBook_Id(id);
        bookRepository.deleteById(id);
        return true;
    }

    public List<Book> searchBook(String keyword) {
        return bookRepository.searchBook(keyword);
    }

    public List<String> suggestTitles(String q) {
        if (q == null)
            return List.of();
        String keyword = q.trim();
        if (keyword.isEmpty())
            return List.of();
        return bookRepository.suggestTitles(keyword, PageRequest.of(0, 8));
    }
}