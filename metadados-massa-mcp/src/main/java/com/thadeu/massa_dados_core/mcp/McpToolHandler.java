package com.thadeu.massa_dados_core.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thadeu.massa_dados_core.mcp.dto.*;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;
import java.util.Map;

@Component
public class McpToolHandler {

    private final ObjectMapper objectMapper;
    private final McpEntityMetadataService metadataService;
    private final McpMapeamentoSemanticoService mapeamentoSemanticoService;
    private final McpConsultaService consultaService;
        private final McpDemandaService demandaService;
    private final EntityManager entityManager;

    public McpToolHandler(ObjectMapper objectMapper,
                          McpEntityMetadataService metadataService,
                          McpMapeamentoSemanticoService mapeamentoSemanticoService,
                          McpConsultaService consultaService,
                          McpDemandaService demandaService,
                          EntityManager entityManager) {
        this.objectMapper = objectMapper;
        this.metadataService = metadataService;
        this.mapeamentoSemanticoService = mapeamentoSemanticoService;
        this.consultaService = consultaService;
        this.demandaService = demandaService;
        this.entityManager = entityManager;
    }

    public ServerResponse handle(ServerRequest request) {
        try {
            JsonNode body = objectMapper.readTree(request.servletRequest().getInputStream());
            String method = body.has("method") ? body.get("method").asText() : "";
            JsonNode params = body.has("params") ? body.get("params") : objectMapper.nullNode();
            JsonNode id = body.has("id") ? body.get("id") : objectMapper.nullNode();

            return switch (method) {
                case "tools/call" -> handleToolCall(params, id);
                case "tools/list" -> handleToolsList(id);
                default -> errorResponse(-32601, "Method not found: " + method, id);
            };
        } catch (Exception e) {
            return errorResponse(-32603, "Internal error: " + e.getMessage(), null);
        }
    }

    private ServerResponse handleToolCall(JsonNode params, JsonNode id) {
        String toolName = params.has("name") ? params.get("name").asText() : "";
        JsonNode arguments = params.has("arguments") ? params.get("arguments") : objectMapper.nullNode();

        return switch (toolName) {
            case "get_entity_metadata" -> handleGetEntityMetadata(arguments, id);
            case "get_mapeamento_semantico" -> handleGetMapeamentoSemantico(arguments, id);
            case "consultar_dados" -> handleConsultarDados(arguments, id);
            case "listar_demandas" -> handleListarDemandas(arguments, id);
            case "resolver_demanda" -> handleResolverDemanda(arguments, id);
            case "detalhar_demanda" -> handleDetalharDemanda(arguments, id);
            case "criar_massa" -> handleCriarMassa(arguments, id);
            default -> errorResponse(-32602, "Unknown tool: " + toolName, id);
        };
    }

    private ServerResponse handleGetEntityMetadata(JsonNode arguments, JsonNode id) {
        try {
            EntityMetadataRequest request = objectMapper.treeToValue(arguments, EntityMetadataRequest.class);
            EntityMetadataResponse response = metadataService.getMetadata(request);
            return successResponse(response, id);
        } catch (Exception e) {
            return errorResponse(-32603, "Error getting metadata: " + e.getMessage(), id);
        }
    }

    private ServerResponse handleGetMapeamentoSemantico(JsonNode arguments, JsonNode id) {
        try {
            var response = Map.of(
                    "mapeamentoSemantico", mapeamentoSemanticoService.getMapeamentoSemanticoJson(),
                    "sinonimos", mapeamentoSemanticoService.getSinonimosJson()
            );
            return successResponse(response, id);
        } catch (Exception e) {
            return errorResponse(-32603, "Error getting mapeamento semantico: " + e.getMessage(), id);
        }
    }

    private ServerResponse handleConsultarDados(JsonNode arguments, JsonNode id) {
        try {
            ConsultaEstruturada consulta = objectMapper.treeToValue(arguments, ConsultaEstruturada.class);
            ConsultaResponse response = consultaService.consultar(consulta);
            return successResponse(response, id);
        } catch (Exception e) {
            return errorResponse(-32603, "Error consulting data: " + e.getMessage(), id);
        }
    }

