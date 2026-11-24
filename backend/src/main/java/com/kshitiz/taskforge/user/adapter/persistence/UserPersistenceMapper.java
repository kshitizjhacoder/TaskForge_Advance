package com.kshitiz.taskforge.user.adapter.persistence;

import com.kshitiz.taskforge.user.domain.model.User;
import org.mapstruct.Mapper;

/**
 * Converts between the domain {@link User} and the database {@link UserJpaEntity}.
 * MapStruct generates the code at compile time; fields are matched by name.
 */
@Mapper
public interface UserPersistenceMapper {

    /**
     * Rebuilds a domain user from a database row.
     *
     * @param entity database row
     * @return domain user
     */
    User toDomain(UserJpaEntity entity);

    /**
     * Copies a domain user into a database row ready to be saved.
     *
     * @param user domain user
     * @return database row
     */
    UserJpaEntity toEntity(User user);
}
