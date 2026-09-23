package com.seolytics.repository;

import com.seolytics.entity.CrawledPage;
import com.seolytics.entity.CrawlSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CrawledPageRepository extends JpaRepository<CrawledPage, Long> {
    List<CrawledPage> findBySessionOrderByIdAsc(CrawlSession session);
    Optional<CrawledPage> findByIdAndSession_Owner_Id(Long id, Long ownerId);
    long countBySession(CrawlSession session);

    void deleteBySession(CrawlSession session);
}
