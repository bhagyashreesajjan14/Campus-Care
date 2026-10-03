package com.campuscare.repository;

import com.campuscare.model.Resolver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResolverRepository extends JpaRepository<Resolver, Long> {
    Resolver findFirstByDepartmentAndActiveTrue(String department);
}
