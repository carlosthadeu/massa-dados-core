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

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ResponseEntity<Map<String, Object>> createErrorResponse(HttpStatus status, int code, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("jsonrpc", "2.0");
        body.put("error", Map.of("code", code, "message", message));
        body.put("id", null);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return new ResponseEntity<>(body, headers, status);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(MissingServletRequestParameterException ex) {
        log.warn("[handleMissingParam] Parâmetro obrigatório ausente: {}", ex.getParameterName());
        return createErrorResponse(HttpStatus.BAD_REQUEST, -32602,
                "Missing required parameter: " + ex.getParameterName());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("[handleIllegalArgument] Argumento inválido: {}", ex.getMessage());
        return createErrorResponse(HttpStatus.BAD_REQUEST, -32602, ex.getMessage());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("[handleMethodNotSupported] Método HTTP não suportado: {}", ex.getMethod());
        return createErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, -32601,
                "Method not found: " + ex.getMethod());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        log.warn("[handleMediaTypeNotSupported] Content-Type não suportado: {}", ex.getContentType());
        return createErrorResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, -32602,
                "Content-Type not supported: " + ex.getContentType());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("[handleMessageNotReadable] Corpo da requisição inválido: {}", ex.getMessage());
        return createErrorResponse(HttpStatus.BAD_REQUEST, -32700,
                "Parse error: " + ex.getMessage());
    }

    /**
     * Trata exceções de Content-Type não aceitável.
     * Para SSE, retorna text/event-stream.
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<String> handleMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex) {
        log.warn("[handleMediaTypeNotAcceptable] Accept header não suportado: {}", ex.getMessage());
        
        // Para SSE, retorna texto simples com Content-Type text/event-stream
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_EVENT_STREAM);
        headers.setCacheControl("no-cache");
        
        return new ResponseEntity<>("", headers, HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        log.error("[handleGeneral] Erro não tratado. Tipo: {}, Mensagem: {}", ex.getClass().getName(), ex.getMessage(), ex);
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, -32603,
                ex.getMessage() != null ? ex.getMessage() : "Internal server error");
    }
}