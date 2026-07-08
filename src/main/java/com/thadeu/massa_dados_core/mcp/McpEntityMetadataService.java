package com.thadeu.massa_dados_core.mcp;

import com.thadeu.massa_dados_core.mcp.dto.EntityMetadataRequest;
import com.thadeu.massa_dados_core.mcp.dto.EntityMetadataResponse;
import com.thadeu.massa_dados_core.mcp.dto.EntityMetadataResponse.AttributeInfo;
import com.thadeu.massa_dados_core.mcp.dto.EntityMetadataResponse.RelationshipInfo;
import jakarta.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviço responsável por extrair metadados de entidades JPA via reflection.
 *
 * <p>Para uma classe informada, carrega a classe, verifica se possui {@code @Entity},
 * extrai {@code @Table}, atributos e relacionamentos.
 */
@Service
public class McpEntityMetadataService {

    private static final Logger log = LoggerFactory.getLogger(McpEntityMetadataService.class);

    /**
     * Obtém os metadados de uma entidade JPA.
     *
     * @param request DTO contendo o nome completo da classe
     * @return DTO com os metadados da entidade
     * @throws IllegalArgumentException se a classe não for encontrada ou não for uma entidade JPA
     */
    public EntityMetadataResponse getEntityMetadata(EntityMetadataRequest request) {
        String className = request.className();

        Class<?> clazz;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Classe não encontrada: " + className, e);
        }

        // Verificar se possui @Entity
        if (!clazz.isAnnotationPresent(Entity.class)) {
            throw new IllegalArgumentException("A classe " + className + " não possui @Entity");
        }

        // Extrair @Table
        String tableName = extractTableName(clazz);

        // Extrair atributos e relacionamentos
        List<AttributeInfo> attributes = new ArrayList<>();
        List<RelationshipInfo> relationships = new ArrayList<>();

        for (Field field : clazz.getDeclaredFields()) {
            if (isRelationshipField(field)) {
                relationships.add(buildRelationshipInfo(field));
            } else {
                attributes.add(buildAttributeInfo(field));
            }
        }

        return new EntityMetadataResponse(className, tableName, attributes, relationships);
    }

    /**
     * Extrai o nome da tabela a partir da anotação {@code @Table}.
     */
    private String extractTableName(Class<?> clazz) {
        Table tableAnnotation = clazz.getAnnotation(Table.class);
        if (tableAnnotation != null && !tableAnnotation.name().isEmpty()) {
            return tableAnnotation.name();
        }
        // Se não houver @Table, usa o nome da classe em snake_case
        return toSnakeCase(clazz.getSimpleName());
    }

    /**
     * Verifica se o campo possui anotação de relacionamento JPA.
     */
    private boolean isRelationshipField(Field field) {
        return field.isAnnotationPresent(OneToOne.class)
                || field.isAnnotationPresent(OneToMany.class)
                || field.isAnnotationPresent(ManyToOne.class)
                || field.isAnnotationPresent(ManyToMany.class);
    }

    /**
     * Constrói um {@link AttributeInfo} a partir de um campo.
     */
    private AttributeInfo buildAttributeInfo(Field field) {
        String name = field.getName();
        String javaType = field.getType().getSimpleName();
        boolean nullable = isNullable(field);
        boolean primaryKey = field.isAnnotationPresent(Id.class);
        String columnName = extractColumnName(field);
        return new AttributeInfo(name, javaType, nullable, primaryKey, columnName);
    }

    /**
     * Constrói um {@link RelationshipInfo} a partir de um campo de relacionamento.
     */
    private RelationshipInfo buildRelationshipInfo(Field field) {
        String name = field.getName();
        String targetEntity = field.getType().getSimpleName();
        String relationshipType = extractRelationshipType(field);
        String joinColumn = extractJoinColumn(field);
        return new RelationshipInfo(name, targetEntity, relationshipType, joinColumn);
    }

    /**
     * Verifica se o campo é nullable (por padrão true, a menos que @Column(nullable=false)).
     */
    private boolean isNullable(Field field) {
        Column column = field.getAnnotation(Column.class);
        if (column != null) {
            return column.nullable();
        }
        return true;
    }

    /**
     * Extrai o nome da coluna a partir de @Column(name).
     */
    private String extractColumnName(Field field) {
        Column column = field.getAnnotation(Column.class);
        if (column != null && !column.name().isEmpty()) {
            return column.name();
        }
        // Se não houver @Column, converte o nome do campo para snake_case
        return toSnakeCase(field.getName());
    }

    /**
     * Extrai o tipo de relacionamento como string.
     */
    private String extractRelationshipType(Field field) {
        if (field.isAnnotationPresent(OneToOne.class)) return "OneToOne";
        if (field.isAnnotationPresent(OneToMany.class)) return "OneToMany";
        if (field.isAnnotationPresent(ManyToOne.class)) return "ManyToOne";
        if (field.isAnnotationPresent(ManyToMany.class)) return "ManyToMany";
        return "Unknown";
    }

    /**
     * Extrai o nome da coluna de junção a partir de @JoinColumn(name).
     */
    private String extractJoinColumn(Field field) {
        JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
        if (joinColumn != null && !joinColumn.name().isEmpty()) {
            return joinColumn.name();
        }
        return "";
    }

    /**
     * Converte um nome camelCase para snake_case.
     */
    private String toSnakeCase(String camelCase) {
        StringBuilder sb = new StringBuilder();
        for (char c : camelCase.toCharArray()) {
            if (Character.isUpperCase(c)) {
                if (sb.length() > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
