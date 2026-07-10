package com.thadeu.massa_dados_core.mcp;

import com.thadeu.massa_dados_core.mcp.dto.AttributeInfo;
import com.thadeu.massa_dados_core.mcp.dto.DdlRequest;
import com.thadeu.massa_dados_core.mcp.dto.DdlResponse;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Serviço responsável por converter scripts DDL em classes Entity JPA.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Extrair nome da tabela e colunas de um script DDL</li>
 *   <li>Gerar código Java da classe Entity com anotações JPA</li>
 *   <li>Salvar o arquivo no diretório de entidades</li>
 *   <li>Recompilar o projeto de entidades</li>
 * </ul>
 *
 * @author Thadeu Garrido
 * @version 1.0
 */
@Service
public class McpDdlToEntityService {

    private static final Logger log = LoggerFactory.getLogger(McpDdlToEntityService.class);

    @Value("${entity.classes.path}")
    private String entityClassesPath;

    @Value("${entity.project.path}")
    private String entityProjectPath;

    private final McpCompileService compileService;

    /**
     * Construtor com injeção do serviço de compilação.
     *
     * @param compileService serviço para recompilar o projeto após gerar a Entity
     */
    public McpDdlToEntityService(McpCompileService compileService) {
        this.compileService = compileService;
    }

    /**
     * Processa um script DDL e gera a classe Entity correspondente.
     *
     * @param request requisição contendo o script DDL
     * @return resposta com o nome da classe, código gerado e resultado da compilação
     * @throws IOException se houver erro ao salvar o arquivo
     * @throws IllegalArgumentException se o DDL não contiver uma instrução CREATE TABLE válida
     */
    public DdlResponse processDdl(DdlRequest request) throws IOException {
        log.info("[processDdl] Iniciando processamento de DDL");
        String ddl = request.ddlScript();

        if (ddl == null || ddl.trim().isEmpty()) {
            log.warn("[processDdl] DDL vazio ou nulo");
            throw new IllegalArgumentException("DDL invalido");
        }

        // Extrair nome da tabela
        String tableName = extractTableName(ddl);
        if (tableName == null) {
            log.warn("[processDdl] Não foi possível extrair nome da tabela do DDL");
            throw new IllegalArgumentException("Não foi possível extrair o nome da tabela do DDL");
        }
        log.debug("[processDdl] Nome da tabela extraída: {}", tableName);

        // Extrair colunas
        List<ColumnInfo> columns = extractColumns(ddl);
        log.debug("[processDdl] Colunas extraídas: {}", columns.size());

        // Gerar nome da classe (PascalCase a partir do nome da tabela)
        String className = toPascalCase(tableName);
        log.debug("[processDdl] Nome da classe gerado: {}", className);

        // Gerar código da Entity
        String entityCode = generateEntityCode(className, tableName, columns);
        log.debug("[processDdl] Código da Entity gerado ({} caracteres)", entityCode.length());

        // Salvar arquivo
        String packagePath = "br.gov.bnb.domain.entity";
        String packageDir = packagePath.replace('.', File.separatorChar);
        Path outputDir = Paths.get(entityClassesPath, packageDir);
        Files.createDirectories(outputDir);

        Path outputFile = outputDir.resolve(className + ".java");
        Files.writeString(outputFile, entityCode);
        log.info("[processDdl] Arquivo salvo em: {}", outputFile.toAbsolutePath());

        // Recompilar Servidor 2
        log.info("[processDdl] Iniciando compilação do projeto de entidades");
        McpCompileService.CompileResult compileResult = compileService.compile();
        if (compileResult.success()) {
            log.info("[processDdl] Compilação bem-sucedida");
        } else {
            log.warn("[processDdl] Compilação falhou: {}", compileResult.message());
        }

        // Construir resposta
        List<AttributeInfo> attributes = columns.stream()
                .map(col -> new AttributeInfo(
                        toCamelCase(col.name()),
                        mapSqlTypeToJava(col.type()),
                        col.nullable(),
                        col.primaryKey(),
                        col.primaryKey(),
                        col.name(),
                        null, null, null,
                        List.of()
                ))
                .toList();

        log.info("[processDdl] Processamento concluído para tabela {}", tableName);
        return new DdlResponse(className, entityCode, tableName, attributes, compileResult.success(), compileResult.message());
    }

