package com.kshitiz.taskforge.user.adapter.persistence;

import com.kshitiz.taskforge.common.adapter.persistence.PageRequests;
import com.kshitiz.taskforge.common.domain.PageQuery;
import com.kshitiz.taskforge.common.domain.PageResult;
import com.kshitiz.taskforge.user.domain.model.User;
import com.kshitiz.taskforge.user.domain.model.UserSearchCriteria;
import com.kshitiz.taskforge.user.domain.port.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Database implementation of the {@link UserRepository} port, using Spring Data JPA.
 */
@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserRepository {

    /** Sort names the API accepts, mapped to entity properties. */
    private static final Map<String, String> SORTABLE_FIELDS = Map.of(
            "fullName", "fullName",
            "email", "email",
            "createdAt", "createdAt");

    private final UserJpaRepository jpaRepository;
    private final UserPersistenceMapper mapper;

    /**
     * {@inheritDoc}
     *
     * <p>Flushes immediately so the returned user has its final timestamps and version.
     */
    @Override
    public User save(User user) {
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(user)));
    }

    /** {@inheritDoc} */
    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    /** {@inheritDoc} */
    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    /** {@inheritDoc} */
    @Override
    public PageResult<User> search(UserSearchCriteria criteria, PageQuery pageQuery) {
        Specification<UserJpaEntity> spec = Specification.allOf(
                UserSpecifications.nameContains(criteria.name()),
                UserSpecifications.hasRole(criteria.role()));
        return PageRequests.toPageResult(
                jpaRepository.findAll(spec, PageRequests.toPageable(pageQuery, SORTABLE_FIELDS)),
                mapper::toDomain);
    }

    /** {@inheritDoc} */
    @Override
    public List<User> findByManagerId(UUID managerId) {
        return jpaRepository.findByManagerIdOrderByFullNameAsc(managerId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    /** {@inheritDoc} */
    @Override
    public void clearManager(UUID managerId) {
        jpaRepository.clearManager(managerId);
    }

    /** {@inheritDoc} */
    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
