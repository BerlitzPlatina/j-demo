package com.example.erp.contact.repository;

import com.example.erp.contact.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Contact Dao.
 * <p>
 * {@link JpaSpecificationExecutor} is what keeps the search off this interface: the filters are
 * composed in {@link ContactSpecifications} and handed to {@code findAll(Specification, Pageable)},
 * so a new filter does not add a derived-query method name here.
 */
@Repository
public interface ContactDao extends JpaRepository<Contact, Long>, JpaSpecificationExecutor<Contact> {
}
