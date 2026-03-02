package com.citi.cms.dto.response;

public class ColumnInfo {
    private String columnName;
    private String dataType;
    private Boolean isNullable;
    private String columnDefault;

    public ColumnInfo(String columnName, String dataType, Boolean isNullable, String columnDefault) {
        this.columnName = columnName;
        this.dataType = dataType;
        this.isNullable = isNullable;
        this.columnDefault = columnDefault;
    }

    public String getColumnName() { return columnName; }
    public void setColumnName(String columnName) { this.columnName = columnName; }
    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }
    public Boolean getIsNullable() { return isNullable; }
    public void setIsNullable(Boolean isNullable) { this.isNullable = isNullable; }
    public String getColumnDefault() { return columnDefault; }
    public void setColumnDefault(String columnDefault) { this.columnDefault = columnDefault; }
}
