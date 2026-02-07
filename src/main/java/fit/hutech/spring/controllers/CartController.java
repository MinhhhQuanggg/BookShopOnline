package fit.hutech.spring.controllers;
import fit.hutech.spring.services.CartService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @GetMapping
    public String showCart(HttpSession session,
                           Authentication authentication,
                           @NotNull Model model) {
        model.addAttribute("cart", cartService.getCart(session, authentication));
        model.addAttribute("totalPrice",
                cartService.getSumPrice(session));
        model.addAttribute("totalQuantity",
                cartService.getSumQuantity(session));
        return "book/cart";
    }

    @GetMapping("/removeFromCart/{id}")
    public String removeFromCart(HttpSession session,
                                 Authentication authentication,
                                 @PathVariable Long id) {
        cartService.removeFromCart(session, authentication, id);
        return "redirect:/cart";
    }

//    @GetMapping("/updateCart/{id}/{quantity}")
//    public String updateCart(HttpSession session,
//                             @PathVariable int id,
//                             @PathVariable int quantity) {
//        var cart = cartService.getCart(session);
//        cart.updateItems(id, quantity);
//        return "book/cart";
//    }

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
    public String checkout(HttpSession session, Authentication authentication) {
        boolean saved = cartService.saveCart(session, authentication);
        return saved ? "redirect:/cart?checkout=success" : "redirect:/cart?checkout=empty";
    }

    @PostMapping("/checkout-selected")
    public String checkoutSelected(HttpSession session,
                                   Authentication authentication,
                                   @RequestParam(name = "bookIds", required = false) List<Long> bookIds) {
        boolean saved = cartService.saveSelectedCart(session, authentication, bookIds);
        return saved ? "redirect:/cart?checkout=success" : "redirect:/cart?checkout=empty";
    }

    @GetMapping("/checkout")
    public String checkoutGet(HttpSession session, Authentication authentication) {
        return "redirect:/cart";
    }
}
