package com.kshitiz.taskforge.project.adapter.persistence;

import com.kshitiz.taskforge.project.domain.model.Project;
import com.kshitiz.taskforge.project.domain.model.ProjectMember;
import org.mapstruct.Mapper;

/**
 * Converts the project feature's domain objects to and from their JPA entities. MapStruct
 * generates the implementation at compile time; fields are matched by name.
 */
@Mapper
public interface ProjectPersistenceMapper {

    /**
     * Rebuilds a domain project from a database row.
     *
     * @param entity database row
     * @return domain project
     */
    Project toDomain(ProjectJpaEntity entity);

    /**
     * Copies a domain project into a database row.
     *
     * @param project domain project
     * @return database row
     */
    ProjectJpaEntity toEntity(Project project);

    /**
     * Rebuilds a domain membership from a database row.
     *
     * @param entity database row
     * @return domain membership
     */
    ProjectMember toDomain(ProjectMemberJpaEntity entity);

    /**
     * Copies a domain membership into a database row.
     *
     * @param member domain membership
     * @return database row
     */
    ProjectMemberJpaEntity toEntity(ProjectMember member);
}
