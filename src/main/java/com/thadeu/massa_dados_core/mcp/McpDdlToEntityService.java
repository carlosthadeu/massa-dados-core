package com.thadeu.massa_dados_core.mcp;

import com.thadeu.massa_dados_core.mcp.dto.DdlRequest;
import com.thadeu.massa_dados_core.mcp.dto.DdlResponse;
import com.thadeu.massa_dados_core.mcp.dto.DdlResponse.AttributeInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
 * <p>Analisa a sintaxe SQL, extrai metadados da tabela e gera o código Java
 * correspondente, salvando o arquivo no diretório configurado.
 */
@Service
public class McpDdlToEntityService {

    private final Path entityClassesPath;
    private final McpCompileService compileService;

    public McpDdlToEntityService(
            @Value("${entity.classes.path}") String entityClassesPath,
            McpCompileService compileService) {
        this.entityClassesPath = Paths.get(entityClassesPath);
        this.compileService = compileService;
    }

    /**
     * Converte um script DDL em uma classe Entity e salva o arquivo.
     *
     * @param request Dados do DDL
     * @return Resposta com o código gerado
     */
    public DdlResponse convertDdlToEntity(DdlRequest request) {
        String ddl = request.ddlScript();

        // Extrair nome da tabela
        String tableName = extractTableName(ddl);
        if (tableName == null) {
            throw new IllegalArgumentException("Não foi possível extrair o nome da tabela do DDL");
        }

        // Extrair colunas
        List<ColumnInfo> columns = extractColumns(ddl);

        // Gerar nome da classe (PascalCase a partir do nome da tabela)
        String className = toPascalCase(tableName);

        // Gerar código Java
        String entityCode = generateEntityCode(className, tableName, columns);

        // Salvar arquivo
        Path filePath = entityClassesPath.resolve(className + ".java");
        try {
            Files.createDirectories(entityClassesPath);
            Files.writeString(filePath, entityCode);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar arquivo Entity: " + filePath, e);
        }

        // Recompilar Servidor 2
        compileService.compile();

        // Montar resposta
        List<AttributeInfo> attributeInfos = columns.stream()
                .map(col -> new AttributeInfo(
                        col.name(),
                        col.javaType(),
                        col.nullable(),
                        col.primaryKey(),
                        col.columnName()))
                .toList();

        return new DdlResponse(
                "com.thadeu.massa_dados_core.domain." + className,
                entityCode,
                tableName,
                attributeInfos
        );
    }

    // ========== Métodos auxiliares ==========

    private String extractTableName(String ddl) {
        Pattern pattern = Pattern.compile(
                "CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(?:\\w+\\.)?(\\w+)",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(ddl);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private List<ColumnInfo> extractColumns(String ddl) {
        List<ColumnInfo> columns = new ArrayList<>();

        // Extrair o bloco entre parênteses que contém as colunas
        Pattern blockPattern = Pattern.compile(
                "\\(([^()]*(?:\\([^()]*\\)[^()]*)*)\\)",
                Pattern.DOTALL);
        Matcher blockMatcher = blockPattern.matcher(ddl);
        if (!blockMatcher.find()) {
            return columns;
        }
        String columnsBlock = blockMatcher.group(1);

        // Dividir por vírgulas, ignorando vírgulas dentro de parênteses
        String[] lines = columnsBlock.split("\\s*,\\s*(?![^()]*\\))");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // Ignorar constraints (CONSTRAINT, PRIMARY KEY, FOREIGN KEY, INDEX)
            if (line.toUpperCase().startsWith("CONSTRAINT")
                    || line.toUpperCase().startsWith("PRIMARY KEY")
                    || line.toUpperCase().startsWith("FOREIGN KEY")
                    || line.toUpperCase().startsWith("INDEX")
                    || line.toUpperCase().startsWith("UNIQUE")) {
                continue;
            }

            // Extrair nome da coluna e tipo
            String[] parts = line.split("\\s+", 2);
            if (parts.length < 2) continue;

            String columnName = parts[0];
            String typePart = parts[1].toUpperCase();

            boolean nullable = !typePart.contains("NOT NULL");
            boolean primaryKey = typePart.contains("PRIMARY KEY");

            // Mapear tipo SQL para tipo Java
            String javaType = mapSqlTypeToJava(typePart);

            // Nome do atributo em camelCase
            String attributeName = toCamelCase(columnName);

            columns.add(new ColumnInfo(attributeName, javaType, nullable, primaryKey, columnName));
        }

        return columns;
    }

    private String mapSqlTypeToJava(String sqlType) {
        if (sqlType.startsWith("INT") || sqlType.startsWith("SMALLINT")
                || sqlType.startsWith("BIGINT")) {
            if (sqlType.startsWith("BIGINT")) return "Long";
            return "Integer";
        }
        if (sqlType.startsWith("VARCHAR") || sqlType.startsWith("CHAR")
                || sqlType.startsWith("CLOB") || sqlType.startsWith("TEXT")) {
            return "String";
        }
        if (sqlType.startsWith("DECIMAL") || sqlType.startsWith("NUMERIC")
                || sqlType.startsWith("FLOAT") || sqlType.startsWith("DOUBLE")) {
            return "java.math.BigDecimal";
        }
        if (sqlType.startsWith("TIMESTAMP") || sqlType.startsWith("DATE")) {
            return "java.time.LocalDateTime";
        }
        if (sqlType.startsWith("BOOLEAN") || sqlType.startsWith("BIT")) {
            return "Boolean";
        }
        // Default
        return "String";
    }

    private String generateEntityCode(String className, String tableName, List<ColumnInfo> columns) {
        StringBuilder sb = new StringBuilder();
        sb.append("package com.thadeu.massa_dados_core.domain;\n\n");
        sb.append("import jakarta.persistence.*;\n");
        sb.append("import lombok.*;\n\n");
        sb.append("@Entity\n");
        sb.append("@Table(name = \"").append(tableName).append("\")\n");
        sb.append("@Data\n");
        sb.append("@NoArgsConstructor\n");
        sb.append("@AllArgsConstructor\n");
        sb.append("public class ").append(className).append(" {\n\n");

        for (ColumnInfo col : columns) {
            sb.append("    @Column(name = \"").append(col.columnName()).append("\"");
            if (!col.nullable()) {
                sb.append(", nullable = false");
            }
            sb.append(")\n");
            if (col.primaryKey()) {
                sb.append("    @Id\n");
                sb.append("    @GeneratedValue(strategy = GenerationType.IDENTITY)\n");
            }
            sb.append("    private ").append(col.javaType()).append(" ").append(col.name()).append(";\n\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    private String toPascalCase(String tableName) {
        // Remove prefixo T696 se existir
        String name = tableName;
        if (name.startsWith("T696")) {
            name = name.substring(4);
        }
        // Converte para camelCase/PascalCase
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

    private record ColumnInfo(
            String name,
            String javaType,
            boolean nullable,
            boolean primaryKey,
            String columnName
    ) {}
}
