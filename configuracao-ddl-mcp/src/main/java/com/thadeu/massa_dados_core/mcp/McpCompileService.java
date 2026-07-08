package com.thadeu.massa_dados_core.mcp;

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

    @Value("${entity.core.project.path}")
    private String coreProjectPath;

    /**
     * Executa a compilação do projeto de entidades via Maven.
     *
     * @return resultado da compilação contendo sucesso e mensagem
     */
    public CompileResult compile() {
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
                return new CompileResult(false, "Timeout após 60 segundos");
            }

            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.exitValue();

            return new CompileResult(exitCode == 0, output);
        } catch (IOException e) {
            return new CompileResult(false, "Erro de I/O: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
