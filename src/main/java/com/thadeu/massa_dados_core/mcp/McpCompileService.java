package com.thadeu.massa_dados_core.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Serviço responsável por recompilar o Servidor 2 após a geração de novas classes Entity.
 *
 * <p>Executa {@code mvn compile} no diretório do projeto do Servidor 2.
 */
@Service
public class McpCompileService {

    private static final Logger log = LoggerFactory.getLogger(McpCompileService.class);

    private final Path projectPath;

    public McpCompileService(@Value("${entity.project.path}") String projectPath) {
        this.projectPath = Paths.get(projectPath);
    }

    /**
     * Executa {@code mvn compile} no diretório do projeto do Servidor 2.
     *
     * @throws RuntimeException se a compilação falhar
     */
    public void compile() {
        log.info("Iniciando compilação do Servidor 2 em: {}", projectPath);

        ProcessBuilder pb = new ProcessBuilder(
                "mvn", "compile", "-q"
        );
        pb.directory(projectPath.toFile());
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            StringBuilder output = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("Compilação falhou. Código de saída: {}\n{}", exitCode, output);
                throw new RuntimeException(
                        "Falha na compilação do Servidor 2. Código: " + exitCode
                                + "\n" + output);
            }

            log.info("Compilação concluída com sucesso.");
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Erro ao executar compilação do Servidor 2", e);
        }
    }
}
