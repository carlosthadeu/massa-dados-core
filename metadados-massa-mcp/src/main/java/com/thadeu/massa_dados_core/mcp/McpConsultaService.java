package com.thadeu.massa_dados_core.mcp;

import com.thadeu.massa_dados_core.mcp.dto.*;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class McpConsultaService {

    @PersistenceContext
    private EntityManager entityManager;

    private final McpMapeamentoSemanticoService mapeamentoSemanticoService;

    public McpConsultaService(McpMapeamentoSemanticoService mapeamentoSemanticoService) {
        this.mapeamentoSemanticoService = mapeamentoSemanticoService;
    }

    private final McpDemandaService demandaService;

    public McpConsultaService(McpMapeamentoSemanticoService mapeamentoSemanticoService,
                               McpDemandaService demandaService) {
        this.mapeamentoSemanticoService = mapeamentoSemanticoService;
        this.demandaService = demandaService;
    }

    public ConsultaResponse consultar(ConsultaEstruturada consulta) {
        try {
            // 1. Obter a classe Entity correspondente à entidade
            Class<?> entityClass = getEntityClass(consulta.entidade());
            if (entityClass == null) {
                // Gerar demanda automaticamente
                demandaService.gerarDemanda(
                        "Consulta para entidade '" + consulta.entidade() + "'",
                        "Entidade '" + consulta.entidade() + "' não encontrada no mapeamento semântico.",
                        "Adicionar mapeamento para '" + consulta.entidade() + "' no mapeamento-semantico.json ou criar classe Entity via ddl_to_entity",
                        "Consulta completa: " + consulta.toString()
                );
                return new ConsultaResponse(
                        "Entidade '" + consulta.entidade() + "' não encontrada no mapeamento semântico. Uma demanda foi gerada para o técnico.",
                        List.of(),
                        null
                );
            }

            // 2. Construir Criteria Query
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<?> query = cb.createQuery();
            Root<?> root = query.from(entityClass);

            // 3. Aplicar filtros
            List<Predicate> predicates = new ArrayList<>();
            if (consulta.filtros() != null) {
                for (Filtro filtro : consulta.filtros()) {
                    Path<?> path = getPath(root, filtro.atributo());
                    if (path == null) {
                        // Gerar demanda automaticamente
                        demandaService.gerarDemanda(
                                "Consulta com atributo '" + filtro.atributo() + "' na entidade '" + consulta.entidade() + "'",
                                "Atributo '" + filtro.atributo() + "' não encontrado na entidade '" + consulta.entidade() + "'.",
                                "Adicionar atributo '" + filtro.atributo() + "' no mapeamento-semantico.json para a entidade '" + consulta.entidade() + "'",
                                "Filtro completo: " + filtro.toString()
                        );
                        return new ConsultaResponse(
                                "Atributo '" + filtro.atributo() + "' não encontrado na entidade '" + consulta.entidade() + "'. Uma demanda foi gerada para o técnico.",
                                List.of(),
                                null
                        );
                    }
                    Object valorConvertido = converterValor(consulta.entidade(), filtro.atributo(), filtro.valor());
                    Predicate predicate = montarPredicate(cb, path, filtro.operador(), valorConvertido);
                    if (predicate != null) {
                        predicates.add(predicate);
                    }
                }
            }
            query.where(predicates.toArray(new Predicate[0]));

            // 4. Aplicar agregação
            if (consulta.agregacao() != null && !consulta.agregacao().isEmpty()) {
                Path<?> atributoAgregacao = getPath(root, consulta.atributoAgregacao());
                if (atributoAgregacao == null) {
                    // Gerar demanda automaticamente
                    demandaService.gerarDemanda(
                            "Consulta com agregação no atributo '" + consulta.atributoAgregacao() + "' na entidade '" + consulta.entidade() + "'",
                            "Atributo de agregação '" + consulta.atributoAgregacao() + "' não encontrado.",
                            "Adicionar atributo '" + consulta.atributoAgregacao() + "' no mapeamento-semantico.json para a entidade '" + consulta.entidade() + "'",
                            "Consulta completa: " + consulta.toString()
                    );
                    return new ConsultaResponse(
                            "Atributo de agregação '" + consulta.atributoAgregacao() + "' não encontrado. Uma demanda foi gerada para o técnico.",
                            List.of(),
                            null
                    );
                }
                Expression<?> expressao = montarAgregacao(cb, consulta.agregacao(), atributoAgregacao);
                query.select((Selection<?>) expressao);
            } else {
                query.select(root);
            }

            // 5. Aplicar ordenação
            if (consulta.ordenacao() != null) {
                Path<?> orderPath = getPath(root, consulta.ordenacao().atributo());
                if (orderPath != null) {
                    if ("desc".equalsIgnoreCase(consulta.ordenacao().direcao())) {
                        query.orderBy(cb.desc((Expression<?>) orderPath));
                    } else {
                        query.orderBy(cb.asc((Expression<?>) orderPath));
                    }
                }
            }

            // 6. Executar
            TypedQuery<?> typedQuery = entityManager.createQuery(query);
            if (consulta.limite() != null) {
                typedQuery.setMaxResults(consulta.limite());
            }

            String sqlGerado = typedQuery.unwrap(org.hibernate.query.Query.class).getQueryString();

            Object resultado;
            if (consulta.agregacao() != null && !consulta.agregacao().isEmpty()) {
                resultado = typedQuery.getSingleResult();
            } else {
                List<?> lista = typedQuery.getResultList();
                resultado = lista;
            }

            // 7. Formatar resposta
            return formatarResposta(consulta, resultado, sqlGerado);

        } catch (Exception e) {
            return new ConsultaResponse(
                    "Erro ao processar consulta: " + e.getMessage(),
                    List.of(),
                    null
            );
        }
    }

    private Class<?> getEntityClass(String entidade) {
        var mapeamento = mapeamentoSemanticoService.getMapeamentoSemantico();
        var entidades = mapeamento.get("entidades");
        if (entidades == null || !entidades.has(entidade)) {
            return null;
        }
        String className = entidades.get(entidade).get("classe").asText();
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private Path<?> getPath(Root<?> root, String atributo) {
        // Suporta atributos aninhados (ex: "portfolio.nome")
        String[] parts = atributo.split("\\.");
        Path<?> path = root;
        for (String part : parts) {
            try {
                path = path.get(part);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return path;
    }

    private Object converterValor(String entidade, String atributo, Object valor) {
        if (valor == null) return null;

        var mapeamento = mapeamentoSemanticoService.getMapeamentoSemantico();
        var entidades = mapeamento.get("entidades");
        if (entidades == null || !entidades.has(entidade)) return valor;

        var atributos = entidades.get(entidade).get("atributos");
        if (atributos == null || !atributos.has(atributo)) return valor;

        var attr = atributos.get(atributo);
        if (attr.has("mapeamento")) {
            var mapeamentoAttr = attr.get("mapeamento");
            // Procurar valor pelo nome ou sinônimos
            String valorStr = valor.toString();
            for (var entry : mapeamentoAttr.properties()) {
                var entryValue = entry.getValue();
                if (entryValue.has("sinonimos")) {
                    for (var sin : entryValue.get("sinonimos")) {
                        if (sin.asText().equalsIgnoreCase(valorStr)) {
                            return entryValue.get("valor").asInt();
                        }
                    }
                }
                if (entry.getKey().equalsIgnoreCase(valorStr)) {
                    return entryValue.get("valor").asInt();
                }
            }
        }

        return valor;
    }

    private Predicate montarPredicate(CriteriaBuilder cb, Path<?> path, String operador, Object valor) {
        if (valor == null && !"IS NULL".equalsIgnoreCase(operador) && !"IS NOT NULL".equalsIgnoreCase(operador)) {
            return null;
        }

        return switch (operador.toUpperCase()) {
            case "=" -> cb.equal(path, valor);
            case "!=" -> cb.notEqual(path, valor);
            case ">" -> cb.greaterThan((Path<Comparable>) path, (Comparable) valor);
            case ">=" -> cb.greaterThanOrEqualTo((Path<Comparable>) path, (Comparable) valor);
            case "<" -> cb.lessThan((Path<Comparable>) path, (Comparable) valor);
            case "<=" -> cb.lessThanOrEqualTo((Path<Comparable>) path, (Comparable) valor);
            case "LIKE" -> cb.like((Path<String>) path, "%" + valor + "%");
            case "IN" -> {
                if (valor instanceof List<?> lista) {
                    yield path.in(lista);
                }
                yield path.in(valor);
            }
            case "BETWEEN" -> {
                if (valor instanceof List<?> range && range.size() == 2) {
                    yield cb.between((Path<Comparable>) path, (Comparable) range.get(0), (Comparable) range.get(1));
                }
                yield null;
            }
            case "IS NULL" -> cb.isNull(path);
            case "IS NOT NULL" -> cb.isNotNull(path);
            default -> null;
        };
    }

    private Expression<?> montarAgregacao(CriteriaBuilder cb, String agregacao, Path<?> path) {
        return switch (agregacao.toUpperCase()) {
            case "COUNT" -> cb.count(path);
            case "SUM" -> cb.sum((Path<Number>) path);
            case "AVG" -> cb.avg((Path<Number>) path);
            case "MIN" -> cb.min((Path<Comparable>) path);
            case "MAX" -> cb.max((Path<Comparable>) path);
            default -> path;
        };
    }

    private ConsultaResponse formatarResposta(ConsultaEstruturada consulta, Object resultado, String sqlGerado) {
        if (consulta.agregacao() != null && !consulta.agregacao().isEmpty()) {
            String resposta = switch (consulta.agregacao().toUpperCase()) {
                case "COUNT" -> "Total: " + resultado;
                case "SUM" -> "Soma: " + resultado;
                case "AVG" -> "Média: " + resultado;
                case "MIN" -> "Mínimo: " + resultado;
                case "MAX" -> "Máximo: " + resultado;
                default -> "Resultado: " + resultado;
            };
            return new ConsultaResponse(resposta, List.of(), sqlGerado);
        }

        if (resultado instanceof List<?> lista) {
            List<Map<String, Object>> dados = lista.stream()
                    .map(item -> {
                        Map<String, Object> map = new LinkedHashMap<>();
                        for (Field field : item.getClass().getDeclaredFields()) {
                            field.setAccessible(true);
                            try {
                                map.put(field.getName(), field.get(item));
                            } catch (IllegalAccessException e) {
                                map.put(field.getName(), null);
                            }
                        }
                        return map;
                    })
                    .collect(Collectors.toList());

            String resposta = "Encontrados " + dados.size() + " registro(s).";
            return new ConsultaResponse(resposta, dados, sqlGerado);
        }

        return new ConsultaResponse("Resultado: " + resultado, List.of(), sqlGerado);
    }
}