    private ServerResponse handleListarDemandas(JsonNode arguments, JsonNode id) {
        try {
            var demandas = demandaService.listarDemandasPendentes();
            return successResponse(Map.of("demandas", demandas), id);
        } catch (Exception e) {
            return errorResponse(-32603, "Error listing demands: " + e.getMessage(), id);
        }
    }

    private ServerResponse handleResolverDemanda(JsonNode arguments, JsonNode id) {
        try {
            String demandaId = arguments.has("id") ? arguments.get("id").asText() : "";
            if (demandaId.isEmpty()) {
                return errorResponse(-32602, "Parâmetro 'id' é obrigatório", id);
            }
            boolean resolvida = demandaService.resolverDemanda(demandaId);
            if (resolvida) {
                return successResponse(Map.of("mensagem", "Demanda resolvida com sucesso"), id);
            } else {
                return errorResponse(-32603, "Demanda não encontrada: " + demandaId, id);
            }
        } catch (Exception e) {
            return errorResponse(-32603, "Error resolving demand: " + e.getMessage(), id);
        }
    }

    private ServerResponse handleDetalharDemanda(JsonNode arguments, JsonNode id) {
        try {
            String demandaId = arguments.has("id") ? arguments.get("id").asText() : "";
            if (demandaId.isEmpty()) {
                return errorResponse(-32602, "Parâmetro 'id' é obrigatório", id);
            }
            var detalhes = demandaService.detalharDemanda(demandaId);
            if (detalhes != null) {
                return successResponse(detalhes, id);
            } else {
                return errorResponse(-32603, "Demanda não encontrada: " + demandaId, id);
            }
        } catch (Exception e) {
            return errorResponse(-32603, "Error detailing demand: " + e.getMessage(), id);
        }
    }

