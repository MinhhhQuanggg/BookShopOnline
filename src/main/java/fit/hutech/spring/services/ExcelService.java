package fit.hutech.spring.services;

import fit.hutech.spring.entities.Book;
import fit.hutech.spring.entities.Category;
import fit.hutech.spring.repositories.IBookRepository;
import fit.hutech.spring.repositories.ICategoryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExcelService {

    private final IBookRepository bookRepository;
    private final ICategoryRepository categoryRepository;
    private final InventoryService inventoryService;

    public void importBooks(MultipartFile file, fit.hutech.spring.entities.User user) throws IOException {
        List<Book> books = new ArrayList<>();
        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            var rows = sheet.iterator();

            if (rows.hasNext()) {
                rows.next();
            }

            while (rows.hasNext()) {
                Row currentRow = rows.next();
                Book book = new Book();

                Cell titleCell = currentRow.getCell(0);
                if (titleCell != null) {
                    book.setTitle(titleCell.getStringCellValue());
                }

                Cell authorCell = currentRow.getCell(1);
                if (authorCell != null) {
                    book.setAuthor(authorCell.getStringCellValue());
                }

                Cell priceCell = currentRow.getCell(2);
                if (priceCell != null) {
                    book.setPrice(priceCell.getNumericCellValue());
                }

                Cell categoryCell = currentRow.getCell(3);
                if (categoryCell != null) {
                    String categoryName = categoryCell.getStringCellValue();
                    Category category = categoryRepository.findByName(categoryName)
                            .orElseGet(() -> categoryRepository.save(Category.builder().name(categoryName).build()));
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

        var savedBooks = bookRepository.saveAll(books);
        for (Book savedBook : savedBooks) {
            int openingStock = savedBook.getStockSafe();
            inventoryService.recordOpeningStock(savedBook, user, openingStock, "Import Excel");
        }
    }

    public byte[] exportBooks() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Books");

            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row header = sheet.createRow(0);
            String[] columns = {"ID", "Title", "Author", "Price", "Category", "Stock", "Active"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            List<Book> books = bookRepository.findAll();
            int rowIndex = 1;
            for (Book book : books) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(book.getId() == null ? 0 : book.getId());
                row.createCell(1).setCellValue(book.getTitle() == null ? "" : book.getTitle());
                row.createCell(2).setCellValue(book.getAuthor() == null ? "" : book.getAuthor());
                row.createCell(3).setCellValue(book.getPrice() == null ? 0 : book.getPrice());
                row.createCell(4).setCellValue(book.getCategory() == null ? "" : book.getCategory().getName());
                row.createCell(5).setCellValue(book.getStockSafe());
                row.createCell(6).setCellValue(Boolean.TRUE.equals(book.getActive()) ? "Active" : "Inactive");
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}