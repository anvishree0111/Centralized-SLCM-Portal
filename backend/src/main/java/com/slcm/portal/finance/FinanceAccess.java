package com.slcm.portal.finance;

import java.sql.PreparedStatement;
import java.sql.Statement;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
class FinanceAccess {
    final JdbcTemplate db;
    FinanceAccess(JdbcTemplate db) { this.db = db; }

    long user() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated() || "anonymousUser".equals(a.getPrincipal()))
            throw error(HttpStatus.UNAUTHORIZED, "Authentication required");
        var ids = db.queryForList("SELECT id FROM users WHERE username=? AND enabled=TRUE", Long.class, a.getName());
        if (ids.isEmpty()) throw error(HttpStatus.UNAUTHORIZED, "Authentication required");
        return ids.getFirst();
    }

    long student() {
        var ids = db.queryForList("SELECT id FROM students WHERE user_id=?", Long.class, user());
        if (ids.isEmpty()) throw error(HttpStatus.NOT_FOUND, "Student profile not found");
        return ids.getFirst();
    }

    long insert(String sql, Object... args) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        try {
            db.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                for (int i=0; i<args.length; i++) statement.setObject(i+1, args[i]);
                return statement;
            }, keys);
            return keys.getKey().longValue();
        } catch (DataIntegrityViolationException e) {
            throw error(HttpStatus.CONFLICT, "Duplicate value or invalid relationship");
        }
    }

    void update(String sql, Object... args) {
        try {
            if (db.update(sql, args) == 0) throw error(HttpStatus.NOT_FOUND, "Record not found");
        } catch (DataIntegrityViolationException e) {
            throw error(HttpStatus.CONFLICT, "Duplicate value or referenced record");
        }
    }

    void audit(String action, String entity, long id) {
        db.update("INSERT INTO audit_logs(actor_user_id,action,entity_type,entity_id) VALUES (?,?,?,?)",
                user(), action, entity, id);
    }

    static ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
