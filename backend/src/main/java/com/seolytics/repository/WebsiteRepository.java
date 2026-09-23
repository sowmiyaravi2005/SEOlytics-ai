package com.seolytics.repository;

import com.seolytics.entity.UserAccount;
import com.seolytics.entity.Website;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WebsiteRepository extends JpaRepository<Website, Long> {
    Optional<Website> findByOwnerAndDomain(UserAccount owner, String domain);
    List<Website> findByOwner(UserAccount owner);
    long countByOwner(UserAccount owner);
}
