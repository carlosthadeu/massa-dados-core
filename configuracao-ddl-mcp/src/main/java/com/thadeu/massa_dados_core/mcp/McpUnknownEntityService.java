package com.thadeu.massa_dados_core.mcp;

import com.thadeu.massa_dados_core.mcp.dto.MissingColumnInfo;
import com.thadeu.massa_dados_core.mcp.dto.UnknownEntityRequest;
import com.thadeu.massa_dados_core.mcp.dto.UnknownEntityResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class McpUnknownEntityService {

    @Value("${entity.classes.path}")
    private String entityClassesPath;

    public UnknownEntityResponse identify(UnknownEntityRequest request) throws IOException {
        String ddl = request.ddlScript();

        // Extrair tabelas do DDL
        List<String> ddlTables = extractTableNames(ddl);
        List<TableColumnInfo> ddlColumns = extractAllColumns(ddl);

        // Obter tabelas existentes das classes Entity
        List<String> existingTables = getExistingTables();

        // Identificar tabelas faltantes
        List<String> missingTables = ddlTables.stream()
                .filter(t -> !existingTables.contains(t))
                .collect(Collectors.toList());

        // Identificar colunas faltantes
        List<MissingColumnInfo> missingColumns = new ArrayList<>();
        for (TableColumnInfo tableCol : ddlColumns) {
            if (existingTables.contains(tableCol.tableName())) {
                // Verificar colunas existentes na Entity
                List<String> existingColumns = getExistingColumns(tableCol.tableName());
                for (String colName : tableCol.columns()) {
                    if (!existingColumns.contains(colName)) {
                        missingColumns.add(new MissingColumnInfo(
                                tableCol.tableName(),
                                colName,
                                "String", // tipo genérico
                                true
                        ));
                    }
                }
            }
        }

        return new UnknownEntityResponse(missingTables, missingColumns);
    }

    private List<String> extractTableNames(String ddl) {
        List<String> tables = new ArrayList<>();
        Pattern pattern = Pattern.compile("CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(?:\\w+\\.)?(\\w+)",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(ddl);
        while (matcher.find()) {
            tables.add(matcher.group(1));
        }
        return tables;
    }

    private List<TableColumnInfo> extractAllColumns(String ddl) {
        List<TableColumnInfo> result = new ArrayList<>();
        // Dividir por CREATE TABLE
        String[] statements = ddl.split("(?i)CREATE\\s+TABLE");
        for (String stmt : statements) {
            stmt = stmt.trim();
            if (stmt.isEmpty()) continue;

            // Extrair nome da tabela
            Pattern tablePattern = Pattern.compile("(?:IF\\s+NOT\\s+EXISTS\\s+)?(?:\\w+\\.)?(\\w+)\\s*\\(");
            Matcher tableMatcher = tablePattern.matcher(stmt);
            if (!tableMatcher.find()) continue;
            String tableName = tableMatcher.group(1);

            // Extrair colunas
            List<String> columns = new ArrayList<>();
            int start = stmt.indexOf('(');
            int end = stmt.lastIndexOf(')');
            if (start < 0 || end < 0) continue;

            String body = stmt.substring(start + 1, end);
            String[] lines = body.split(",\\s*");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.toUpperCase().startsWith("CONSTRAINT") || line.toUpperCase().startsWith("PRIMARY KEY")) {
                    continue;
                }
                String[] parts = line.split("\\s+", 2);
                if (parts.length >= 1) {
                    columns.add(parts[0]);
                }
            }

            result.add(new TableColumnInfo(tableName, columns));
        }
        return result;
    }

    private List<String> getExistingTables() throws IOException {
        Path entityDir = Paths.get(entityClassesPath, "br", "gov", "bnb", "domain", "entity");
        if (!Files.exists(entityDir)) return List.of();

        try (Stream<Path> files = Files.list(entityDir)) {
            return files
                    .filter(p -> p.toString().endsWith(".java"))
                    .map(this::extractTableNameFromFile)
                    .filter(name -> name != null)
                    .collect(Collectors.toList());
        }
    }

    private String extractTableNameFromFile(Path filePath) {
        try {
            String content = Files.readString(filePath);
            Pattern pattern = Pattern.compile("@Table\\(name\\s*=\\s*\"(\\w+)\"\\)");
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (IOException ignored) {}
        return null;
    }

    private List<String> getExistingColumns(String tableName) {
        Path entityDir = Paths.get(entityClassesPath, "br", "gov", "bnb", "domain", "entity");
        if (!Files.exists(entityDir)) return List.of();

        try (Stream<Path> files = Files.list(entityDir)) {
            return files
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> {
                        try {
                            String content = Files.readString(p);
                            return content.contains("@Table(name=\"" + tableName + "\")");
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .flatMap(p -> {
                        try {
                            String content = Files.readString(p);
                            return extractColumnNames(content).stream();
                        } catch (IOException e) {
                            return Stream.empty();
                        }
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return List.of();
        }
    }

    private List<String> extractColumnNames(String javaContent) {
        List<String> columns = new ArrayList<>();
        Pattern pattern = Pattern.compile("@Column\\(name\\s*=\\s*\"(\\w+)\"\\)");
        Matcher matcher = pattern.matcher(javaContent);
        while (matcher.find()) {
            columns.add(matcher.group(1));
        }
        return columns;
    }

    record TableColumnInfo(String tableName, List<String> columns) {}
}
