package com.thadeu.massa_dados_core.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thadeu.massa_dados_core.mcp.DdlToolsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/mcp")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class McpController {

    private static final Logger log = LoggerFactory.getLogger(McpController.class);
    private static final String CRLF = "\r\n";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DdlToolsService ddlToolsService;
    private final Map<String, PrintWriter> writers = new ConcurrentHashMap<>();

    public McpController(DdlToolsService ddlToolsService) {
        this.ddlToolsService = ddlToolsService;
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void handleSseHandshake(HttpServletRequest request, HttpServletResponse response) {
        String sessionId = UUID.randomUUID().toString();
        log.info("[SSE-GET] Nova conexão SSE solicitada: sessionId={}", sessionId);

        response.setContentType("text/event-stream;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");

        try {
            String endpointUrl = ServletUriComponentsBuilder.fromContextPath(request)
                    .path("/mcp/message")
                    .queryParam("sessionId", sessionId)
                    .toUriString();

            PrintWriter writer = response.getWriter();
            writers.put(sessionId, writer);

            writer.print("event: endpoint" + CRLF);
            writer.print("data: " + endpointUrl + CRLF);
            writer.print(CRLF);
            writer.flush();

            log.info("[SSE-GET] Evento 'endpoint' transmitido com sucesso: {}", endpointUrl);

            while (true) {
                try {
                    Thread.sleep(1000);
                    if (writer.checkError()) {
                        log.info("[SSE-GET] Cliente desconectado: {}", sessionId);
                        break;
                    }
                } catch (InterruptedException e) {
                    log.info("[SSE-GET] Thread interrompida: {}", sessionId);
                    Thread.currentThread().interrupt();
                    break;
                }
            }

        } catch (Exception e) {
            log.error("[SSE-GET] Erro no handshake: {}", e.getMessage(), e);
        } finally {
            writers.remove(sessionId);
        }
    }

    @PostMapping
    public Object handleMcpRequest(
            @RequestBody String body,
            @RequestHeader(value = "Accept", defaultValue = "") String acceptHeader) {

        log.info("[MCP] POST - Accept: {}, body: {}", acceptHeader, body);

        if (acceptHeader.contains("text/event-stream")) {
            log.info("[MCP] Processando requisição via fluxo SSE");
            return handleSsePost(body);
        }

        log.info("[MCP] Processando requisição via resposta JSON");
        return handleJsonPost(body);
    }

    private ResponseEntity<Map<String, Object>> handleSsePost(String body) {
        String sessionId = UUID.randomUUID().toString();
        log.info("[SSE-POST] Nova sessão iniciada: {}", sessionId);

        try {
            JsonNode json = objectMapper.readTree(body);
            String method = json.path("method").asText();

            if ("notifications/initialized".equals(method)) {
                log.info("[SSE-POST] Notificação 'initialized' recebida");
                return ResponseEntity.ok().build();
            }

            Object id = json.has("id") && json.get("id").isInt() ? json.get("id").asInt() : json.path("id").asText();

            Map<String, Object> response = new HashMap<>();
            response.put("jsonrpc", "2.0");
            response.put("id", id);

            switch (method) {
                case "initialize" -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("protocolVersion", "2024-11-05");
                    Map<String, Object> capabilities = new HashMap<>();
                    Map<String, Object> tools = new HashMap<>();
                    tools.put("listChanged", true);
                    capabilities.put("tools", tools);
                    result.put("capabilities", capabilities);
                    Map<String, Object> serverInfo = new HashMap<>();
                    serverInfo.put("name", "configuracao-ddl-mcp");
                    serverInfo.put("version", "1.0.0");
                    result.put("serverInfo", serverInfo);
                    response.put("result", result);
                }
                case "tools/list" -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("tools", DdlToolsService.getToolDefinitions());
                    response.put("result", result);
                }
                case "tools/call" -> {
                    String toolName = json.path("params").path("name").asText();
                    JsonNode arguments = json.path("params").path("arguments");

                    Map<String, Object> result = new HashMap<>();
                    try {
                        Object toolResult = executeTool(toolName, arguments);
                        Map<String, Object> content = new HashMap<>();
                        content.put("type", "text");
                        content.put("text", objectMapper.writeValueAsString(toolResult));
                        result.put("content", java.util.List.of(content));
                    } catch (Exception e) {
                        Map<String, Object> error = new HashMap<>();
                        error.put("code", -32000);
                        error.put("message", e.getMessage());
                        result.put("error", error);
                    }
                    response.put("result", result);
                }
                default -> {
                    Map<String, Object> error = new HashMap<>();
                    error.put("code", -32601);
                    error.put("message", "Method not found: " + method);
                    response.put("error", error);
                }
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            return ResponseEntity.ok().headers(headers).body(response);

        } catch (Exception e) {
            log.error("[SSE-POST] Falha no processamento: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("jsonrpc", "2.0");
            errorResponse.put("id", null);
            Map<String, Object> error = new HashMap<>();
            error.put("code", -32603);
            error.put("message", e.getMessage());
            errorResponse.put("error", error);
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    private ResponseEntity<Map<String, Object>> handleJsonPost(String body) {
        try {
            JsonNode json = objectMapper.readTree(body);
            String method = json.path("method").asText();

            if ("notifications/initialized".equals(method)) {
                log.info("[JSON] Notificação 'initialized' recebida");
                return ResponseEntity.ok().build();
            }

            Object id = json.has("id") && json.get("id").isInt() ? json.get("id").asInt() : json.path("id").asText();

            Map<String, Object> response = new HashMap<>();
            response.put("jsonrpc", "2.0");
            response.put("id", id);

            switch (method) {
                case "initialize" -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("protocolVersion", "2024-11-05");
                    Map<String, Object> capabilities = new HashMap<>();
                    Map<String, Object> tools = new HashMap<>();
                    tools.put("listChanged", true);
                    capabilities.put("tools", tools);
                    result.put("capabilities", capabilities);
                    Map<String, Object> serverInfo = new HashMap<>();
                    serverInfo.put("name", "configuracao-ddl-mcp");
                    serverInfo.put("version", "1.0.0");
                    result.put("serverInfo", serverInfo);
                    response.put("result", result);
                }
                case "tools/list" -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("tools", DdlToolsService.getToolDefinitions());
                    response.put("result", result);
                }
                case "tools/call" -> {
                    String toolName = json.path("params").path("name").asText();
                    JsonNode arguments = json.path("params").path("arguments");

                    Map<String, Object> result = new HashMap<>();
                    try {
                        Object toolResult = executeTool(toolName, arguments);
                        Map<String, Object> content = new HashMap<>();
                        content.put("type", "text");
                        content.put("text", objectMapper.writeValueAsString(toolResult));
                        result.put("content", java.util.List.of(content));
                    } catch (Exception e) {
                        Map<String, Object> error = new HashMap<>();
                        error.put("code", -32000);
                        error.put("message", e.getMessage());
                        result.put("error", error);
                    }
                    response.put("result", result);
                }
                default -> {
                    Map<String, Object> error = new HashMap<>();
                    error.put("code", -32601);
                    error.put("message", "Method not found: " + method);
                    response.put("error", error);
                }
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            return ResponseEntity.ok().headers(headers).body(response);

        } catch (Exception e) {
            log.error("[JSON] Erro ao processar requisição JSON", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("jsonrpc", "2.0");
            errorResponse.put("id", null);
            Map<String, Object> error = new HashMap<>();
            error.put("code", -32603);
            error.put("message", e.getMessage());
            errorResponse.put("error", error);

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/message")
    public ResponseEntity<Map<String, Object>> handleSseMessage(
            @RequestParam String sessionId,
            @RequestBody String body) {

        log.info("[SSE-MESSAGE] Mensagem para sessão {}: {}", sessionId, body);

        PrintWriter writer = writers.get(sessionId);
        if (writer == null) {
            log.warn("[SSE-MESSAGE] Sessão não encontrada ou expirada: {}", sessionId);
            return ResponseEntity.status(404).build();
        }

        try {
            JsonNode json = objectMapper.readTree(body);
            String method = json.path("method").asText();

            if ("notifications/initialized".equals(method)) {
                log.info("[SSE-MESSAGE] Notificação 'initialized' recebida com sucesso");
                return ResponseEntity.ok().build();
            }

            Object id = json.has("id") && json.get("id").isInt() ? json.get("id").asInt() : json.path("id").asText();

            Map<String, Object> response = new HashMap<>();
            response.put("jsonrpc", "2.0");
            response.put("id", id);

            switch (method) {
                case "initialize" -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("protocolVersion", "2024-11-05");
                    Map<String, Object> capabilities = new HashMap<>();
                    Map<String, Object> tools = new HashMap<>();
                    tools.put("listChanged", true);
                    capabilities.put("tools", tools);
                    result.put("capabilities", capabilities);
                    Map<String, Object> serverInfo = new HashMap<>();
                    serverInfo.put("name", "configuracao-ddl-mcp");
                    serverInfo.put("version", "1.0.0");
                    result.put("serverInfo", serverInfo);
                    response.put("result", result);
                }
                case "tools/list" -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("tools", DdlToolsService.getToolDefinitions());
                    response.put("result", result);
                }
                case "tools/call" -> {
                    String toolName = json.path("params").path("name").asText();
                    JsonNode arguments = json.path("params").path("arguments");

                    Map<String, Object> result = new HashMap<>();
                    try {
                        Object toolResult = executeTool(toolName, arguments);
                        Map<String, Object> content = new HashMap<>();
                        content.put("type", "text");
                        content.put("text", objectMapper.writeValueAsString(toolResult));
                        result.put("content", java.util.List.of(content));
                    } catch (Exception e) {
                        Map<String, Object> error = new HashMap<>();
                        error.put("code", -32000);
                        error.put("message", e.getMessage());
                        result.put("error", error);
                    }
                    response.put("result", result);
                }
                default -> {
                    Map<String, Object> error = new HashMap<>();
                    error.put("code", -32601);
                    error.put("message", "Method not found: " + method);
                    response.put("error", error);
                }
            }

            String event = "event: message" + CRLF +
                          "data: " + objectMapper.writeValueAsString(response) + CRLF +
                          CRLF;
            writer.print(event);
            writer.flush();

            log.info("[SSE-MESSAGE] Resposta transmitida com sucesso para {}", sessionId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            return ResponseEntity.ok().headers(headers).body(response);

        } catch (Exception e) {
            log.error("[SSE-MESSAGE] Erro no processamento: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("jsonrpc", "2.0");
            errorResponse.put("id", null);
            Map<String, Object> error = new HashMap<>();
            error.put("code", -32603);
            error.put("message", e.getMessage());
            errorResponse.put("error", error);

            sendSseError(writer, e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    private Object executeTool(String toolName, JsonNode arguments) {
        return switch (toolName) {
            case "ddl_to_entity" -> {
                String ddlScript = arguments.path("ddlScript").asText();
                if (ddlScript == null || ddlScript.isEmpty()) {
                    throw new IllegalArgumentException("ddlScript é obrigatório");
                }
                yield ddlToolsService.ddlToEntity(ddlScript);
            }
            case "identify_unknown_entities" -> {
                String ddlScript = arguments.path("ddlScript").asText();
                if (ddlScript == null || ddlScript.isEmpty()) {
                    throw new IllegalArgumentException("ddlScript é obrigatório");
                }
                yield ddlToolsService.identifyUnknownEntities(ddlScript);
            }
            case "get_generation_config" -> ddlToolsService.getGenerationConfig();
            default -> throw new IllegalArgumentException("Tool não encontrada: " + toolName);
        };
    }

    private void sendSseError(PrintWriter writer, String errorMessage) {
        try {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("jsonrpc", "2.0");
            errorResponse.put("id", null);

            Map<String, Object> error = new HashMap<>();
            error.put("code", -32603);
            error.put("message", errorMessage);
            errorResponse.put("error", error);

            String event = "event: message" + CRLF +
                          "data: " + objectMapper.writeValueAsString(errorResponse) + CRLF +
                          CRLF;
            writer.print(event);
            writer.flush();
        } catch (Exception ex) {
            log.error("[SSE] Falha ao transmitir evento de erro", ex);
        }
    }
}