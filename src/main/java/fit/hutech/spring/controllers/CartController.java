package fit.hutech.spring.controllers;

import fit.hutech.spring.entities.Invoice;
import fit.hutech.spring.services.CartService;
import fit.hutech.spring.services.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;
    private final PaymentService paymentService;

    @GetMapping
    public String showCart(HttpSession session,
            Authentication authentication,
            @NotNull Model model) {
        model.addAttribute("cart", cartService.getCart(session, authentication));
        model.addAttribute("totalPrice", cartService.getSumPrice(session));
        model.addAttribute("totalQuantity", cartService.getSumQuantity(session));
        return "book/cart";
    }

    @GetMapping("/removeFromCart/{id}")
    public String removeFromCart(HttpSession session,
            Authentication authentication,
            @PathVariable Long id) {
        cartService.removeFromCart(session, authentication, id);
        return "redirect:/cart";
    }

    @PostMapping("/update")
    @ResponseBody
    public String updateCart(HttpSession session,
            Authentication authentication,
            @RequestParam Long bookId,
            @RequestParam int quantity) {
        cartService.updateQuantity(session, authentication, bookId, quantity);
        return "ok";
    }

    @GetMapping("/clearCart")
    public String clearCart(HttpSession session, Authentication authentication) {
        cartService.clearCart(session, authentication);
        return "redirect:/cart";
    }

    @PostMapping("/checkout")
    public String checkout(HttpSession session,
            Authentication authentication,
            HttpServletRequest request,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            Invoice invoice = cartService.createPendingInvoice(session, authentication);
            if (invoice == null) {
                return "redirect:/cart?checkout=empty";
            }
            String paymentUrl = paymentService.createPaymentUrl(invoice, request);
            return "redirect:" + paymentUrl;
        } catch (IllegalArgumentException | IllegalStateException e) {
            String msg = e.getMessage();
            if ("Cart cannot exceed 50 different books".equals(msg))
                msg = "Giỏ hàng không được vượt quá 50 loại sách.";
            else if ("Some books do not exist".equals(msg))
                msg = "Một số sách trong giỏ không tồn tại.";
            else if ("Book not found".equals(msg))
                msg = "Không tìm thấy sách.";
            else if ("Book is not active for sale".equals(msg))
                msg = "Sách hiện không bán.";
            else if ("Quantity exceeds stock".equals(msg))
                msg = "Số lượng trong kho không đủ.";
            redirectAttributes.addFlashAttribute("errorMessage", msg);
            return "redirect:/cart";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi tạo hóa đơn: " + e.getMessage());
            return "redirect:/cart";
        }
    }

    @PostMapping("/checkout-selected")
    public String checkoutSelected(HttpSession session,
            Authentication authentication,
            HttpServletRequest request,
            @RequestParam(name = "bookIds", required = false) List<Long> bookIds,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            Invoice invoice = cartService.createPendingInvoice(session, authentication, bookIds);
            if (invoice == null) {
                return "redirect:/cart?checkout=empty";
            }
            String paymentUrl = paymentService.createPaymentUrl(invoice, request);
            return "redirect:" + paymentUrl;
        } catch (IllegalArgumentException | IllegalStateException e) {
            String msg = e.getMessage();
            if ("Cart cannot exceed 50 different books".equals(msg))
                msg = "Giỏ hàng không được vượt quá 50 loại sách.";
            else if ("Some books do not exist".equals(msg))
                msg = "Một số sách trong giỏ không tồn tại.";
            else if ("Book not found".equals(msg))
                msg = "Không tìm thấy sách.";
            else if ("Book is not active for sale".equals(msg))
                msg = "Sách hiện không bán.";
            else if ("Quantity exceeds stock".equals(msg))
                msg = "Số lượng trong kho không đủ.";
            redirectAttributes.addFlashAttribute("errorMessage", msg);
            return "redirect:/cart";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi tạo hóa đơn: " + e.getMessage());
            return "redirect:/cart";
        }
    }

    @GetMapping("/checkout")
    public String checkoutGet(HttpSession session, Authentication authentication) {
        return "redirect:/cart";
    }
}
