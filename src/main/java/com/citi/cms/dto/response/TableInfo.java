package com.citi.cms.dto.response;

public class TableInfo {
    private String tableName;
    private String schemaName;
    private Long rowCount;

    public TableInfo(String tableName, String schemaName, Long rowCount) {
        this.tableName = tableName;
        this.schemaName = schemaName;
        this.rowCount = rowCount;
    }

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public String getSchemaName() { return schemaName; }
    public void setSchemaName(String schemaName) { this.schemaName = schemaName; }
    public Long getRowCount() { return rowCount; }
    public void setRowCount(Long rowCount) { this.rowCount = rowCount; }
}
