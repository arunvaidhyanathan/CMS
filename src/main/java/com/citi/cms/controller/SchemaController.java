package com.citi.cms.controller;

import com.citi.cms.dto.response.ColumnInfo;
import com.citi.cms.dto.response.TableInfo;
import com.citi.cms.service.SchemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/schema")
@CrossOrigin(origins = "*")
public class SchemaController {

    @Autowired
    private SchemaService schemaService;

    @GetMapping("/schemas")
    public ResponseEntity<List<String>> getSchemas() {
        return ResponseEntity.ok(schemaService.getSchemas());
    }

    @GetMapping("/tables")
    public ResponseEntity<List<TableInfo>> getAllTables() {
        return ResponseEntity.ok(schemaService.getAllTables());
    }

    @GetMapping("/tables/{schemaName}")
    public ResponseEntity<List<TableInfo>> getTablesBySchema(@PathVariable String schemaName) {
        return ResponseEntity.ok(schemaService.getTablesForSchema(schemaName));
    }

    @GetMapping("/tables/{schemaName}/{tableName}/columns")
    public ResponseEntity<List<ColumnInfo>> getTableColumns(
            @PathVariable String schemaName,
            @PathVariable String tableName) {
        return ResponseEntity.ok(schemaService.getTableColumns(schemaName, tableName));
    }
}
