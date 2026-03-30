package fit.hutech.spring.services;

import fit.hutech.spring.daos.Cart;
import fit.hutech.spring.daos.Item;
import fit.hutech.spring.entities.Book;
import fit.hutech.spring.entities.Invoice;
import fit.hutech.spring.entities.ItemInvoice;
import fit.hutech.spring.entities.PaymentMethod;
import fit.hutech.spring.entities.PaymentStatus;
import fit.hutech.spring.entities.StockMovementType;
import fit.hutech.spring.entities.User;
import fit.hutech.spring.repositories.IBookRepository;
import fit.hutech.spring.repositories.ICartRepository;
import fit.hutech.spring.repositories.IInvoiceRepository;
import fit.hutech.spring.repositories.IItemInvoiceRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = { Exception.class, Throwable.class })
public class CartService {
    private static final String CART_SESSION_KEY = "cart";
    private static final int MAX_DISTINCT_ITEMS = 50;
    private static final int MAX_PER_ITEM = 10;
    private final IInvoiceRepository invoiceRepository;
    private final IItemInvoiceRepository itemInvoiceRepository;
    private final IBookRepository bookRepository;
    private final ICartRepository cartRepository;
    private final UserService userService;
    private final InventoryService inventoryService;

    private User resolveUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User u) {
            return u;
        }

        if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
            return userService.findByUsername(ud.getUsername()).orElse(null);
        }

        if (principal instanceof OAuth2User oauth2User) {
            Object email = oauth2User.getAttributes().get("email");
            if (email instanceof String s && !s.isBlank()) {
                return userService.findByEmail(s).orElse(null);
            }
        }

        return null;
    }

    private Cart buildSessionCartFromDb(@NotNull User user) {
        Cart cart = new Cart();
        var rows = cartRepository.findAllByUser_Id(user.getId());
        rows.forEach(row -> {
            var book = row.getBook();
            cart.addItems(new Item(
                    book.getId(),
                    book.getTitle(),
                    row.getUnitPrice() == null ? book.getPrice() : row.getUnitPrice().doubleValue(),
                    row.getQuantity() == null ? 1 : row.getQuantity()));
        });
        return cart;
    }

    public Cart getCart(@NotNull HttpSession session, Authentication authentication) {
        Cart existing = (Cart) session.getAttribute(CART_SESSION_KEY);
        if (existing != null)
            return existing;

        User user = resolveUser(authentication);
        Cart cart = (user == null) ? new Cart() : buildSessionCartFromDb(user);
        session.setAttribute(CART_SESSION_KEY, cart);
        return cart;
    }

    public Cart getCart(@NotNull HttpSession session) {
        return getCart(session, null);
    }

    public void updateCart(@NotNull HttpSession session, Cart cart) {
        session.setAttribute(CART_SESSION_KEY, cart);
    }

    public void removeCart(@NotNull HttpSession session) {
        session.removeAttribute(CART_SESSION_KEY);
    }

    public int getSumQuantity(@NotNull HttpSession session) {
        return getCart(session).getCartItems().stream().mapToInt(Item::getQuantity).sum();
    }

    public double getSumPrice(@NotNull HttpSession session) {
        return getCart(session).getCartItems().stream().mapToDouble(item -> item.getPrice() * item.getQuantity()).sum();
    }

    private void ensureCartLimits(@NotNull Cart cart, Long addingBookId) {
        if (cart.getCartItems().size() > MAX_DISTINCT_ITEMS) {
            throw new IllegalArgumentException("Cart cannot exceed " + MAX_DISTINCT_ITEMS + " different books");
        }
        boolean exists = cart.getCartItems().stream().anyMatch(i -> i.getBookId().equals(addingBookId));
        if (!exists && cart.getCartItems().size() >= MAX_DISTINCT_ITEMS) {
            throw new IllegalArgumentException("Cart cannot exceed " + MAX_DISTINCT_ITEMS + " different books");
        }
    }

    private int normalizeQuantity(int quantity) {
        if (quantity < 1)
            throw new IllegalArgumentException("Quantity must be at least 1");
        if (quantity > MAX_PER_ITEM)
            throw new IllegalArgumentException("Quantity must be at most " + MAX_PER_ITEM);
        return quantity;
    }

    public void addToCart(@NotNull HttpSession session, Authentication authentication, @NotNull Long bookId,
            int quantity) {
        User user = resolveUser(authentication);
        if (user == null) {
            throw new IllegalStateException("User not authenticated");
        }

        int addQty = normalizeQuantity(quantity);
        var book = bookRepository.findById(bookId).orElseThrow();
        if (!book.isActiveForSale()) {
            throw new IllegalArgumentException("Book is not active for sale");
        }

        var cart = getCart(session, authentication);
        ensureCartLimits(cart, book.getId());

        int existingQty = cart.getCartItems().stream()
                .filter(i -> i.getBookId().equals(book.getId()))
                .map(Item::getQuantity)
                .findFirst()
                .orElse(0);
        int newQty = normalizeQuantity(existingQty + addQty);
        if (newQty > book.getStockSafe()) {
            throw new IllegalArgumentException("Quantity exceeds stock");
        }

        cart.addItems(new Item(book.getId(), book.getTitle(), book.getPrice(), addQty));
        updateCart(session, cart);

        var existing = cartRepository.findByUser_IdAndBook_Id(user.getId(), book.getId()).orElse(null);
        if (existing == null) {
            cartRepository.save(fit.hutech.spring.entities.Cart.builder()
                    .user(user)
                    .book(book)
                    .quantity(addQty)
                    .unitPrice(BigDecimal.valueOf(book.getPrice()))
                    .build());
        } else {
            int mergedQty = (existing.getQuantity() == null ? 0 : existing.getQuantity()) + addQty;
            int finalQty = normalizeQuantity(mergedQty);
            if (finalQty > book.getStockSafe()) {
                throw new IllegalArgumentException("Quantity exceeds stock");
            }
            existing.setQuantity(finalQty);
            existing.setUnitPrice(BigDecimal.valueOf(book.getPrice()));
            cartRepository.save(existing);
        }
    }

    public void updateQuantity(@NotNull HttpSession session, Authentication authentication, @NotNull Long bookId,
            int quantity) {
        User user = resolveUser(authentication);
        if (user == null) {
            throw new IllegalStateException("User not authenticated");
        }

        int newQty = normalizeQuantity(quantity);
        var book = bookRepository.findById(bookId).orElseThrow();
        if (!book.isActiveForSale()) {
            throw new IllegalArgumentException("Book is not active for sale");
        }
        if (newQty > book.getStockSafe()) {
            throw new IllegalArgumentException("Quantity exceeds stock");
        }

        var cart = getCart(session, authentication);
        cart.updateItems(bookId, newQty);
        updateCart(session, cart);

        var row = cartRepository.findByUser_IdAndBook_Id(user.getId(), bookId).orElse(null);
        if (row == null) {
            cartRepository.save(fit.hutech.spring.entities.Cart.builder()
                    .user(user)
                    .book(book)
                    .quantity(newQty)
                    .unitPrice(BigDecimal.valueOf(book.getPrice()))
                    .build());
        } else {
            row.setQuantity(newQty);
            row.setUnitPrice(BigDecimal.valueOf(book.getPrice()));
            cartRepository.save(row);
        }
    }

    public void removeFromCart(@NotNull HttpSession session, Authentication authentication, @NotNull Long bookId) {
        User user = resolveUser(authentication);
        if (user == null) {
            throw new IllegalStateException("User not authenticated");
        }

        var cart = getCart(session, authentication);
        cart.removeItems(bookId);
        updateCart(session, cart);

        cartRepository.deleteByUser_IdAndBook_Id(user.getId(), bookId);
    }

    public void clearCart(@NotNull HttpSession session, Authentication authentication) {
        User user = resolveUser(authentication);
        if (user == null) {
            throw new IllegalStateException("User not authenticated");
        }
        removeCart(session);
        cartRepository.deleteByUser_Id(user.getId());
    }

    private List<Item> resolveCheckoutItems(Cart cart, List<Long> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) {
            return cart.getCartItems();
        }
        Set<Long> selectedIds = new HashSet<>(bookIds);
        return cart.getCartItems().stream().filter(i -> selectedIds.contains(i.getBookId())).toList();
    }

    private Invoice createPendingInvoiceInternal(@NotNull HttpSession session,
            Authentication authentication,
            List<Long> bookIds) {
        User user = resolveUser(authentication);
        if (user == null) {
            throw new IllegalStateException("User not authenticated");
        }

        var cart = getCart(session, authentication);
        if (cart.getCartItems().isEmpty())
            return null;

        var checkoutItems = resolveCheckoutItems(cart, bookIds);
        if (checkoutItems.isEmpty())
            return null;
        if (checkoutItems.size() > MAX_DISTINCT_ITEMS) {
            throw new IllegalArgumentException("Cart cannot exceed " + MAX_DISTINCT_ITEMS + " different books");
        }

        var ids = checkoutItems.stream().map(Item::getBookId).toList();
        var books = bookRepository.findAllById(ids);
        if (books.size() != ids.size()) {
            throw new IllegalArgumentException("Some books do not exist");
        }
        Map<Long, Book> bookMap = books.stream().collect(Collectors.toMap(Book::getId, b -> b));

        double total = 0;
        for (Item item : checkoutItems) {
            int qty = normalizeQuantity(item.getQuantity());
            var book = bookMap.get(item.getBookId());
            if (book == null)
                throw new IllegalArgumentException("Book not found");
            if (!book.isActiveForSale())
                throw new IllegalArgumentException("Book is not active for sale");
            if (qty > book.getStockSafe())
                throw new IllegalArgumentException("Quantity exceeds stock");
            total += (book.getPrice() == null ? 0 : book.getPrice()) * qty;
        }

        Invoice invoice = new Invoice();
        invoice.setInvoiceDate(new Date());
        invoice.setPrice(total);
        invoice.setUser(user);
        invoice.setPaymentStatus(PaymentStatus.PENDING);
        invoice.setPaymentMethod(PaymentMethod.VNPAY);
        invoice = invoiceRepository.save(invoice);

        for (Item item : checkoutItems) {
            ItemInvoice invoiceItem = new ItemInvoice();
            invoiceItem.setInvoice(invoice);
            invoiceItem.setQuantity(normalizeQuantity(item.getQuantity()));
            invoiceItem.setBook(bookMap.get(item.getBookId()));
            itemInvoiceRepository.save(invoiceItem);
        }

        return invoice;
    }

    public Invoice createPendingInvoice(@NotNull HttpSession session, Authentication authentication) {
        return createPendingInvoiceInternal(session, authentication, null);
    }

    public Invoice createPendingInvoice(@NotNull HttpSession session, Authentication authentication,
            List<Long> bookIds) {
        return createPendingInvoiceInternal(session, authentication, bookIds);
    }

    @Transactional
    public Invoice confirmPayment(Long invoiceId, String transactionId, String responseCode, HttpSession session) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        if (invoice.getPaymentStatus() == PaymentStatus.PAID) {
            return invoice;
        }
        if (invoice.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Invoice is not pending payment");
        }

        var invoiceItems = itemInvoiceRepository.findByInvoice_Id(invoiceId);
        var bookIds = invoiceItems.stream().map(item -> item.getBook().getId()).toList();
        var books = bookRepository.findAllById(bookIds);
        Map<Long, Book> bookMap = books.stream().collect(Collectors.toMap(Book::getId, b -> b));

        for (ItemInvoice item : invoiceItems) {
            Book book = bookMap.get(item.getBook().getId());
            if (book == null) {
                throw new IllegalArgumentException("Book not found");
            }
            int qty = normalizeQuantity(item.getQuantity());
            int currentStock = book.getStockSafe();
            if (qty > currentStock) {
                throw new IllegalArgumentException("Quantity exceeds stock");
            }
            int newStock = currentStock - qty;
            book.setStock(newStock);
            inventoryService.recordMovement(book,
                    invoice.getUser(),
                    invoice,
                    StockMovementType.SALE,
                    currentStock,
                    newStock,
                    "Thanh toán hóa đơn #" + invoice.getId());
        }
        bookRepository.saveAll(books);

        invoice.setPaymentStatus(PaymentStatus.PAID);
        invoice.setPaymentMethod(
                transactionId != null && transactionId.startsWith("MOCK-") ? PaymentMethod.MOCK : PaymentMethod.VNPAY);
        invoice.setPaymentTransactionId(transactionId);
        invoice.setPaymentMessage(responseCode);
        invoice.setPaidAt(new Date());
        invoiceRepository.save(invoice);

        if (session != null) {
            Cart cart = (Cart) session.getAttribute(CART_SESSION_KEY);
            if (cart != null) {
                Set<Long> purchasedIds = new HashSet<>(bookIds);
                cart.getCartItems().removeIf(item -> purchasedIds.contains(item.getBookId()));
                if (cart.getCartItems().isEmpty()) {
                    removeCart(session);
                } else {
                    updateCart(session, cart);
                }
            }
        }

        User user = invoice.getUser();
        if (user != null) {
            for (Long bookId : bookIds) {
                cartRepository.deleteByUser_IdAndBook_Id(user.getId(), bookId);
            }
        }

        return invoice;
    }

    @Transactional
    public Invoice failPayment(Long invoiceId, String responseCode, String message) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        if (invoice.getPaymentStatus() == PaymentStatus.PAID) {
            return invoice;
        }
        invoice.setPaymentStatus(PaymentStatus.FAILED);
        invoice.setPaymentMessage(message != null ? message : responseCode);
        return invoiceRepository.save(invoice);
    }
}
