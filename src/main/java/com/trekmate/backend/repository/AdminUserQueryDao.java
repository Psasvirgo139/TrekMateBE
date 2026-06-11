package com.trekmate.backend.repository;

import com.trekmate.backend.dto.projection.AdminUserRow;
import com.trekmate.backend.dto.response.AdminUserStatsResponse;
import com.trekmate.backend.model.enums.UserAccountStatus;
import com.trekmate.backend.model.enums.UserRoleFilter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Truy van user bang native SQL 1 lan — JOIN customers + guides, co phan trang.
 */
@Repository
public class AdminUserQueryDao {

    private static final String BASE_FROM = """
            FROM users u
            LEFT JOIN customers c ON c.user_id = u.id
            LEFT JOIN guides g ON g.user_id = u.id
            """;

    @PersistenceContext
    private EntityManager em;

    public Page<AdminUserRow> findUsers(
            UserRoleFilter role,
            UserAccountStatus status,
            String search,
            Pageable pageable) {

        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        Map<String, Object> params = new HashMap<>();

        appendRoleFilter(where, params, role);
        appendStatusFilter(where, params, status);
        appendSearchFilter(where, params, search);

        String select = """
                SELECT u.id, u.email, u.phone, u.is_admin, u.is_active, u.is_verified,
                       u.last_login_at, u.updated_at,
                       c.full_name, c.avatar_url,
                       g.display_name, g.avatar_url, g.experience_years, g.profile_approved_at,
                       (c.id IS NOT NULL), (g.id IS NOT NULL)
                """ + BASE_FROM + where + " ORDER BY u.updated_at DESC";

        Query dataQuery = em.createNativeQuery(select);
        params.forEach(dataQuery::setParameter);
        dataQuery.setFirstResult((int) pageable.getOffset());
        dataQuery.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = dataQuery.getResultList();
        List<AdminUserRow> content = rows.stream().map(this::mapRow).toList();

        String countSql = "SELECT COUNT(DISTINCT u.id) " + BASE_FROM + where;
        Query countQuery = em.createNativeQuery(countSql);
        params.forEach(countQuery::setParameter);
        long total = ((Number) countQuery.getSingleResult()).longValue();

        return new PageImpl<>(content, pageable, total);
    }

    public AdminUserStatsResponse fetchStats() {
        long totalGuides = count("SELECT COUNT(*) FROM guides");
        long activeUsers = countUsersByStatus(UserAccountStatus.ACTIVE);
        long pending = countUsersByStatus(UserAccountStatus.PENDING);
        return new AdminUserStatsResponse(totalGuides, activeUsers, pending);
    }

    public Optional<AdminUserRow> findById(UUID id) {
        String sql = """
                SELECT u.id, u.email, u.phone, u.is_admin, u.is_active, u.is_verified,
                       u.last_login_at, u.updated_at,
                       c.full_name, c.avatar_url,
                       g.display_name, g.avatar_url, g.experience_years, g.profile_approved_at,
                       (c.id IS NOT NULL), (g.id IS NOT NULL)
                """ + BASE_FROM + " WHERE u.id = :id";

        Query query = em.createNativeQuery(sql);
        query.setParameter("id", id);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        if (rows.isEmpty()) return Optional.empty();
        return Optional.of(mapRow(rows.get(0)));
    }

    private long countUsersByStatus(UserAccountStatus status) {
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        Map<String, Object> params = new HashMap<>();
        appendStatusFilter(where, params, status);
        String sql = "SELECT COUNT(DISTINCT u.id) " + BASE_FROM + where;
        Query query = em.createNativeQuery(sql);
        params.forEach(query::setParameter);
        return ((Number) query.getSingleResult()).longValue();
    }

    private long count(String sql) {
        return ((Number) em.createNativeQuery(sql).getSingleResult()).longValue();
    }

    private void appendRoleFilter(StringBuilder where, Map<String, Object> params, UserRoleFilter role) {
        if (role == null || role == UserRoleFilter.ALL) return;
        switch (role) {
            case ADMIN -> where.append(" AND u.is_admin = true ");
            case GUIDE -> where.append(" AND g.id IS NOT NULL ");
            case CUSTOMER -> where.append(" AND c.id IS NOT NULL AND u.is_admin = false ");
            default -> { }
        }
    }

    private void appendStatusFilter(StringBuilder where, Map<String, Object> params, UserAccountStatus status) {
        if (status == null) return;
        switch (status) {
            case BANNED -> where.append(" AND u.is_active = false ");
            case PENDING -> where.append("""
                     AND u.is_active = true
                     AND (u.is_verified = false OR (g.id IS NOT NULL AND g.profile_approved_at IS NULL))
                    """);
            case ACTIVE -> where.append("""
                     AND u.is_active = true
                     AND u.is_verified = true
                     AND (g.id IS NULL OR g.profile_approved_at IS NOT NULL)
                    """);
            default -> { }
        }
    }

    private void appendSearchFilter(StringBuilder where, Map<String, Object> params, String search) {
        if (!StringUtils.hasText(search)) return;
        where.append("""
                 AND (
                   LOWER(u.email) LIKE LOWER(:search)
                   OR LOWER(COALESCE(c.full_name, '')) LIKE LOWER(:search)
                   OR LOWER(COALESCE(g.display_name, '')) LIKE LOWER(:search)
                 )
                """);
        params.put("search", "%" + search.trim() + "%");
    }

    private AdminUserRow mapRow(Object[] r) {
        return new AdminUserRow(
                UUID.fromString(r[0].toString()),
                (String) r[1],
                (String) r[2],
                toBoolean(r[3]),
                toBoolean(r[4]),
                toBoolean(r[5]),
                toLocalDateTime(r[6]),
                toLocalDateTime(r[7]),
                (String) r[8],
                (String) r[9],
                (String) r[10],
                (String) r[11],
                r[12] != null ? ((Number) r[12]).shortValue() : null,
                toLocalDateTime(r[13]),
                toBoolean(r[14]),
                toBoolean(r[15])
        );
    }

    private Boolean toBoolean(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean b) return b;
        return Boolean.parseBoolean(value.toString());
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDateTime ldt) return ldt;
        if (value instanceof Timestamp ts) return ts.toLocalDateTime();
        return null;
    }
}