    /**
     * Extrai o nome da tabela de um script DDL.
     *
     * @param ddl script DDL
     * @return nome da tabela ou null se não encontrado
     */
    private String extractTableName(String ddl) {
        Pattern pattern = Pattern.compile("CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(?:\\w+\\.)?(\\w+)",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(ddl);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Extrai as colunas de um script DDL.
     *
     * @param ddl script DDL
     * @return lista de informações das colunas
     */
    private List<ColumnInfo> extractColumns(String ddl) {
        List<ColumnInfo> columns = new ArrayList<>();
        // Extrair bloco entre parênteses
        int start = ddl.indexOf('(');
        int end = ddl.lastIndexOf(')');
        if (start < 0 || end < 0) return columns;

        String body = ddl.substring(start + 1, end);
        String[] lines = body.split(",\\s*(?![^()]*\\))");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.toUpperCase().startsWith("CONSTRAINT") || line.toUpperCase().startsWith("PRIMARY KEY")) {
                continue;
            }

            // Extrair nome da coluna e tipo
            String[] parts = line.split("\\s+", 3);
            if (parts.length < 2) continue;

            String colName = parts[0];
            String rawType = parts[1].toUpperCase();
            String colType = rawType.contains("(") ? rawType.substring(0, rawType.indexOf('(')) : rawType;

            boolean nullable = !line.toUpperCase().contains("NOT NULL");
            boolean primaryKey = line.toUpperCase().contains("PRIMARY KEY");

            columns.add(new ColumnInfo(colName, colType, nullable, primaryKey));
        }

        return columns;
    }

    /**
     * Gera o código Java da classe Entity.
     *
     * @param className nome da classe
     * @param tableName nome da tabela
     * @param columns   lista de colunas
     * @return código Java completo da Entity
     */
    private String generateEntityCode(String className, String tableName, List<ColumnInfo> columns) {
        StringBuilder sb = new StringBuilder();
        sb.append("package br.gov.bnb.domain.entity;\n\n");
        sb.append("import javax.persistence.*;\n");
        sb.append("import java.io.Serializable;\n");
        sb.append("import java.time.LocalDateTime;\n");
        sb.append("import java.math.BigDecimal;\n\n");
        sb.append("@Entity\n");
        sb.append("@Table(name=\"").append(tableName).append("\")\n");
        sb.append("public class ").append(className).append(" implements Serializable {\n\n");

        // Atributos
        for (ColumnInfo col : columns) {
            String fieldName = toCamelCase(col.name());
            String javaType = mapSqlTypeToJava(col.type());

            sb.append("    @Column(name = \"").append(col.name()).append("\"");
            if (!col.nullable()) {
                sb.append(", nullable = false");
            }
            sb.append(")\n");
            if (col.primaryKey()) {
                sb.append("    @Id\n");
                sb.append("    @GeneratedValue(strategy = GenerationType.IDENTITY)\n");
            }
            sb.append("    private ").append(javaType).append(" ").append(fieldName).append(";\n\n");
        }

        // Getters e Setters
        for (ColumnInfo col : columns) {
            String fieldName = toCamelCase(col.name());
            String javaType = mapSqlTypeToJava(col.type());
            String getterName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            String setterName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);

            sb.append("    public ").append(javaType).append(" ").append(getterName).append("() {\n");
            sb.append("        return ").append(fieldName).append(";\n");
            sb.append("    }\n\n");

            sb.append("    public void ").append(setterName).append("(").append(javaType).append(" ").append(fieldName).append(") {\n");
            sb.append("        this.").append(fieldName).append(" = ").append(fieldName).append(";\n");
            sb.append("    }\n\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Converte um nome de tabela (snake_case) para PascalCase.
     *
     * @param tableName nome da tabela
     * @return nome em PascalCase
     */
    private String toPascalCase(String tableName) {
        // Remove prefixo T696 e sufixo
        String name = tableName;
        if (name.startsWith("T696")) {
            name = name.substring(4);
        }
        // Converte snake_case para PascalCase
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

    /**
     * Converte um nome de coluna (snake_case) para camelCase.
     *
     * @param columnName nome da coluna
     * @return nome em camelCase
     */
    private String toCamelCase(String columnName) {
        String pascal = toPascalCase(columnName);
        if (pascal.isEmpty()) return "";
        return Character.toLowerCase(pascal.charAt(0)) + pascal.substring(1);
    }

    /**
     * Mapeia um tipo SQL para o tipo Java correspondente.
     *
     * @param sqlType tipo SQL
     * @return tipo Java
     */
    private String mapSqlTypeToJava(String sqlType) {
        return switch (sqlType) {
            case "INT", "INTEGER", "SMALLINT", "BIGINT" -> "Long";
            case "VARCHAR", "CHAR", "CLOB" -> "String";
            case "DECIMAL", "NUMERIC", "FLOAT", "DOUBLE" -> "BigDecimal";
            case "BOOLEAN", "BIT" -> "Boolean";
            case "TIMESTAMP", "DATETIME", "DATE" -> "LocalDateTime";
            default -> "String";
        };
    }

    /**
     * Registro interno para informações de coluna.
     *
     * @param name       nome da coluna
     * @param type       tipo SQL
     * @param nullable   se permite nulo
     * @param primaryKey se é chave primária
     */
    record ColumnInfo(String name, String type, boolean nullable, boolean primaryKey) {}
}
