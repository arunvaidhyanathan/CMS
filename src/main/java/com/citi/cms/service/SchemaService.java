package com.citi.cms.service;

import com.citi.cms.dto.response.ColumnInfo;
import com.citi.cms.dto.response.TableInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class SchemaService {

    private static final Logger logger = LoggerFactory.getLogger(SchemaService.class);

    private static final List<String> ALLOWED_SCHEMAS =
            Arrays.asList("cms_workflow", "cms_flowable_workflow", "entitlements");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<String> getSchemas() {
        String sql = """
                SELECT schema_name
                FROM information_schema.schemata
                WHERE schema_name = ANY(?)
                ORDER BY schema_name
                """;
        return jdbcTemplate.queryForList(sql, String.class,
                new Object[]{ALLOWED_SCHEMAS.toArray(new String[0])});
    }

    public List<TableInfo> getTablesForSchema(String schemaName) {
        if (!ALLOWED_SCHEMAS.contains(schemaName)) {
            throw new IllegalArgumentException("Schema not allowed: " + schemaName);
        }

        String sql = """
                SELECT t.table_name, t.table_schema
                FROM information_schema.tables t
                WHERE t.table_schema = ?
                AND t.table_type = 'BASE TABLE'
                ORDER BY t.table_name
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            String tableName = rs.getString("table_name");
            String schema = rs.getString("table_schema");
            Long rowCount = getTableRowCount(schemaName, tableName);
            return new TableInfo(tableName, schema, rowCount);
        }, schemaName);
    }

    public List<TableInfo> getAllTables() {
        String sql = """
                SELECT t.table_name, t.table_schema
                FROM information_schema.tables t
                WHERE t.table_schema = ANY(?)
                AND t.table_type = 'BASE TABLE'
                ORDER BY t.table_schema, t.table_name
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            String tableName = rs.getString("table_name");
            String schema = rs.getString("table_schema");
            Long rowCount = getTableRowCount(schema, tableName);
            return new TableInfo(tableName, schema, rowCount);
        }, new Object[]{ALLOWED_SCHEMAS.toArray(new String[0])});
    }

    private Long getTableRowCount(String schemaName, String tableName) {
        try {
            String countSql = "SELECT COUNT(*) FROM \"" + schemaName + "\".\"" + tableName + "\"";
            Long count = jdbcTemplate.queryForObject(countSql, Long.class);
            return count != null ? count : 0L;
        } catch (Exception e) {
            logger.warn("Could not get row count for {}.{}: {}", schemaName, tableName, e.getMessage());
            return 0L;
        }
    }

    public List<ColumnInfo> getTableColumns(String schemaName, String tableName) {
        if (!ALLOWED_SCHEMAS.contains(schemaName)) {
            throw new IllegalArgumentException("Schema not allowed: " + schemaName);
        }

        String sql = """
                SELECT column_name, data_type, is_nullable, column_default
                FROM information_schema.columns
                WHERE table_schema = ?
                AND table_name = ?
                ORDER BY ordinal_position
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new ColumnInfo(
                        rs.getString("column_name"),
                        rs.getString("data_type"),
                        "YES".equals(rs.getString("is_nullable")),
                        rs.getString("column_default")
                ), schemaName, tableName);
    }
}
