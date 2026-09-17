package com.kbase.backend.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    @Query("""
            select distinct project
            from Project project
            join ProjectMember member on member.project = project
            where member.user.id = :userId
            order by project.createdAt desc
            """)
    List<Project> findAccessibleByUserId(@Param("userId") UUID userId);
}
