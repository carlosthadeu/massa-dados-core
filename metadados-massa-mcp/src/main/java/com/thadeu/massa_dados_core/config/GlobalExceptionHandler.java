package com.thadeu.massa_dados_core.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "jsonrpc", "2.0",
                "error", Map.of("code", -32602, "message", ex.getMessage()),
                "id", null
        ));
    }

    @ExceptionHandler(ClassNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleClassNotFound(ClassNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "jsonrpc", "2.0",
                "error", Map.of("code", -32603, "message", "Classe não encontrada: " + ex.getMessage()),
                "id", null
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "jsonrpc", "2.0",
                "error", Map.of("code", -32603, "message", ex.getMessage()),
                "id", null
        ));
    }
}
