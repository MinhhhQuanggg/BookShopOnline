package fit.hutech.spring.repositories;

import fit.hutech.spring.entities.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IBookRepository extends
        PagingAndSortingRepository<Book, Long>, JpaRepository<Book, Long> {
    @Query("""
                SELECT b FROM Book b
                WHERE b.title LIKE %?1%
                OR b.author LIKE %?1%
                OR b.category.name LIKE %?1%
            """)
    List<Book> searchBook(String keyword);

    @Query(value = """
                SELECT b FROM Book b
                WHERE b.title LIKE %?1%
                OR b.author LIKE %?1%
                OR b.category.name LIKE %?1%
            """, countQuery = """
                SELECT COUNT(b) FROM Book b
                WHERE b.title LIKE %?1%
                OR b.author LIKE %?1%
                OR b.category.name LIKE %?1%
            """)
    Page<Book> searchBook(String keyword, Pageable pageable);

    @Query("""
                SELECT b FROM Book b
                WHERE (:categoryId IS NULL OR b.category.id = :categoryId)
                AND (:keyword IS NULL OR b.title LIKE %:keyword% OR b.author LIKE %:keyword%)
            """)
    Page<Book> searchBookWithCategory(@Param("keyword") String keyword, @Param("categoryId") Long categoryId,
            Pageable pageable);

    @Query("""
                SELECT DISTINCT b.title
                FROM Book b
                WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :q, '%'))
                ORDER BY b.title
            """)
    List<String> suggestTitles(@Param("q") String q, Pageable pageable);
}
