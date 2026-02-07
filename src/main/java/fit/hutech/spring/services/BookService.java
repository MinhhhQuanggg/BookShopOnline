package fit.hutech.spring.services;

import fit.hutech.spring.entities.Book;
import fit.hutech.spring.repositories.IBookRepository;
import fit.hutech.spring.repositories.ICartRepository;
import fit.hutech.spring.repositories.IItemInvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookService {

    private final IBookRepository bookRepository;
    private final IItemInvoiceRepository itemInvoiceRepository;
    private final ICartRepository cartRepository;

    // ===== GET ALL (PAGING + SORT + CATEGORY) =====
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

    // ===== GET BY ID =====
    public Optional<Book> getBookById(Long id) {
        return bookRepository.findById(id);
    }

    // ===== ADD =====
    public Book addBook(Book book, fit.hutech.spring.entities.User user) {
        if (book.getActive() == null) {
            book.setActive(true);
        }
        if (book.getStock() == null) {
            book.setStock(100);
        }
        book.setUser(user);
        return bookRepository.save(book);
    }

    @Deprecated
    public Book addBook(Book book) {
        return addBook(book, null);
    }

    // ===== UPDATE =====
    public void updateBook(@NotNull Book book) {
        bookRepository.findById(book.getId()).ifPresent(existingBook -> {
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
            bookRepository.save(existingBook);
        });
    }

    // ===== DELETE =====
    @Transactional
    public boolean deleteBookById(Long id) {
        // Kiểm tra xem sách có trong hóa đơn nào không
        boolean hasInvoices = itemInvoiceRepository.existsByBookId(id);

        if (hasInvoices) {
            return false; // Không thể xóa, sách đã được mua
        }

        // Xóa sách khỏi giỏ hàng của tất cả user trước
        cartRepository.deleteByBook_Id(id);

        bookRepository.deleteById(id);
        return true; // Xóa thành công
    }

    // ===== SEARCH =====
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
