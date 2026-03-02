package com.citi.cms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSetMetaData;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class QueryService {

    private static final Logger logger = LoggerFactory.getLogger(QueryService.class);

    private static final List<String> ALLOWED_SCHEMAS =
            Arrays.asList("cms_workflow", "cms_flowable_workflow", "entitlements");

    private static final String[] DANGEROUS_KEYWORDS = {
        "DROP", "DELETE", "TRUNCATE", "ALTER", "CREATE", "INSERT", "UPDATE",
        "GRANT", "REVOKE", "EXECUTE", "EXEC", "XP_", "SP_", "--", "/*"
    };

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> executeQuery(String query, String schemaName) {
        validateQuery(query);

        if (schemaName != null && !schemaName.isBlank()) {
            if (!ALLOWED_SCHEMAS.contains(schemaName)) {
                throw new IllegalArgumentException("Schema not allowed: " + schemaName);
            }
            jdbcTemplate.execute("SET search_path TO \"" + schemaName + "\"");
        }

        logger.info("Executing query on schema '{}': {}", schemaName, query.substring(0, Math.min(query.length(), 100)));

        return jdbcTemplate.query(query, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            for (int i = 1; i <= cols; i++) {
                row.put(meta.getColumnLabel(i), rs.getObject(i));
            }
            return row;
        });
    }

    private void validateQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be empty");
        }

        String upper = query.trim().toUpperCase();

        if (!upper.startsWith("SELECT")) {
            throw new IllegalArgumentException("Only SELECT statements are allowed");
        }

        for (String keyword : DANGEROUS_KEYWORDS) {
            if (upper.contains(keyword)) {
                throw new IllegalArgumentException("Query contains disallowed keyword: " + keyword);
            }
        }
    }
}
