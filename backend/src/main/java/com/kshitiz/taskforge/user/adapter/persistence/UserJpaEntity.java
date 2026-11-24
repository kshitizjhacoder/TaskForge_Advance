package com.kshitiz.taskforge.user.adapter.persistence;

import com.kshitiz.taskforge.common.adapter.persistence.BaseJpaEntity;
import com.kshitiz.taskforge.user.domain.model.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * Database row of the {@code users} table.
 *
 * <p>{@code @SoftDelete} makes Hibernate turn a delete into
 * {@code UPDATE users SET deleted = true} and hide deleted rows from every query.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
@SoftDelete(columnName = "deleted")
public class UserJpaEntity extends BaseJpaEntity {

    @Column(name = "full_name", length = 100, nullable = false)
    private String fullName;

    @Column(name = "email", length = 150, nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", length = 100, nullable = false)
    private String passwordHash;

    /** Stored as its code (e.g. "ADMIN"), which references {@code master_role.code}. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "role_code", length = 30, nullable = false)
    private Role role;

    @Column(name = "manager_id")
    private UUID managerId;
}