    private ServerResponse handleCriarMassa(JsonNode arguments, JsonNode id) {
        try {
            // Recebe JSON de criação de massa
            JsonNode entidades = arguments.get("entidades");
            if (entidades == null || !entidades.isArray()) {
                return errorResponse(-32602, "Parâmetro 'entidades' é obrigatório e deve ser um array", id);
            }

            // Para cada entidade, criar via JPA
            List<Map<String, Object>> resultados = new java.util.ArrayList<>();
            for (JsonNode entidade : entidades) {
                String tipo = entidade.has("tipo") ? entidade.get("tipo").asText() : "";
                JsonNode dados = entidade.has("dados") ? entidade.get("dados") : objectMapper.nullNode();
                JsonNode filhos = entidade.has("filhos") ? entidade.get("filhos") : objectMapper.nullNode();

                // Obter classe Entity correspondente
                var mapeamento = mapeamentoSemanticoService.getMapeamentoSemantico();
                var entidadesMap = mapeamento.get("entidades");
                if (entidadesMap == null || !entidadesMap.has(tipo)) {
                    demandaService.gerarDemanda(
                            "Criação de massa para entidade '" + tipo + "'",
                            "Entidade '" + tipo + "' não encontrada no mapeamento semântico.",
                            "Adicionar mapeamento para '" + tipo + "' no mapeamento-semantico.json",
                            "Dados recebidos: " + dados.toString()
                    );
                    resultados.add(Map.of(
                            "tipo", tipo,
                            "status", "erro",
                            "mensagem", "Entidade não mapeada. Demanda gerada."
                    ));
                    continue;
                }

                String className = entidadesMap.get(tipo).get("classe").asText();
                Class<?> entityClass;
                try {
                    entityClass = Class.forName(className);
                } catch (ClassNotFoundException e) {
                    resultados.add(Map.of(
                            "tipo", tipo,
                            "status", "erro",
                            "mensagem", "Classe não encontrada: " + className
                    ));
                    continue;
                }

                // Criar instância via reflection
                Object entity = entityClass.getDeclaredConstructor().newInstance();

                // Preencher atributos
                var atributos = entidadesMap.get(tipo).get("atributos");
                if (atributos != null && dados != null) {
                    for (var attr : dados.properties()) {
                        String attrName = attr.getKey();
                        JsonNode attrValue = attr.getValue();

                        if (atributos.has(attrName)) {
                            var attrDef = atributos.get(attrName);
                            String coluna = attrDef.has("coluna") ? attrDef.get("coluna").asText() : "";
                            String relacionamento = attrDef.has("relacionamento") ? attrDef.get("relacionamento").asText() : "";

                            if (!relacionamento.isEmpty()) {
                                // É um relacionamento - precisa buscar a entidade referenciada
                                // Por simplicidade, ignoramos por enquanto
                                continue;
                            }

                            // Encontrar campo na classe
                            try {
                                java.lang.reflect.Field field = entityClass.getDeclaredField(attrName);
                                field.setAccessible(true);

                                // Converter valor
                                Object valor = converterValorParaCampo(field, attrValue);
                                field.set(entity, valor);
                            } catch (NoSuchFieldException e) {
                                // Tentar pelo nome da coluna
                                for (java.lang.reflect.Field f : entityClass.getDeclaredFields()) {
                                    if (f.isAnnotationPresent(jakarta.persistence.Column.class)) {
                                        jakarta.persistence.Column col = f.getAnnotation(jakarta.persistence.Column.class);
                                        if (col.name().equals(coluna)) {
                                            f.setAccessible(true);
                                            Object valor = converterValorParaCampo(f, attrValue);
                                            f.set(entity, valor);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Persistir via EntityManager
                entityManager.persist(entity);

                resultados.add(Map.of(
                        "tipo", tipo,
                        "status", "sucesso",
                        "id", Map.of("id", java.util.Optional.ofNullable(
                                entity.getClass().getMethod("getSq_aco_ett").invoke(entity)
                        ).orElse(""))
                ));
            }

            return successResponse(Map.of("resultados", resultados), id);
        } catch (Exception e) {
            return errorResponse(-32603, "Error creating mass data: " + e.getMessage(), id);
        }
    }

    private Object converterValorParaCampo(java.lang.reflect.Field field, JsonNode value) {
        Class<?> type = field.getType();
        if (type == String.class) {
            return value.asText();
        } else if (type == Integer.class || type == int.class) {
            return value.asInt();
        } else if (type == Long.class || type == long.class) {
            return value.asLong();
        } else if (type == Double.class || type == double.class) {
            return value.asDouble();
        } else if (type == Boolean.class || type == boolean.class) {
            return value.asBoolean();
        } else if (type == java.math.BigDecimal.class) {
            return java.math.BigDecimal.valueOf(value.asDouble());
        } else if (type == java.time.LocalDate.class) {
            return java.time.LocalDate.parse(value.asText());
        } else if (type == java.time.LocalDateTime.class) {
            return java.time.LocalDateTime.parse(value.asText());
        }
        return value.asText();
    }

    private ServerResponse handleToolsList(JsonNode id) {
            var tools = java.util.List.of(
                    Map.of(
                            "name", "get_entity_metadata",
                            "description", "Retorna metadados de uma classe Entity JPA (nome da tabela, atributos, relacionamentos)",
                            "inputSchema", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "className", Map.of(
                                                    "type", "string",
                                                    "description", "Nome completo da classe (ex: br.gov.bnb.domain.entity.Portfolio)"
                                            )
                                    ),
                                    "required", java.util.List.of("className")
                            )
                    ),
                    Map.of(
                            "name", "get_mapeamento_semantico",
                            "description", "Retorna o mapeamento semântico completo (entidades, atributos, valores de enum, sinônimos) para interpretação de comandos em linguagem natural",
                            "inputSchema", Map.of(
                                    "type", "object",
                                    "properties", Map.of(),
                                    "required", java.util.List.of()
                            )
                    ),
                    Map.of(
                            "name", "consultar_dados",
                            "description", "Executa uma consulta estruturada no banco de dados e retorna os resultados",
                            "inputSchema", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "comando", Map.of(
                                                    "type", "string",
                                                    "description", "Tipo de comando: 'consultar'"
                                            ),
                                            "entidade", Map.of(
                                                    "type", "string",
                                                    "description", "Nome da entidade no mapeamento semântico (ex: acao_estrategica, portfolio)"
                                            ),
                                            "filtros", Map.of(
                                                    "type", "array",
                                                    "description", "Lista de filtros",
                                                    "items", Map.of(
                                                            "type", "object",
                                                            "properties", Map.of(
                                                                    "atributo", Map.of("type", "string"),
                                                                    "operador", Map.of("type", "string"),
                                                                    "valor", Map.of("type", "string")
                                                            )
                                                    )
                                            ),
                                            "agregacao", Map.of(
                                                    "type", "string",
                                                    "description", "Tipo de agregação: count, sum, avg, min, max"
                                            ),
                                            "atributoAgregacao", Map.of(
                                                    "type", "string",
                                                    "description", "Atributo para agregação"
                                            ),
                                            "ordenacao", Map.of(
                                                    "type", "object",
                                                    "properties", Map.of(
                                                            "atributo", Map.of("type", "string"),
                                                            "direcao", Map.of("type", "string")
                                                    )
                                            ),
                                            "limite", Map.of(
                                                    "type", "integer",
                                                    "description", "Limite de resultados"
                                            )
                                    ),
                                    "required", java.util.List.of("comando", "entidade")
                            )
                    ),
                    Map.of(
                            "name", "listar_demandas",
                            "description", "Lista as demandas pendentes de configuração",
                            "inputSchema", Map.of(
                                    "type", "object",
                                    "properties", Map.of(),
                                    "required", java.util.List.of()
                            )
                    ),
                    Map.of(
                            "name", "resolver_demanda",
                            "description", "Marca uma demanda como resolvida",
                            "inputSchema", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "id", Map.of(
                                                    "type", "string",
                                                    "description", "ID da demanda (nome do arquivo)"
                                            )
                                    ),
                                    "required", java.util.List.of("id")
                            )
                    ),
                    Map.of(
                            "name", "detalhar_demanda",
                            "description", "Retorna detalhes completos de uma demanda específica",
                            "inputSchema", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "id", Map.of(
                                                    "type", "string",
                                                    "description", "ID da demanda (nome do arquivo)"
                                            )
                                    ),
                                    "required", java.util.List.of("id")
                            )
                    ),
                    Map.of(
                            "name", "criar_massa",
                            "description", "Cria massa de dados no banco a partir de um JSON estruturado com entidades, atributos e relacionamentos",
                            "inputSchema", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "entidades", Map.of(
                                                    "type", "array",
                                                    "description", "Lista de entidades a serem criadas",
                                                    "items", Map.of(
                                                            "type", "object",
                                                            "properties", Map.of(
                                                                    "tipo", Map.of("type", "string", "description", "Tipo da entidade (ex: portfolio, acao_estrategica)"),
                                                                    "dados", Map.of("type", "object", "description", "Atributos da entidade"),
                                                                    "filhos", Map.of("type", "array", "description", "Entidades filhas (ex: etapas dentro de ação)")
                                                            )
                                                    )
                                            )
                                    ),
                                    "required", java.util.List.of("entidades")
                            )
                    )
            );

            // tools/list precisa retornar result.tools DIRETAMENTE (sem content wrapper)
            var body = Map.of(
                    "jsonrpc", "2.0",
                    "result", Map.of("tools", tools),
                    "id", id
            );
            return ServerResponse.ok().body(body);
        }

    private ServerResponse successResponse(Object result, JsonNode id) {
        var body = Map.of(
                "jsonrpc", "2.0",
                "result", Map.of("content", java.util.List.of(Map.of("type", "text", "text", toJsonString(result)))),
                "id", id
        );
        return ServerResponse.ok().body(body);
    }

    private ServerResponse errorResponse(int code, String message, JsonNode id) {
        var body = Map.of(
                "jsonrpc", "2.0",
                "error", Map.of("code", code, "message", message),
                "id", id
        );
        return ServerResponse.status(400).body(body);
    }

    private String toJsonString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{\"error\":\"serialization failed\"}";
        }
    }
}
