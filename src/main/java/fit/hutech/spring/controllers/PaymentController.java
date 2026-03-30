package fit.hutech.spring.controllers;

import fit.hutech.spring.services.CartService;
import fit.hutech.spring.services.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    private final CartService cartService;

    @GetMapping("/mock/{invoiceId}")
    public String mockPayment(@PathVariable Long invoiceId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        try {
            cartService.confirmPayment(invoiceId, "MOCK-" + invoiceId, "00", session);
            redirectAttributes.addFlashAttribute("successMessage", "Thanh toán thành công.");
            return "redirect:/cart?payment=success";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Thanh toán thất bại: " + ex.getMessage());
            return "redirect:/cart?payment=failed";
        }
    }

    @GetMapping("/vnpay/return")
    public String vnpayReturn(HttpServletRequest request,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        try {
            PaymentService.PaymentReturn paymentReturn = paymentService.parseVnpayReturn(request.getParameterMap());
            if (paymentReturn.invoiceId() == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy hóa đơn thanh toán.");
                return "redirect:/cart?payment=failed";
            }

            if (paymentReturn.success()) {
                cartService.confirmPayment(paymentReturn.invoiceId(), paymentReturn.transactionNo(),
                        paymentReturn.responseCode(), session);
                redirectAttributes.addFlashAttribute("successMessage", "Thanh toán thành công.");
                return "redirect:/cart?payment=success";
            }

            cartService.failPayment(paymentReturn.invoiceId(), paymentReturn.responseCode(), paymentReturn.message());
            redirectAttributes.addFlashAttribute("errorMessage", "Thanh toán không thành công.");
            return "redirect:/cart?payment=failed";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xử lý thanh toán: " + ex.getMessage());
            return "redirect:/cart?payment=failed";
        }
    }
}
