package com.thadeu.massa_dados_core.mcp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
public class McpCompileService {

    @Value("${entity.project.path}")
    private String projectPath;

    public CompileResult compile() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "mvn.cmd", "compile", "-q"
            );
            pb.directory(new File(projectPath));
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

    public record CompileResult(boolean success, String message) {}
}
