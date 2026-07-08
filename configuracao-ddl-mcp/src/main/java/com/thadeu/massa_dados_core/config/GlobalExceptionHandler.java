package com.thadeu.massa_dados_core.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    /**
     * Trata exceções de argumento inválido (parâmetros incorretos).
     *
     * @param ex exceção capturada
     * @return resposta HTTP 400 com formato JSON-RPC de erro
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "jsonrpc", "2.0",
                "error", Map.of("code", -32602, "message", ex.getMessage()),
                "id", null
        ));
    }

    /**
     * Trata exceções genéricas não tratadas por outros handlers.
     *
     * @param ex exceção capturada
     * @return resposta HTTP 500 com formato JSON-RPC de erro
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "jsonrpc", "2.0",
                "error", Map.of("code", -32603, "message", ex.getMessage()),
                "id", null
        ));
    }
}
