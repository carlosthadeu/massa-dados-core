package com.thadeu.massa_dados_core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Manipulador global de exceções para o servidor configuracao-ddl-mcp.
 *
 * <p>Captura exceções e retorna respostas padronizadas no formato JSON-RPC,
 * sempre com Content-Type application/json para evitar conflitos com SSE.</p>
 *
 * @author Thadeu Garrido
 * @version 2.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Cria uma resposta de erro JSON-RPC com Content-Type application/json.
     *
     * @param status  código HTTP
     * @param code    código de erro JSON-RPC
     * @param message mensagem de erro
     * @return ResponseEntity com headers e body apropriados
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(HttpStatus status, int code, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("jsonrpc", "2.0");
        body.put("error", Map.of("code", code, "message", message));
        body.put("id", null);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return new ResponseEntity<>(body, headers, status);
    }

    /**
     * Trata exceções de parâmetro obrigatório ausente (ex: sessionId).
     *
     * @param ex exceção capturada
     * @return resposta HTTP 400 com formato JSON-RPC de erro
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(MissingServletRequestParameterException ex) {
        log.warn("[handleMissingParam] Parâmetro obrigatório ausente: {}", ex.getParameterName());
        return createErrorResponse(HttpStatus.BAD_REQUEST, -32602,
                "Missing required parameter: " + ex.getParameterName());
    }

    /**
     * Trata exceções de argumento inválido (parâmetros incorretos).
     *
     * @param ex exceção capturada
     * @return resposta HTTP 400 com formato JSON-RPC de erro
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("[handleIllegalArgument] Argumento inválido: {}", ex.getMessage());
        return createErrorResponse(HttpStatus.BAD_REQUEST, -32602, ex.getMessage());
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
        return createErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, -32601,
                "Method not found: " + ex.getMethod());
    }

    /**
     * Trata exceções de Content-Type não suportado.
     *
     * @param ex exceção capturada
     * @return resposta HTTP 415 com formato JSON-RPC de erro
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        log.warn("[handleMediaTypeNotSupported] Content-Type não suportado: {}", ex.getContentType());
        return createErrorResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, -32602,
                "Content-Type not supported: " + ex.getContentType());
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
        return createErrorResponse(HttpStatus.BAD_REQUEST, -32700,
                "Parse error: " + ex.getMessage());
    }

    /**
     * Trata exceções de Content-Type não aceitável (Accept header inválido).
     *
     * <p>Não faz nada para evitar interferir no handshake SSE.
     * O Spring gerencia isso automaticamente.</p>
     *
     * @param ex exceção capturada
     * @return null para permitir que o Spring lide com a exceção
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Map<String, Object>> handleMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex) {
        log.warn("[handleMediaTypeNotAcceptable] Accept header não suportado: {}", ex.getMessage());
        // Retorna null para que o Spring continue o processamento normal
        // e não interrompa o handshake SSE
        return null;
    }

    /**
     * Trata exceções genéricas não tratadas por outros handlers.
     *
     * @param ex exceção capturada
     * @return resposta HTTP 500 com formato JSON-RPC de erro
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        log.error("[handleGeneral] Erro não tratado. Tipo: {}, Mensagem: {}", ex.getClass().getName(), ex.getMessage(), ex);
        // Log adicional para capturar exceções durante o handshake SSE
        if (ex.getMessage() != null && ex.getMessage().contains("SseEmitter")) {
            log.error("[handleGeneral] Exceção relacionada ao SseEmitter detectada. Stack trace completo:", ex);
        }
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, -32603,
                ex.getMessage() != null ? ex.getMessage() : "Internal server error");
    }
}
