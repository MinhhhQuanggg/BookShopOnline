package fit.hutech.spring.controllers;

import fit.hutech.spring.entities.Role;
import fit.hutech.spring.services.RoleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/roles")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class RoleController {
    private final RoleService roleService;

    /**
     * Hiển thị danh sách roles
     */
    @GetMapping
    public String listRoles(@NotNull Model model) {
        var roles = roleService.getAllRoles();
        model.addAttribute("roles", roles);
        model.addAttribute("newRole", new Role());
        return "admin/roles";
    }

    /**
     * Xem chi tiết role
     */
    @GetMapping("/{id}")
    public String roleDetail(@PathVariable Long id, @NotNull Model model) {
        Role role = roleService.getRoleById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + id));

        int userCount = roleService.countUsersByRoleId(id);

        model.addAttribute("role", role);
        model.addAttribute("userCount", userCount);
        return "admin/role-detail";
    }

    /**
     * Tạo role mới
     */
    @PostMapping
    public String createRole(
            @Valid @ModelAttribute("newRole") Role role,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Invalid role data");
            return "redirect:/admin/roles";
        }

        try {
            roleService.createRole(role);
            redirectAttributes.addFlashAttribute("success", "Role created successfully");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/roles";
    }

    /**
     * Cập nhật role
     */
    @PostMapping("/{id}")
    public String updateRole(
            @PathVariable Long id,
            @Valid @ModelAttribute Role role,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Invalid role data");
            return "redirect:/admin/roles";
        }

        try {
            roleService.updateRole(id, role);
            redirectAttributes.addFlashAttribute("success", "Role updated successfully");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/roles";
    }

    /**
     * Xóa role
     */
    @PostMapping("/{id}/delete")
    public String deleteRole(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            roleService.deleteRole(id);
            redirectAttributes.addFlashAttribute("success", "Role deleted successfully");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/roles";
    }
}


