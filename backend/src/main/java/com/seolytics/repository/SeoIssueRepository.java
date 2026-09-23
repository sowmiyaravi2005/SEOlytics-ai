package com.seolytics.repository;

import com.seolytics.domain.IssueSeverity;
import com.seolytics.domain.IssueType;
import com.seolytics.entity.CrawlSession;
import com.seolytics.entity.SeoIssue;
import com.seolytics.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SeoIssueRepository extends JpaRepository<SeoIssue, Long> {

    List<SeoIssue> findBySessionOrderBySeverityAscIdAsc(CrawlSession session);

    List<SeoIssue> findBySessionAndPage_Id(CrawlSession session, Long pageId);

    Optional<SeoIssue> findByIdAndSession_Owner(Long id, UserAccount owner);

    long countBySessionAndSeverity(CrawlSession session, IssueSeverity severity);

    void deleteBySession(CrawlSession session);

    @Query("""
            select i from SeoIssue i left join fetch i.page
            where i.session = :session
              and (:severity is null or i.severity = :severity)
              and (:type is null or i.issueType = :type)
              and (:query is null or lower(i.pageUrl) like lower(concat('%', :query, '%'))
                   or lower(i.title) like lower(concat('%', :query, '%')))
            order by i.severity asc, i.id asc
            """)
    List<SeoIssue> search(@Param("session") CrawlSession session,
                          @Param("severity") IssueSeverity severity,
                          @Param("type") IssueType type,
                          @Param("query") String query);

    @Query("""
            select i.issueType, count(i)
            from SeoIssue i
            where i.session.owner = :owner
            group by i.issueType
            order by count(i) desc
            """)
    List<Object[]> topIssueTypes(@Param("owner") UserAccount owner);
}
