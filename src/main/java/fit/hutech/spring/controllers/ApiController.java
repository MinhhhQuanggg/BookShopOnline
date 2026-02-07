package fit.hutech.spring.controllers;

import fit.hutech.spring.entities.Book;
import fit.hutech.spring.services.BookService;
import fit.hutech.spring.services.CategoryService;
import fit.hutech.spring.viewmodels.BookGetVm;
import fit.hutech.spring.viewmodels.BookPostVm;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/books")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ApiController {
    private final BookService bookService;
    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<BookGetVm>> getAllBooks(
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy) {

        return ResponseEntity.ok(bookService.getAllBooks(
                pageNo == null ? 0 : pageNo,
                pageSize == null ? 20 : pageSize,
                sortBy == null ? "id" : sortBy)
                .stream()
                .map(BookGetVm::from)
                .toList());
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<BookGetVm> getBookById(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getBookById(id)
                .map(BookGetVm::from)
                .orElse(null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBookById(@PathVariable Long id) {
        bookService.deleteBookById(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<BookGetVm>> searchBooks(@RequestParam String keyword) {
        return ResponseEntity.ok(bookService.searchBook(keyword)
                .stream()
                .map(BookGetVm::from)
                .toList());
    }

    @PostMapping
    public ResponseEntity<BookGetVm> addBook(@RequestBody BookPostVm bookPostVm) {
        Book book = new Book();
        book.setTitle(bookPostVm.title());
        book.setAuthor(bookPostVm.author());
        book.setPrice(bookPostVm.price());
        book.setCategory(categoryService.getCategoryById(bookPostVm.categoryId()).orElse(null));

        Book savedBook = bookService.addBook(book);
        return ResponseEntity.ok(BookGetVm.from(savedBook));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookGetVm> updateBook(@PathVariable Long id, @RequestBody BookPostVm bookPostVm) {
        Book existingBook = bookService.getBookById(id)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        existingBook.setTitle(bookPostVm.title());
        existingBook.setAuthor(bookPostVm.author());
        existingBook.setPrice(bookPostVm.price());
        existingBook.setCategory(categoryService.getCategoryById(bookPostVm.categoryId()).orElse(null));

        bookService.updateBook(existingBook);
        return ResponseEntity.ok(BookGetVm.from(existingBook));
    }
}
