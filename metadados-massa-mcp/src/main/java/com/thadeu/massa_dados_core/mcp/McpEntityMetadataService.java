package com.thadeu.massa_dados_core.mcp;

import com.thadeu.massa_dados_core.mcp.dto.AttributeInfo;
import com.thadeu.massa_dados_core.mcp.dto.EntityMetadataRequest;
import com.thadeu.massa_dados_core.mcp.dto.EntityMetadataResponse;
import com.thadeu.massa_dados_core.mcp.dto.RelationshipInfo;
import jakarta.persistence.*;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Service
public class McpEntityMetadataService {

    public EntityMetadataResponse getMetadata(EntityMetadataRequest request) throws ClassNotFoundException {
        Class<?> entityClass = Class.forName(request.className());

        // Verificar se tem @Entity
        if (!entityClass.isAnnotationPresent(jakarta.persistence.Entity.class)) {
            throw new IllegalArgumentException("Classe " + request.className() + " não possui @Entity");
        }

        // Extrair @Table
        String tableName = "";
        if (entityClass.isAnnotationPresent(Table.class)) {
            tableName = entityClass.getAnnotation(Table.class).name();
        }

        // Extrair atributos
        List<AttributeInfo> attributes = new ArrayList<>();
        List<RelationshipInfo> relationships = new ArrayList<>();

        for (Field field : entityClass.getDeclaredFields()) {
            String fieldName = field.getName();
            String fieldType = field.getType().getSimpleName();

            // Anotações JPA
            List<String> annotations = new ArrayList<>();
            boolean isId = field.isAnnotationPresent(Id.class);
            boolean isGeneratedValue = field.isAnnotationPresent(GeneratedValue.class);
            boolean nullable = true;
            String columnName = fieldName;

            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);
                columnName = column.name().isEmpty() ? fieldName : column.name();
                nullable = column.nullable();
                annotations.add("@Column");
            }

            if (isId) annotations.add("@Id");
            if (isGeneratedValue) annotations.add("@GeneratedValue");

            // Relacionamentos
            if (field.isAnnotationPresent(ManyToOne.class)) {
                ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
                String joinColumn = "";
                if (field.isAnnotationPresent(JoinColumn.class)) {
                    joinColumn = field.getAnnotation(JoinColumn.class).name();
                }
                relationships.add(new RelationshipInfo(
                        fieldName,
                        fieldType,
                        "",
                        joinColumn,
                        "ManyToOne"
                ));
                annotations.add("@ManyToOne");
                continue; // Não adiciona como atributo simples
            }

            if (field.isAnnotationPresent(OneToMany.class)) {
                OneToMany oneToMany = field.getAnnotation(OneToMany.class);
                relationships.add(new RelationshipInfo(
                        fieldName,
                        fieldType,
                        oneToMany.mappedBy(),
                        "",
                        "OneToMany"
                ));
                annotations.add("@OneToMany");
                continue;
            }

            if (field.isAnnotationPresent(OneToOne.class)) {
                OneToOne oneToOne = field.getAnnotation(OneToOne.class);
                String joinColumn = "";
                if (field.isAnnotationPresent(JoinColumn.class)) {
                    joinColumn = field.getAnnotation(JoinColumn.class).name();
                }
                relationships.add(new RelationshipInfo(
                        fieldName,
                        fieldType,
                        oneToOne.mappedBy(),
                        joinColumn,
                        "OneToOne"
                ));
                annotations.add("@OneToOne");
                continue;
            }

            if (field.isAnnotationPresent(ManyToMany.class)) {
                ManyToMany manyToMany = field.getAnnotation(ManyToMany.class);
                relationships.add(new RelationshipInfo(
                        fieldName,
                        fieldType,
                        manyToMany.mappedBy(),
                        "",
                        "ManyToMany"
                ));
                annotations.add("@ManyToMany");
                continue;
            }

            // Atributo simples
            attributes.add(new AttributeInfo(
                    fieldName,
                    fieldType,
                    nullable,
                    isId,
                    isGeneratedValue,
                    columnName,
                    null, null, null,
                    annotations
            ));
        }

        return new EntityMetadataResponse(
                entityClass.getSimpleName(),
                tableName,
                attributes,
                relationships
        );
    }
}
