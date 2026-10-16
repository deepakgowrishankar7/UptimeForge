package com.pulsewatch.repository;

import com.pulsewatch.entity.StatusPage;
import com.pulsewatch.entity.Team;
import com.pulsewatch.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StatusPageRepository extends JpaRepository<StatusPage, Long> {
    Optional<StatusPage> findBySlug(String slug);
    boolean existsBySlug(String slug);

    @Query("SELECT sp FROM StatusPage sp WHERE sp.owner = :user OR sp.team IN :teams")
    List<StatusPage> findAccessibleStatusPages(@Param("user") User user, @Param("teams") List<Team> teams);

    Optional<StatusPage> findBySlugAndPublishedTrue(String slug);
}
