package com.thadeu.massa_dados_core.mcp;

import com.thadeu.massa_dados_core.mcp.dto.GenerationConfigResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class McpGenerationConfigService {

    private static final Logger log = LoggerFactory.getLogger(McpGenerationConfigService.class);

    @Value("${entity.classes.path}")
    private String entityClassesPath;

    private static final String GENERATED_PACKAGE = "com.thadeu.entities-core.domain.entity";
    private static final String SOURCE_PACKAGE = "domain.entity";

    public GenerationConfigResponse getConfig() {
        log.info("[getConfig] Obtendo configuracoes de geracao");

        Path manualDir = Paths.get(entityClassesPath, "domain", "entity");
        Path generatedDir = Paths.get(entityClassesPath, "com", "thadeu", "entities-core", "domain", "entity");

        List<String> manualEntities = scanEntities(manualDir);
        List<String> generatedEntities = scanEntities(generatedDir);

        String relativePathBase = extractRelativeBase(entityClassesPath);
        String generatedRelativePath = relativePathBase + "com/thadeu/entities-core/domain/entity/";
        String sourceRelativePath = relativePathBase + "domain/entity/";

        return new GenerationConfigResponse(
                generatedRelativePath, GENERATED_PACKAGE, SOURCE_PACKAGE,
                sourceRelativePath, generatedEntities, manualEntities.size());
    }

    private List<String> scanEntities(Path dir) {
        if (!Files.exists(dir) || !Files.isDirectory(dir)) return List.of();
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(p -> p.toString().endsWith(".java"))
                    .map(p -> { String fn = p.getFileName().toString(); return fn.substring(0, fn.length() - 5); })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.warn("[scanEntities] Erro ao escanear: {}", e.getMessage());
            return List.of();
        }
    }

    private String extractRelativeBase(String absolutePath) {
        int idx = absolutePath.indexOf("entities-core");
        if (idx >= 0) {
            String rel = absolutePath.substring(idx).replace('\\', '/');
            return rel.endsWith("/") ? rel : rel + "/";
        }
        String fallback = absolutePath.replace('\\', '/');
        return fallback.endsWith("/") ? fallback : fallback + "/";
    }
}