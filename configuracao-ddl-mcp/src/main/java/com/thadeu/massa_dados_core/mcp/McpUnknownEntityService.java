package com.thadeu.massa_dados_core.mcp;

import com.thadeu.massa_dados_core.mcp.dto.MissingColumnInfo;
import com.thadeu.massa_dados_core.mcp.dto.UnknownEntityRequest;
import com.thadeu.massa_dados_core.mcp.dto.UnknownEntityResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Serviço responsável por identificar tabelas e colunas não reconhecidas
 * comparando um script DDL com as classes Entity existentes.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Extrair tabelas e colunas de um script DDL</li>
 *   <li>Comparar com as classes Entity existentes no diretório de entidades</li>
 *   <li>Retornar lista de tabelas e colunas não reconhecidas</li>
 * </ul>
 *
 * @author Thadeu Garrido
 * @version 1.0
 */
@Service
public class McpUnknownEntityService {

    private static final Logger log = LoggerFactory.getLogger(McpUnknownEntityService.class);

    @Value("${entity.classes.path}")
    private String entityClassesPath;

    /**
     * Identifica tabelas e colunas não reconhecidas comparando o DDL com as Entity existentes.
     *
     * @param request requisição contendo o script DDL
     * @return resposta com listas de tabelas e colunas faltantes
     * @throws IOException se houver erro ao ler os arquivos de entidades
     */
    public UnknownEntityResponse identify(UnknownEntityRequest request) throws IOException {
        log.info("[identify] Iniciando identificação de entidades desconhecidas");
        String ddl = request.ddlScript();

        List<String> ddlTables = extractTableNames(ddl);
        log.debug("[identify] Tabelas extraídas do DDL: {}", ddlTables);
        List<TableColumnInfo> ddlColumns = extractAllColumns(ddl);
        log.debug("[identify] Colunas extraídas do DDL: {}", ddlColumns.size());

        List<String> existingTables = getExistingTables();
        log.debug("[identify] Tabelas existentes: {}", existingTables);

        List<String> missingTables = ddlTables.stream()
                .filter(t -> !existingTables.contains(t))
                .collect(Collectors.toList());
        log.info("[identify] Tabelas faltantes: {}", missingTables);

        List<MissingColumnInfo> missingColumns = new ArrayList<>();
        for (TableColumnInfo tableCol : ddlColumns) {
            if (existingTables.contains(tableCol.tableName())) {
                List<String> existingColumns = getExistingColumns(tableCol.tableName());
                for (String colName : tableCol.columns()) {
                    if (!existingColumns.contains(colName)) {
                        missingColumns.add(new MissingColumnInfo(
                                tableCol.tableName(),
                                colName,
                                "String",
                                true
                        ));
                    }
                }
            }
        }
        log.info("[identify] Colunas faltantes: {}", missingColumns.size());

        log.info("[identify] Identificação concluída");
        return new UnknownEntityResponse(missingTables, missingColumns);
    }

    /**
     * Extrai nomes de tabelas de um script DDL.
     *
     * @param ddl script DDL
     * @return lista de nomes de tabelas
     */
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

    /**
     * Extrai informações de colunas de um script DDL.
     *
     * @param ddl script DDL
     * @return lista de informações de tabelas e suas colunas
     */
    private List<TableColumnInfo> extractAllColumns(String ddl) {
        List<TableColumnInfo> result = new ArrayList<>();
        String[] statements = ddl.split("(?i)CREATE\\s+TABLE");
        for (String stmt : statements) {
            stmt = stmt.trim();
            if (stmt.isEmpty()) continue;

            Pattern tablePattern = Pattern.compile("(?:IF\\s+NOT\\s+EXISTS\\s+)?(?:\\w+\\.)?(\\w+)\\s*\\(");
            Matcher tableMatcher = tablePattern.matcher(stmt);
            if (!tableMatcher.find()) continue;
            String tableName = tableMatcher.group(1);

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

    /**
     * Obtém a lista de nomes de tabelas das classes Entity existentes.
     *
     * @return lista de nomes de tabelas
     * @throws IOException se houver erro ao ler os arquivos
     */
        private List<String> getExistingTables() throws IOException {
        List<String> allTables = new ArrayList<>();

        // 1. Escanear entidades manuais (domain.entity)
        Path manualDir = Paths.get(entityClassesPath, "domain", "entity");
        if (Files.exists(manualDir)) {
            try (Stream<Path> files = Files.list(manualDir)) {
                files.filter(p -> p.toString().endsWith(".java"))
                     .map(this::extractTableNameFromFile)
                     .filter(Objects::nonNull)
                     .forEach(allTables::add);
            }
        }

        // 2. Escanear entidades geradas (com.thadeu.entities-core.domain.entity)
        Path generatedDir = Paths.get(entityClassesPath, "com", "thadeu", "entities-core", "domain", "entity");
        if (Files.exists(generatedDir)) {
            try (Stream<Path> files = Files.list(generatedDir)) {
                files.filter(p -> p.toString().endsWith(".java"))
                     .map(this::extractTableNameFromFile)
                     .filter(Objects::nonNull)
                     .forEach(allTables::add);
            }
        }

        log.debug("[getExistingTables] Total de tabelas encontradas: {}", allTables.size());
        return allTables;
    }

    /**
     * Extrai o nome da tabela de um arquivo de classe Entity.
     *
     * @param filePath caminho do arquivo
     * @return nome da tabela ou null se não encontrado
     */
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

    /**
     * Obtém a lista de nomes de colunas de uma tabela específica.
     *
     * @param tableName nome da tabela
     * @return lista de nomes de colunas
     */
    private List<String> getExistingColumns(String tableName) {
            List<String> allColumns = new ArrayList<>();

            // 1. Escanear entidades manuais (domain.entity)
            Path manualDir = Paths.get(entityClassesPath, "domain", "entity");
            allColumns.addAll(scanColumnsFromDir(manualDir, tableName));

            // 2. Escanear entidades geradas (com.thadeu.entities-core.domain.entity)
            Path generatedDir = Paths.get(entityClassesPath, "com", "thadeu", "entities-core", "domain", "entity");
            allColumns.addAll(scanColumnsFromDir(generatedDir, tableName));

            log.debug("[getExistingColumns] Total de colunas encontradas para {}: {}", tableName, allColumns.size());
            return allColumns;
        }

        /**
         * Escaneia um diretório em busca de colunas de uma tabela específica.
         *
         * @param dir       diretório a ser escaneado
         * @param tableName nome da tabela procurada
         * @return lista de nomes de colunas encontradas
         */
        private List<String> scanColumnsFromDir(Path dir, String tableName) {
            if (!Files.exists(dir)) return List.of();

            try (Stream<Path> files = Files.list(dir)) {
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

    /**
     * Extrai nomes de colunas de um conteúdo Java.
     *
     * @param javaContent conteúdo do arquivo Java
     * @return lista de nomes de colunas
     */
    private List<String> extractColumnNames(String javaContent) {
        List<String> columns = new ArrayList<>();
        Pattern pattern = Pattern.compile("@Column\\(name\\s*=\\s*\"(\\w+)\"\\)");
        Matcher matcher = pattern.matcher(javaContent);
        while (matcher.find()) {
            columns.add(matcher.group(1));
        }
        return columns;
    }

    /**
     * Registro interno para informações de tabela e suas colunas.
     *
     * @param tableName nome da tabela
     * @param columns   lista de nomes de colunas
     */
    record TableColumnInfo(String tableName, List<String> columns) {}
}
