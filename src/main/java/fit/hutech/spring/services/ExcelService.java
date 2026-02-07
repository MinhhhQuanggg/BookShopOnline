package fit.hutech.spring.services;

import fit.hutech.spring.entities.Book;
import fit.hutech.spring.entities.Category;
import fit.hutech.spring.repositories.IBookRepository;
import fit.hutech.spring.repositories.ICategoryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExcelService {

    private final IBookRepository bookRepository;
    private final ICategoryRepository categoryRepository;

    public void importBooks(MultipartFile file, fit.hutech.spring.entities.User user) throws IOException {
        List<Book> books = new ArrayList<>();
        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // Skip header
            if (rows.hasNext()) {
                rows.next();
            }

            while (rows.hasNext()) {
                Row currentRow = rows.next();
                Book book = new Book();

                // 0: Title, 1: Author, 2: Price, 3: Category Name, 4: Stock
                Cell titleCell = currentRow.getCell(0);
                if (titleCell != null)
                    book.setTitle(titleCell.getStringCellValue());

                Cell authorCell = currentRow.getCell(1);
                if (authorCell != null)
                    book.setAuthor(authorCell.getStringCellValue());

                Cell priceCell = currentRow.getCell(2);
                if (priceCell != null)
                    book.setPrice(priceCell.getNumericCellValue());

                Cell categoryCell = currentRow.getCell(3);
                if (categoryCell != null) {
                    String categoryName = categoryCell.getStringCellValue();
                    Category category = categoryRepository.findByName(categoryName)
                            .orElseGet(() -> {
                                Category newCat = Category.builder().name(categoryName).build();
                                return categoryRepository.save(newCat);
                            });
                    book.setCategory(category);
                }

                Cell stockCell = currentRow.getCell(4);
                if (stockCell != null) {
                    book.setStock((int) stockCell.getNumericCellValue());
                } else {
                    book.setStock(0);
                }

                book.setActive(true);
                book.setUser(user);
                books.add(book);
            }
        }
        bookRepository.saveAll(books);
    }
}
