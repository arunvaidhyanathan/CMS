import React, { useState, useEffect, useRef } from 'react';
import Editor from '@monaco-editor/react';
import { AgGridReact } from 'ag-grid-react';
import { AllCommunityModule, ModuleRegistry } from 'ag-grid-community';
import 'ag-grid-community/styles/ag-grid.css';
import 'ag-grid-community/styles/ag-theme-alpine.css';
import axios from 'axios';

ModuleRegistry.registerModules([AllCommunityModule]);

const API_BASE = '/api';

const COLORS = {
  bg: '#1e1e2e',
  sidebar: '#181825',
  panel: '#24273a',
  border: '#313244',
  accent: '#89b4fa',
  accentHover: '#b4befe',
  text: '#cdd6f4',
  textMuted: '#6c7086',
  success: '#a6e3a1',
  error: '#f38ba8',
  errorBg: '#2d1b1b',
  schemaTag: '#313244',
  tableHover: '#313244',
  tableActive: '#45475a',
  buttonBg: '#89b4fa',
  buttonText: '#1e1e2e',
  buttonDisabled: '#45475a',
};

export default function SchemaEditor() {
  const [schemas, setSchemas] = useState([]);
  const [selectedSchema, setSelectedSchema] = useState(null);
  const [tables, setTables] = useState({});          // { schemaName: [TableInfo] }
  const [expandedSchemas, setExpandedSchemas] = useState({});
  const [selectedTable, setSelectedTable] = useState(null);  // { schema, table }
  const [columns, setColumns] = useState([]);
  const [showColumns, setShowColumns] = useState(false);

  const [sqlQuery, setSqlQuery] = useState('-- Select a table on the left to get started\n');
  const [queryResults, setQueryResults] = useState([]);
  const [columnDefs, setColumnDefs] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const [executionTime, setExecutionTime] = useState(null);
  const [rowCount, setRowCount] = useState(null);

  const gridRef = useRef();

  useEffect(() => {
    fetchSchemas();
  }, []);

  const fetchSchemas = async () => {
    try {
      const resp = await axios.get(`${API_BASE}/schema/schemas`);
      setSchemas(resp.data);
      // Auto-expand first schema
      if (resp.data.length > 0) {
        fetchTablesForSchema(resp.data[0]);
        setExpandedSchemas({ [resp.data[0]]: true });
      }
    } catch (err) {
      setError('Failed to load schemas: ' + (err.response?.data?.message || err.message));
    }
  };

  const fetchTablesForSchema = async (schemaName) => {
    if (tables[schemaName]) return; // already loaded
    try {
      const resp = await axios.get(`${API_BASE}/schema/tables/${schemaName}`);
      setTables(prev => ({ ...prev, [schemaName]: resp.data }));
    } catch (err) {
      setError('Failed to load tables for ' + schemaName + ': ' + (err.response?.data?.message || err.message));
    }
  };

  const toggleSchema = (schemaName) => {
    const isExpanded = expandedSchemas[schemaName];
    setExpandedSchemas(prev => ({ ...prev, [schemaName]: !isExpanded }));
    if (!isExpanded) {
      fetchTablesForSchema(schemaName);
    }
  };

  const handleTableClick = async (schemaName, tableName) => {
    setSelectedTable({ schema: schemaName, table: tableName });
    setSqlQuery(`SELECT *\nFROM "${schemaName}"."${tableName}"\nLIMIT 100;`);
    setShowColumns(false);

    try {
      const resp = await axios.get(`${API_BASE}/schema/tables/${schemaName}/${tableName}/columns`);
      setColumns(resp.data);
    } catch (err) {
      setColumns([]);
    }
  };

  const toggleColumns = () => setShowColumns(v => !v);

  const executeQuery = async () => {
    if (!sqlQuery.trim() || sqlQuery.startsWith('--')) {
      setError('Please enter a SQL query');
      return;
    }
    setIsLoading(true);
    setError(null);
    setQueryResults([]);
    setColumnDefs([]);
    const t0 = Date.now();

    try {
      const resp = await axios.post(`${API_BASE}/query/execute`, {
        query: sqlQuery,
        schema: selectedTable?.schema || null,
      });

      setExecutionTime(Date.now() - t0);
      const data = resp.data;

      if (data && data.length > 0) {
        const cols = Object.keys(data[0]).map(key => ({
          field: key,
          headerName: key,
          sortable: true,
          filter: true,
          resizable: true,
          minWidth: 100,
          tooltipField: key,
          valueFormatter: (p) => {
            if (p.value === null || p.value === undefined) return 'NULL';
            if (typeof p.value === 'object') return JSON.stringify(p.value);
            return p.value;
          },
        }));
        setColumnDefs(cols);
        setQueryResults(data);
        setRowCount(data.length);
      } else {
        setRowCount(0);
        setError('Query returned no rows');
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Query failed: ' + err.message);
      setExecutionTime(Date.now() - t0);
    } finally {
      setIsLoading(false);
    }
  };

  const handleKeyDown = (e) => {
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
      executeQuery();
    }
  };

  return (
    <div style={{ display: 'flex', height: '100vh', width: '100vw', background: COLORS.bg, color: COLORS.text, fontFamily: 'inherit' }}>
      {/* ── LEFT SIDEBAR ── */}
      <div style={{ width: 260, minWidth: 200, maxWidth: 320, background: COLORS.sidebar, borderRight: `1px solid ${COLORS.border}`, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
        <div style={{ padding: '16px 14px 10px', borderBottom: `1px solid ${COLORS.border}` }}>
          <div style={{ fontSize: 13, fontWeight: 700, color: COLORS.accent, letterSpacing: 1, textTransform: 'uppercase' }}>CMS Schema Editor</div>
          <div style={{ fontSize: 11, color: COLORS.textMuted, marginTop: 2 }}>Neon · workflow DB</div>
        </div>

        <div style={{ flex: 1, overflowY: 'auto', padding: '8px 0' }}>
          {schemas.length === 0 && (
            <div style={{ padding: '12px 14px', color: COLORS.textMuted, fontSize: 12 }}>Loading schemas…</div>
          )}
          {schemas.map(schema => (
            <div key={schema}>
              {/* Schema header */}
              <div
                onClick={() => toggleSchema(schema)}
                style={{ display: 'flex', alignItems: 'center', padding: '7px 14px', cursor: 'pointer', userSelect: 'none', gap: 6 }}
                onMouseEnter={e => e.currentTarget.style.background = COLORS.tableHover}
                onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
              >
                <span style={{ fontSize: 10, color: COLORS.textMuted, width: 10 }}>
                  {expandedSchemas[schema] ? '▼' : '▶'}
                </span>
                <span style={{ fontSize: 11, background: COLORS.schemaTag, color: COLORS.accent, padding: '1px 6px', borderRadius: 3, fontWeight: 600, letterSpacing: 0.5 }}>
                  {schema}
                </span>
              </div>

              {/* Tables */}
              {expandedSchemas[schema] && (
                <div>
                  {!tables[schema] && (
                    <div style={{ padding: '4px 14px 4px 28px', fontSize: 11, color: COLORS.textMuted }}>Loading…</div>
                  )}
                  {(tables[schema] || []).map(t => (
                    <div
                      key={t.tableName}
                      onClick={() => handleTableClick(schema, t.tableName)}
                      style={{
                        padding: '5px 14px 5px 30px',
                        cursor: 'pointer',
                        fontSize: 12,
                        background: selectedTable?.schema === schema && selectedTable?.table === t.tableName
                          ? COLORS.tableActive : 'transparent',
                        borderLeft: selectedTable?.schema === schema && selectedTable?.table === t.tableName
                          ? `2px solid ${COLORS.accent}` : '2px solid transparent',
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                      }}
                      onMouseEnter={e => { if (selectedTable?.table !== t.tableName) e.currentTarget.style.background = COLORS.tableHover; }}
                      onMouseLeave={e => { if (selectedTable?.table !== t.tableName || selectedTable?.schema !== schema) e.currentTarget.style.background = 'transparent'; }}
                    >
                      <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        <span style={{ marginRight: 5, opacity: 0.5 }}>⊞</span>{t.tableName}
                      </span>
                      <span style={{ fontSize: 10, color: COLORS.textMuted, flexShrink: 0, marginLeft: 4 }}>
                        {t.rowCount.toLocaleString()}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>
      </div>

      {/* ── MAIN AREA ── */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>

        {/* ── SQL EDITOR PANEL ── */}
        <div style={{ height: 300, minHeight: 200, borderBottom: `1px solid ${COLORS.border}`, display: 'flex', flexDirection: 'column' }}>
          {/* Toolbar */}
          <div style={{ height: 44, background: COLORS.panel, borderBottom: `1px solid ${COLORS.border}`, display: 'flex', alignItems: 'center', padding: '0 16px', gap: 12, flexShrink: 0 }}>
            <span style={{ fontWeight: 600, fontSize: 13, color: COLORS.text }}>SQL Query</span>
            {selectedTable && (
              <span style={{ fontSize: 11, color: COLORS.textMuted, background: COLORS.schemaTag, padding: '2px 8px', borderRadius: 3 }}>
                {selectedTable.schema}.{selectedTable.table}
              </span>
            )}
            <div style={{ flex: 1 }} />
            <span style={{ fontSize: 11, color: COLORS.textMuted }}>Ctrl+Enter to run</span>
            {columns.length > 0 && (
              <button
                onClick={toggleColumns}
                style={{ padding: '4px 10px', fontSize: 11, background: COLORS.schemaTag, color: COLORS.text, border: `1px solid ${COLORS.border}`, borderRadius: 4, cursor: 'pointer' }}
              >
                {showColumns ? 'Hide Columns' : `Columns (${columns.length})`}
              </button>
            )}
            <button
              onClick={executeQuery}
              disabled={isLoading}
              style={{
                padding: '6px 18px',
                fontSize: 13,
                fontWeight: 600,
                background: isLoading ? COLORS.buttonDisabled : COLORS.buttonBg,
                color: isLoading ? COLORS.textMuted : COLORS.buttonText,
                border: 'none',
                borderRadius: 5,
                cursor: isLoading ? 'not-allowed' : 'pointer',
                transition: 'background 0.15s',
              }}
            >
              {isLoading ? 'Running…' : '▶ Run'}
            </button>
          </div>

          {/* Column info bar */}
          {showColumns && columns.length > 0 && (
            <div style={{ background: COLORS.panel, borderBottom: `1px solid ${COLORS.border}`, padding: '6px 16px', display: 'flex', flexWrap: 'wrap', gap: 6, maxHeight: 80, overflowY: 'auto', flexShrink: 0 }}>
              {columns.map(col => (
                <span key={col.columnName} style={{ fontSize: 11, background: COLORS.schemaTag, padding: '2px 7px', borderRadius: 3, color: COLORS.text, fontFamily: 'monospace' }}>
                  <span style={{ color: COLORS.accent }}>{col.columnName}</span>
                  <span style={{ color: COLORS.textMuted }}> {col.dataType}</span>
                  {!col.isNullable && <span style={{ color: COLORS.error, marginLeft: 3 }}>*</span>}
                </span>
              ))}
            </div>
          )}

          {/* Monaco editor */}
          <div style={{ flex: 1, overflow: 'hidden' }} onKeyDown={handleKeyDown}>
            <Editor
              height="100%"
              defaultLanguage="sql"
              value={sqlQuery}
              onChange={v => setSqlQuery(v || '')}
              theme="vs-dark"
              options={{
                minimap: { enabled: false },
                fontSize: 14,
                lineNumbers: 'on',
                scrollBeyondLastLine: false,
                automaticLayout: true,
                tabSize: 2,
                wordWrap: 'on',
                padding: { top: 10 },
              }}
            />
          </div>
        </div>

        {/* ── RESULTS PANEL ── */}
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
          {/* Results toolbar */}
          <div style={{ height: 40, background: COLORS.panel, borderBottom: `1px solid ${COLORS.border}`, display: 'flex', alignItems: 'center', padding: '0 16px', gap: 12, flexShrink: 0 }}>
            <span style={{ fontWeight: 600, fontSize: 13 }}>Results</span>
            {rowCount !== null && !error && (
              <span style={{ fontSize: 11, color: COLORS.success }}>
                {rowCount.toLocaleString()} row{rowCount !== 1 ? 's' : ''}
              </span>
            )}
            {executionTime !== null && (
              <span style={{ fontSize: 11, color: COLORS.textMuted }}>{executionTime}ms</span>
            )}
          </div>

          {/* Error bar */}
          {error && (
            <div style={{ padding: '8px 16px', background: COLORS.errorBg, color: COLORS.error, fontSize: 12, borderBottom: `1px solid ${COLORS.error}`, flexShrink: 0 }}>
              {error}
            </div>
          )}

          {/* AG Grid */}
          <div className="ag-theme-alpine-dark" style={{ flex: 1, overflow: 'hidden' }}>
            <AgGridReact
              ref={gridRef}
              rowData={queryResults}
              columnDefs={columnDefs}
              defaultColDef={{
                sortable: true,
                filter: true,
                resizable: true,
                minWidth: 80,
                flex: 1,
              }}
              pagination={true}
              paginationPageSize={100}
              domLayout="normal"
              style={{ height: '100%', width: '100%' }}
              rowSelection="multiple"
              enableCellTextSelection={true}
              copyHeadersToClipboard={true}
              tooltipShowDelay={500}
            />
          </div>
        </div>
      </div>
    </div>
  );
}
