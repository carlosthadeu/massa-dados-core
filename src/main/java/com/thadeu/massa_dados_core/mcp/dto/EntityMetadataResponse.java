package com.thadeu.massa_dados_core.mcp.dto;

import java.util.List;

/**
 * DTO de response contendo os metadados de uma entidade JPA.
 *
 * @param className     Nome completo da classe
 * @param tableName     Nome da tabela (anotação @Table)
 * @param attributes    Lista de atributos da entidade
 * @param relationships Lista de relacionamentos da entidade
 */
public record EntityMetadataResponse(
        String className,
        String tableName,
        List<AttributeInfo> attributes,
        List<RelationshipInfo> relationships
) {

    /**
     * Informações de um atributo da entidade.
     *
     * @param name       Nome do campo
     * @param javaType   Tipo Java do campo
     * @param nullable   Se o campo pode ser nulo
     * @param primaryKey Se o campo é chave primária
     * @param columnName Nome da coluna (anotação @Column)
     */
    public record AttributeInfo(
            String name,
            String javaType,
            boolean nullable,
            boolean primaryKey,
            String columnName
    ) {}

    /**
     * Informações de um relacionamento da entidade.
     *
     * @param name          Nome do campo
     * @param targetEntity  Nome da classe alvo do relacionamento
     * @param relationshipType Tipo do relacionamento (ManyToOne, OneToMany, etc.)
     * @param joinColumn    Nome da coluna de junção (se aplicável)
     */
    public record RelationshipInfo(
            String name,
            String targetEntity,
            String relationshipType,
            String joinColumn
    ) {}
}
