package com.seolytics.repository;

import com.seolytics.domain.CrawlStatus;
import com.seolytics.entity.CrawlSession;
import com.seolytics.entity.UserAccount;
import com.seolytics.entity.Website;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CrawlSessionRepository extends JpaRepository<CrawlSession, Long> {

    @Query("select s from CrawlSession s join fetch s.website where s.id = :id and s.owner = :owner")
    Optional<CrawlSession> findByIdAndOwner(@Param("id") Long id, @Param("owner") UserAccount owner);

    @Query("select s from CrawlSession s join fetch s.website where s.owner = :owner order by s.createdAt desc")
    List<CrawlSession> findByOwnerOrderByCreatedAtDesc(@Param("owner") UserAccount owner);

    List<CrawlSession> findByOwnerAndWebsiteOrderByCreatedAtDesc(UserAccount owner, Website website);

    Optional<CrawlSession> findTopByWebsiteAndIdLessThanOrderByIdDesc(Website website, Long id);

    long countByOwner(UserAccount owner);

    @Query("select coalesce(sum(s.pagesCrawled), 0) from CrawlSession s where s.owner = :owner")
    long sumPagesCrawled(@Param("owner") UserAccount owner);

    @Query("select avg(s.seoScore) from CrawlSession s where s.owner = :owner and s.status in :statuses and s.seoScore is not null")
    Double averageScore(@Param("owner") UserAccount owner, @Param("statuses") List<CrawlStatus> statuses);

    List<CrawlSession> findTop8ByOwnerAndStatusInOrderByFinishedAtDesc(UserAccount owner, List<CrawlStatus> statuses);
}
