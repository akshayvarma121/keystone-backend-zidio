package com.zidio.keystone.repository;

import com.zidio.keystone.domain.Site;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SiteRepository extends JpaRepository<Site, UUID> {

    @Query("SELECT s FROM Site s WHERE " +
           "(:name IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%'))) AND " +
           "(:customerId IS NULL OR s.customer.id = :customerId)")
    Page<Site> searchSites(@Param("name") String name, @Param("customerId") UUID customerId, Pageable pageable);
}
