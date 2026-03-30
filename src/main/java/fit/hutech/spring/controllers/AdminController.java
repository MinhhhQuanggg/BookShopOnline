package fit.hutech.spring.controllers;

import fit.hutech.spring.services.RoleService;
import fit.hutech.spring.services.UserService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final UserService userService;
    private final RoleService roleService;

    @GetMapping("/users")
    public String users(@NotNull Model model) {
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("allRoles", roleService.getAllRoles());
        return "admin/users";
    }

    /**
     * Xem chi tiết user
     */
    @GetMapping("/users/{id}")
    public String userDetail(@PathVariable Long id, @NotNull Model model) {
        var user = userService.getUserById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        model.addAttribute("user", user);
        return "admin/user-detail";
    }

    /**
     * Cập nhật role đơn cho user (giữ nguyên để backward compatible)
     */
    @PostMapping("/users/{id}/role")
    public String setUserRole(@PathVariable Long id, @RequestParam String role) {
        userService.setSingleRole(id, role);
        return "redirect:/admin/users";
    }

    /**
     * Cập nhật nhiều roles cho user
     */
    @PostMapping("/users/{id}/roles")
    public String updateUserRoles(
            @PathVariable Long id,
            @RequestParam(required = false) List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            // Nếu không chọn role nào, set mặc định là USER
            userService.setSingleRole(id, "USER");
        } else {
            userService.updateUserRoles(id, new java.util.HashSet<>(roleIds));
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/users/add")
    public String addUserForm(@NotNull Model model) {
        model.addAttribute("user", new fit.hutech.spring.entities.User());
        return "admin/user-add";
    }

    @PostMapping("/users/add")
    public String addUser(
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.ModelAttribute("user") fit.hutech.spring.entities.User user,
            org.springframework.validation.BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            var errors = bindingResult.getAllErrors()
                    .stream()
                    .map(org.springframework.context.support.DefaultMessageSourceResolvable::getDefaultMessage)
                    .toArray(String[]::new);
            model.addAttribute("errors", errors);
            return "admin/user-add";
        }

        if (userService.findByUsername(user.getUsername()).isPresent()) {
            model.addAttribute("errors", new String[] { "Tên đăng nhập đã tồn tại" });
            return "admin/user-add";
        }
        if (userService.findByEmail(user.getEmail()).isPresent()) {
            model.addAttribute("errors", new String[] { "Email đã tồn tại" });
            return "admin/user-add";
        }

        userService.save(user);
        userService.setDefaultRole(user.getUsername());
        return "redirect:/admin/users";
    }

    @GetMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUserById(id);
        return "redirect:/admin/users";
    }
}


