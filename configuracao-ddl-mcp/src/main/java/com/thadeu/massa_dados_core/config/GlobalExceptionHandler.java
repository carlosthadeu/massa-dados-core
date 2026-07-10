package com.thadeu.massa_dados_core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Manipulador global de exceções para o servidor configuracao-ddl-mcp.
 *
 * <p>Captura exceções e retorna respostas padronizadas no formato JSON-RPC.</p>
 *
 * @author Thadeu Garrido
 * @version 1.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Trata exceções de argumento inválido (parâmetros incorretos).
     *
     * @param ex exceção capturada
     * @return resposta HTTP 400 com formato JSON-RPC de erro
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("[handleIllegalArgument] Argumento inválido: {}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("jsonrpc", "2.0");
        body.put("error", Map.of("code", -32602, "message", ex.getMessage()));
        body.put("id", null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Trata exceções de método HTTP não suportado.
     *
     * @param ex exceção capturada
     * @return resposta HTTP 405 com formato JSON-RPC de erro
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("[handleMethodNotSupported] Método HTTP não suportado: {}", ex.getMethod());
        Map<String, Object> body = new HashMap<>();
        body.put("jsonrpc", "2.0");
        body.put("error", Map.of("code", -32601, "message", "Method not found: " + ex.getMethod()));
        body.put("id", null);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }

    /**
     * Trata exceções de corpo da requisição não legível (JSON inválido ou vazio).
     *
     * @param ex exceção capturada
     * @return resposta HTTP 400 com formato JSON-RPC de erro
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("[handleMessageNotReadable] Corpo da requisição inválido: {}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("jsonrpc", "2.0");
        body.put("error", Map.of("code", -32700, "message", "Parse error: " + ex.getMessage()));
        body.put("id", null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Trata exceções genéricas não tratadas por outros handlers.
     *
     * @param ex exceção capturada
     * @return resposta HTTP 500 com formato JSON-RPC de erro
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        log.error("[handleGeneral] Erro não tratado", ex);
        Map<String, Object> body = new HashMap<>();
        body.put("jsonrpc", "2.0");
        body.put("error", Map.of("code", -32603, "message", ex.getMessage()));
        body.put("id", null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
