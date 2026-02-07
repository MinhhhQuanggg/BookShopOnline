package fit.hutech.spring.controllers;

import fit.hutech.spring.entities.Category;
import fit.hutech.spring.services.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public String listCategories(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        return "category/list";
    }

    @GetMapping("/add")
    public String addCategoryForm(Model model) {
        model.addAttribute("category", new Category());
        return "category/add";
    }

    @PostMapping("/add")
    public String addCategory(@Valid @ModelAttribute("category") Category category,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            return "category/add";
        }
        categoryService.addCategory(category);
        return "redirect:/categories";
    }

    @GetMapping("/edit/{id}")
    public String editCategoryForm(@PathVariable("id") Long id, Model model) {
        var category = categoryService.getCategoryById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid category Id:" + id));
        model.addAttribute("category", category);
        return "category/edit";
    }

    @PostMapping("/edit")
    public String updateCategory(@Valid @ModelAttribute("category") Category category,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            return "category/edit";
        }
        categoryService.updateCategory(category);
        return "redirect:/categories";
    }

    @GetMapping("/delete/{id}")
    public String deleteCategory(@PathVariable("id") Long id) {
        categoryService.deleteCategoryById(id);
        return "redirect:/categories";
    }
}
