package com.kshitiz.taskforge.user.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data repository for {@link UserJpaEntity}. Spring generates the implementation
 * from the method names. {@link JpaSpecificationExecutor} adds dynamic filtering used by
 * user search.
 */
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID>,
        JpaSpecificationExecutor<UserJpaEntity> {

    /**
     * Checks whether an email is already used.
     *
     * @param email normalised email
     * @return {@code true} if a (non-deleted) user has this email
     */
    boolean existsByEmail(String email);

    /**
     * Lists the users who report to a manager.
     *
     * @param managerId the manager's ID
     * @return direct reports sorted by name
     */
    List<UserJpaEntity> findByManagerIdOrderByFullNameAsc(UUID managerId);

    /**
     * Sets {@code manager_id} to null for everyone reporting to {@code managerId}, in one
     * UPDATE statement.
     *
     * @param managerId the manager's ID
     * @return number of users changed
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update UserJpaEntity u set u.managerId = null where u.managerId = :managerId")
    int clearManager(@Param("managerId") UUID managerId);
}
