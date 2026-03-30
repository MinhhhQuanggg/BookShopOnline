package fit.hutech.spring.repositories;

import fit.hutech.spring.entities.ItemInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IItemInvoiceRepository extends JpaRepository<ItemInvoice, Long> {
    boolean existsByBookId(Long bookId);

    List<ItemInvoice> findByInvoice_Id(Long invoiceId);
}
