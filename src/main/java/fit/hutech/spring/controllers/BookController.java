package fit.hutech.spring.controllers;

import fit.hutech.spring.entities.Book;
import fit.hutech.spring.services.BookService;
import fit.hutech.spring.services.ExcelService;
import fit.hutech.spring.services.CartService;
import fit.hutech.spring.services.CategoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {
        private final CartService cartService;
        private final CategoryService categoryService;
        private final BookService bookService;
        private final ExcelService excelService;

        private final fit.hutech.spring.services.UserService userService;

        private fit.hutech.spring.entities.User getCurrentUser(Authentication authentication) {
                if (authentication == null || !authentication.isAuthenticated())
                        return null;
                Object principal = authentication.getPrincipal();
                if (principal instanceof fit.hutech.spring.entities.User)
                        return (fit.hutech.spring.entities.User) principal;
                if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                        return userService.findByUsername(
                                        ((org.springframework.security.core.userdetails.UserDetails) principal)
                                                        .getUsername())
                                        .orElse(null);
                }
                if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User oauth2User) {
                        String email = oauth2User.getAttribute("email");
                        return userService.findByEmail(email).orElse(null);
                }
                return null;
        }

        private boolean canEditOrDelete(fit.hutech.spring.entities.Book book,
                        fit.hutech.spring.entities.User currentUser) {
                if (currentUser == null)
                        return false;
                // Admin has full access
                if (currentUser.getRoles().stream().anyMatch(r -> r.getName().equals("ADMIN")))
                        return true;
                // User can only edit their own books
                return book.getUser() != null && book.getUser().equals(currentUser);
        }

        @GetMapping
        public String showAllBooks(@NotNull Model model,
                        @RequestParam(defaultValue = "0") Integer pageNo,
                        @RequestParam(defaultValue = "20") Integer pageSize,
                        @RequestParam(defaultValue = "id") String sortBy,
                        @RequestParam(required = false) String keyword,
                        @RequestParam(required = false) Long categoryId,
                        Authentication authentication) {
                var page = bookService.getBooksPage(pageNo, pageSize, sortBy, keyword, categoryId);
                model.addAttribute("books", page.getContent());
                model.addAttribute("currentPage", pageNo);
                model.addAttribute("totalPages", page.getTotalPages());
                model.addAttribute("pageSize", pageSize);
                model.addAttribute("sortBy", sortBy);
                model.addAttribute("keyword", keyword);
                model.addAttribute("selectedCategoryId", categoryId);
                model.addAttribute("categories", categoryService.getAllCategories());

                // Pass current user ID for View logic
                var currentUser = getCurrentUser(authentication);
                if (currentUser != null) {
                        model.addAttribute("currentUserId", currentUser.getId());
                        model.addAttribute("isAdmin",
                                        currentUser.getRoles().stream().anyMatch(r -> r.getName().equals("ADMIN")));
                }

                return "book/list";
        }

        @GetMapping("/api-list")
        public String showApiBooks() {
                return "book/api-list";
        }

        @GetMapping("/live")
        public String liveBooks(@NotNull Model model,
                        @RequestParam(defaultValue = "0") Integer pageNo,
                        @RequestParam(defaultValue = "20") Integer pageSize,
                        @RequestParam(defaultValue = "id") String sortBy,
                        @RequestParam(required = false) String keyword,
                        @RequestParam(required = false) Long categoryId,
                        Authentication authentication) {
                var page = bookService.getBooksPage(pageNo, pageSize, sortBy, keyword, categoryId);
                model.addAttribute("books", page.getContent());
                model.addAttribute("currentPage", pageNo);
                model.addAttribute("totalPages", page.getTotalPages());
                model.addAttribute("pageSize", pageSize);
                model.addAttribute("sortBy", sortBy);
                model.addAttribute("keyword", keyword);
                model.addAttribute("selectedCategoryId", categoryId);

                var currentUser = getCurrentUser(authentication);
                if (currentUser != null) {
                        model.addAttribute("currentUserId", currentUser.getId());
                        model.addAttribute("isAdmin",
                                        currentUser.getRoles().stream().anyMatch(r -> r.getName().equals("ADMIN")));
                }
                return "book/list :: results";
        }

        @GetMapping("/add")
        public String addBookForm(@NotNull Model model) {
                model.addAttribute("book", new Book());
                model.addAttribute("categories",
                                categoryService.getAllCategories());
                return "book/add";
        }

        @PostMapping("/add")
        public String addBook(
                        @Valid @ModelAttribute("book") Book book,
                        @NotNull BindingResult bindingResult,
                        Model model,
                        Authentication authentication) {
                if (bindingResult.hasErrors()) {
                        var errors = bindingResult.getAllErrors()
                                        .stream()
                                        .map(DefaultMessageSourceResolvable::getDefaultMessage)
                                        .toArray(String[]::new);
                        model.addAttribute("errors", errors);
                        model.addAttribute("categories",
                                        categoryService.getAllCategories());
                        return "book/add";
                }
                var currentUser = getCurrentUser(authentication);
                bookService.addBook(book, currentUser);
                return "redirect:/books";
        }

        @GetMapping("/edit/{id}")
        public String editBookForm(@NotNull Model model, @PathVariable long id, Authentication authentication) {
                var bookOpt = bookService.getBookById(id);
                var book = bookOpt.orElseThrow(() -> new IllegalArgumentException("Book not found"));

                var currentUser = getCurrentUser(authentication);
                if (!canEditOrDelete(book, currentUser)) {
                        return "redirect:/books?error=Bạn không có quyền chỉnh sửa sách này";
                }

                model.addAttribute("book", book);
                model.addAttribute("categories",
                                categoryService.getAllCategories());
                return "book/edit";
        }

        @PostMapping("/edit")
        public String editBook(@Valid @ModelAttribute("book") Book book,
                        @NotNull BindingResult bindingResult,
                        Model model,
                        Authentication authentication) {
                if (bindingResult.hasErrors()) {
                        var errors = bindingResult.getAllErrors()
                                        .stream()
                                        .map(DefaultMessageSourceResolvable::getDefaultMessage)
                                        .toArray(String[]::new);
                        model.addAttribute("errors", errors);
                        model.addAttribute("categories",
                                        categoryService.getAllCategories());
                        return "book/edit";
                }

                // Double check permission before update
                var existingBookOpt = bookService.getBookById(book.getId());
                if (existingBookOpt.isPresent()) {
                        var currentUser = getCurrentUser(authentication);
                        if (!canEditOrDelete(existingBookOpt.get(), currentUser)) {
                                return "redirect:/books?error=Bạn không có quyền chỉnh sửa sách này";
                        }
                        // Keep the original owner
                        book.setUser(existingBookOpt.get().getUser());
                }

                bookService.updateBook(book);
                return "redirect:/books";
        }

        @GetMapping("/delete/{id}")
        public String deleteBook(@PathVariable long id, Authentication authentication) {
                var bookOpt = bookService.getBookById(id);
                if (bookOpt.isEmpty()) {
                        throw new IllegalArgumentException("Book not found");
                }

                var currentUser = getCurrentUser(authentication);
                if (!canEditOrDelete(bookOpt.get(), currentUser)) {
                        return "redirect:/books?error=Bạn không có quyền xóa sách này";
                }

                boolean deleted = bookService.deleteBookById(id);
                if (!deleted) {
                        // Sách không thể xóa vì đã có hóa đơn
                        return "redirect:/books?error=Không thể xóa sách đã được mua";
                }

                return "redirect:/books";
        }

        @PostMapping("/add-to-cart")
        public String addToCart(HttpServletRequest request,
                        HttpSession session,
                        Authentication authentication,
                        @RequestParam long id,
                        @RequestParam(defaultValue = "1") int quantity,
                        RedirectAttributes redirectAttributes) {
                if (authentication == null || !authentication.isAuthenticated()
                                || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
                        return "redirect:/login";
                }
                var bookOpt = bookService.getBookById(id);
                var book = bookOpt.orElseThrow(() -> new IllegalArgumentException("Book not found"));
                try {
                        cartService.addToCart(session, authentication, book.getId(), quantity);
                        redirectAttributes.addFlashAttribute(
                                        "successMessage",
                                        "Đã thêm \"" + book.getTitle() + "\" vào giỏ hàng.");
                } catch (IllegalArgumentException e) {
                        String msg = e.getMessage();
                        if ("Book is not active for sale".equals(msg)) {
                                msg = "Sách này hiện không bán (Hàng trưng bày/Ngừng kinh doanh)";
                        } else if ("Quantity exceeds stock".equals(msg)) {
                                msg = "Số lượng trong kho không đủ";
                        }
                        redirectAttributes.addAttribute("error", msg);
                }

                String referer = request.getHeader("Referer");
                if (referer == null || referer.isBlank()) {
                        return "redirect:/books";
                }

                String base = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
                if (referer.startsWith(base)) {
                        String path = referer.substring(base.length());
                        return "redirect:" + (path.isBlank() ? "/books" : path);
                }
                if (referer.startsWith("/")) {
                        return "redirect:" + referer;
                }
                return "redirect:/books";
        }

        @GetMapping("/search")
        public String searchBook(
                        @RequestParam String keyword,
                        @RequestParam(defaultValue = "0") Integer pageNo,
                        @RequestParam(defaultValue = "20") Integer pageSize,
                        @RequestParam(defaultValue = "id") String sortBy) {
                String encoded = UriUtils.encodeQueryParam(keyword, StandardCharsets.UTF_8);
                return "redirect:/books?keyword=" + encoded + "&pageNo=" + pageNo + "&pageSize=" + pageSize + "&sortBy="
                                + sortBy;
        }

        @GetMapping("/suggest")
        @ResponseBody
        public List<String> suggest(@RequestParam(name = "q", required = false) String q) {
                return bookService.suggestTitles(q);
        }

        @PostMapping("/import")
        public String importBooks(@RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                        Authentication authentication,
                        RedirectAttributes redirectAttributes) {
                if (file.isEmpty()) {
                        redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn file Excel để import");
                        return "redirect:/books";
                }
                try {
                        var currentUser = getCurrentUser(authentication);
                        excelService.importBooks(file, currentUser);
                        redirectAttributes.addFlashAttribute("successMessage", "Import danh sách sách thành công!");
                } catch (Exception e) {
                        redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi import file: " + e.getMessage());
                }
                return "redirect:/books";
        }
}
