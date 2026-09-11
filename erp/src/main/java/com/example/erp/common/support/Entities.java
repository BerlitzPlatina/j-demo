package com.example.erp.common.support;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.common.web.exception.ResourceNotFoundException;

/**
 * The lookup every service starts a read, an update and a delete with.
 * <p>
 * It exists so that a missing row is a 404 with the same wording everywhere, instead of each
 * feature deciding for itself - or, worse, letting an empty {@code Optional} through.
 */
public final class Entities {

    private Entities() {
    }

    /**
     * @param entityName how the entity is named to the caller, e.g. {@code "Tax"}
     * @throws ResourceNotFoundException if no row has that id
     */
    public static <E> E findOrThrow(JpaRepository<E, Long> dao, Long id, String entityName) {
        return dao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(entityName + " not found with id: " + id));
    }
}
