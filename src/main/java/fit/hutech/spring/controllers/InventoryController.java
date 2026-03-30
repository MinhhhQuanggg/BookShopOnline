package fit.hutech.spring.controllers;

import fit.hutech.spring.services.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/inventory")
@PreAuthorize("hasAuthority('ADMIN')")
public class InventoryController {
    private final InventoryService inventoryService;

    @GetMapping
    public String showInventory(Model model) {
        model.addAttribute("movements", inventoryService.getRecentMovements());
        return "inventory/list";
    }
}
