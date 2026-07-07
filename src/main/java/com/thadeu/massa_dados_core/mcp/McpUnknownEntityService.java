package com.thadeu.massa_dados_core.mcp;

import com.thadeu.massa_dados_core.mcp.dto.UnknownEntityRequest;
import com.thadeu.massa_dados_core.mcp.dto.UnknownEntityResponse;
import com.thadeu.massa_dados_core.mcp.dto.UnknownEntityResponse.MissingColumnInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Serviço responsável por identificar tabelas/colunas no DDL que não possuem
 * classe Entity correspondente no projeto.
 */
@Service
public class McpUnknownEntityService {

    private final Path entityClassesPath;

    public McpUnknownEntityService(@Value("${entity.classes.path}") String entityClassesPath) {
        this.entityClassesPath = Paths.get(entityClassesPath);
    }

    /**
     * Compara um script DDL com as classes Entity existentes e retorna
     * as tabelas/colunas não reconhecidas.
     *
     * @param request Dados do DDL
     * @return Lista de itens não reconhecidos
     */
    public UnknownEntityResponse identifyUnknownEntities(UnknownEntityRequest request) {
        String ddl = request.ddlScript();

        // Extrair tabelas do DDL
        List<String> ddlTables = extractTableNames(ddl);

        // Extrair colunas do DDL (agrupadas por tabela)
        var ddlColumns = extractColumnsByTable(ddl);

        // Obter classes Entity existentes
        Set<String> existingEntities = getExistingEntityNames();

        // Identificar tabelas faltantes
        List<String> missingTables = new ArrayList<>();
        for (String table : ddlTables) {
            String className = toPascalCase(table);
            if (!existingEntities.contains(className)) {
                missingTables.add(table);
            }
        }

        // Identificar colunas faltantes (apenas para tabelas que existem)
        List<MissingColumnInfo> missingColumns = new ArrayList<>();
        for (var entry : ddlColumns.entrySet()) {
            String tableName = entry.getKey();
            String className = toPascalCase(tableName);
            if (!existingEntities.contains(className)) {
                continue; // Tabela inteira faltante, já reportada acima
            }
            // Para cada coluna, verificar se existe na Entity
            Set<String> existingFields = getExistingFieldNames(className);
            for (String column : entry.getValue()) {
                String fieldName = toCamelCase(column);
                if (!existingFields.contains(fieldName)) {
                    missingColumns.add(new MissingColumnInfo(tableName, column, ""));
                }
            }
        }

        return new UnknownEntityResponse(missingTables, missingColumns);
    }

    // ========== Métodos auxiliares ==========

    private List<String> extractTableNames(String ddl) {
        List<String> tables = new ArrayList<>();
        Pattern pattern = Pattern.compile(
                "CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(?:\\w+\\.)?(\\w+)",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(ddl);
        while (matcher.find()) {
            tables.add(matcher.group(1));
        }
        return tables;
    }

    private java.util.Map<String, List<String>> extractColumnsByTable(String ddl) {
        // Implementação simplificada: extrai apenas nomes de colunas
        // Para cada CREATE TABLE, extrai o bloco de colunas
        java.util.Map<String, List<String>> result = new java.util.HashMap<>();
        Pattern tablePattern = Pattern.compile(
                "CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(?:\\w+\\.)?(\\w+)\\s*\\(([^()]*(?:\\([^()]*\\)[^()]*)*)\\)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher tableMatcher = tablePattern.matcher(ddl);
        while (tableMatcher.find()) {
            String tableName = tableMatcher.group(1);
            String columnsBlock = tableMatcher.group(2);
            List<String> columns = new ArrayList<>();
            String[] lines = columnsBlock.split("\\s*,\\s*(?![^()]*\\))");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (line.toUpperCase().startsWith("CONSTRAINT")
                        || line.toUpperCase().startsWith("PRIMARY KEY")
                        || line.toUpperCase().startsWith("FOREIGN KEY")
                        || line.toUpperCase().startsWith("INDEX")
                        || line.toUpperCase().startsWith("UNIQUE")) {
                    continue;
                }
                String[] parts = line.split("\\s+", 2);
                if (parts.length >= 1) {
                    columns.add(parts[0]);
                }
            }
            result.put(tableName, columns);
        }
        return result;
    }

    private Set<String> getExistingEntityNames() {
        try (Stream<Path> files = Files.list(entityClassesPath)) {
            return files
                    .filter(p -> p.toString().endsWith(".java"))
                    .map(p -> {
                        String fileName = p.getFileName().toString();
                        return fileName.substring(0, fileName.lastIndexOf('.'));
                    })
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            return Set.of();
        }
    }

    private Set<String> getExistingFieldNames(String className) {
        // Tenta carregar a classe via reflection para obter os campos
        try {
            Class<?> clazz = Class.forName("com.thadeu.massa_dados_core.domain." + className);
            return java.util.Arrays.stream(clazz.getDeclaredFields())
                    .map(java.lang.reflect.Field::getName)
                    .collect(Collectors.toSet());
        } catch (ClassNotFoundException e) {
            return Set.of();
        }
    }

    private String toPascalCase(String tableName) {
        String name = tableName;
        if (name.startsWith("T696")) {
            name = name.substring(4);
        }
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = true;
        for (char c : name.toCharArray()) {
            if (c == '_') {
                nextUpper = true;
            } else if (nextUpper) {
                sb.append(Character.toUpperCase(c));
                nextUpper = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    private String toCamelCase(String columnName) {
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = false;
        for (char c : columnName.toCharArray()) {
            if (c == '_') {
                nextUpper = true;
            } else if (nextUpper) {
                sb.append(Character.toUpperCase(c));
                nextUpper = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }
}
