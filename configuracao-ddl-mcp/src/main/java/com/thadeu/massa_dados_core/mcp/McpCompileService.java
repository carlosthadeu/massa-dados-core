package com.thadeu.massa_dados_core.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Serviço responsável por recompilar o projeto de entidades via Maven.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Executar {@code mvn compile} no diretório do projeto de entidades</li>
 *   <li>Capturar saída e código de retorno do processo Maven</li>
 *   <li>Retornar resultado da compilação (sucesso/erro com detalhes)</li>
 * </ul>
 *
 * @author Thadeu Garrido
 * @version 1.0
 */
@Service
public class McpCompileService {

    private static final Logger log = LoggerFactory.getLogger(McpCompileService.class);

    @Value("${entity.core.project.path}")
    private String coreProjectPath;

    /**
     * Executa a compilação do projeto de entidades via Maven.
     *
     * @return resultado da compilação contendo sucesso e mensagem
     */
    public CompileResult compile() {
        log.info("[compile] Iniciando compilação do projeto em {}", coreProjectPath);
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "mvn.cmd", "compile", "-q"
            );
            pb.directory(new File(coreProjectPath));
            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean finished = process.waitFor(60, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("[compile] Timeout após 60 segundos");
                return new CompileResult(false, "Timeout após 60 segundos");
            }

            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.exitValue();

            if (exitCode == 0) {
                log.info("[compile] Compilação bem-sucedida");
            } else {
                log.warn("[compile] Compilação falhou com código {}", exitCode);
            }
            log.debug("[compile] Saída do Maven: {}", output);

            return new CompileResult(exitCode == 0, output);
        } catch (IOException e) {
            log.error("[compile] Erro de I/O ao executar Maven", e);
            return new CompileResult(false, "Erro de I/O: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[compile] Compilação interrompida", e);
            return new CompileResult(false, "Compilação interrompida: " + e.getMessage());
        }
    }

    /**
     * Registro que encapsula o resultado da compilação.
     *
     * @param success true se a compilação foi bem-sucedida
     * @param message mensagem de saída ou erro da compilação
     */
    public record CompileResult(boolean success, String message) {}
}
