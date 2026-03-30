package com.laetienda.schema.repository;

import com.laetienda.model.schema.DbGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DbGroupRepository extends JpaRepository<DbGroup, Long> {
//    DbGroup findById(long id);
    DbGroup findByName(String name);
}
