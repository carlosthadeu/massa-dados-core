package com.thadeu.massa_dados_core.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class McpDemandaService {

    @Value("${demandas.path:demandas}")
    private String demandasPath;

    private final ObjectMapper objectMapper;

    public McpDemandaService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Paths.get(demandasPath, "pendentes"));
        Files.createDirectories(Paths.get(demandasPath, "resolvidas"));
    }

    public String gerarDemanda(String solicitacao, String motivo, String sugestao, String detalhesTecnicos) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss"));
            String nomeArquivo = timestamp + "-" + gerarSlug(solicitacao) + ".md";
            Path arquivo = Paths.get(demandasPath, "pendentes", nomeArquivo);

            String conteudo = String.format("""
                # Demanda: %s

                ## Solicitação do usuário
                %s

                ## Motivo da falha
                %s

                ## Sugestão de ação
                %s

                ## Detalhes técnicos
                %s

                ## Status
                Pendente
                """,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    solicitacao,
                    motivo,
                    sugestao,
                    detalhesTecnicos
            );

            Files.writeString(arquivo, conteudo);
            return nomeArquivo;
        } catch (IOException e) {
            return null;
        }
    }

    public List<Map<String, Object>> listarDemandasPendentes() throws IOException {
        return listarDemandas("pendentes");
    }

    public List<Map<String, Object>> listarDemandasResolvidas() throws IOException {
        return listarDemandas("resolvidas");
    }

    private List<Map<String, Object>> listarDemandas(String status) throws IOException {
        Path dir = Paths.get(demandasPath, status);
        if (!Files.exists(dir)) return List.of();

        try (Stream<Path> files = Files.list(dir)) {
            return files
                    .filter(p -> p.toString().endsWith(".md"))
                    .map(p -> {
                        try {
                            String conteudo = Files.readString(p);
                            String nome = p.getFileName().toString();
                            String primeiraLinha = conteudo.lines().findFirst().orElse("");
                            String dataHora = primeiraLinha.replace("# Demanda: ", "").trim();

                            return Map.<String, Object>of(
                                    "id", nome,
                                    "dataHora", dataHora,
                                    "resumo", extrairResumo(conteudo),
                                    "status", status
                            );
                        } catch (IOException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
    }

    public Map<String, Object> detalharDemanda(String id) throws IOException {
        // Procurar em pendentes e resolvidas
        for (String status : List.of("pendentes", "resolvidas")) {
            Path arquivo = Paths.get(demandasPath, status, id);
            if (Files.exists(arquivo)) {
                String conteudo = Files.readString(arquivo);
                return Map.of(
                        "id", id,
                        "conteudo", conteudo,
                        "status", status
                );
            }
        }
        return null;
    }

    public boolean resolverDemanda(String id) throws IOException {
        Path pendente = Paths.get(demandasPath, "pendentes", id);
        if (!Files.exists(pendente)) return false;

        Path resolvida = Paths.get(demandasPath, "resolvidas", id);
        Files.move(pendente, resolvida, StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

    private String gerarSlug(String texto) {
        return texto.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "-")
                .substring(0, Math.min(texto.length(), 50));
    }

    private String extrairResumo(String conteudo) {
        // Extrair a primeira linha após "## Solicitação do usuário"
        String[] linhas = conteudo.split("\n");
        boolean capturar = false;
        for (String linha : linhas) {
            if (linha.trim().equals("## Solicitação do usuário")) {
                capturar = true;
                continue;
            }
            if (capturar && !linha.trim().isEmpty()) {
                return linha.trim();
            }
        }
        return "";
    }
}
