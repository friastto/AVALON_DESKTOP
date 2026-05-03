package org.avalon.desktop.sales.domain.repository;

import org.avalon.desktop.sales.domain.model.Return;

import java.sql.Connection;
import java.util.Optional;

public interface ReturnRepository {
    void save(Return returnObj);
    Optional<Return> findById(Long id);

    // Métodos transaccionales (aceptan Connection)
    void save(Return returnObj, Connection conn);
    Optional<Return> findById(Long id, Connection conn);
}
