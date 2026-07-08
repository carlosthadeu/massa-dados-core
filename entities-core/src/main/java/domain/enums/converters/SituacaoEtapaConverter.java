package domain.enums.converters;

import domain.enums.Situacao;
import domain.enums.SituacaoEtapa;

import javax.persistence.AttributeConverter;

/**
 * JPA {\@code AttributeConverter} para o enum {@link domain.enums.SituacaoEtapa}.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Converter o enum para coluna do banco ({@code Integer}).</li>
 *   <li>Converter o valor do banco de volta para o enum.</li>
 * </ul>
 *
 * @author Thadeu Garrido
 * @version 1.0
 */
public class SituacaoEtapaConverter implements AttributeConverter<SituacaoEtapa, Integer> {

    @Override
    public Integer convertToDatabaseColumn(SituacaoEtapa attribute) {
        return attribute == null ? null : Integer.valueOf(attribute.getCodigo());
    }

    @Override
    public SituacaoEtapa convertToEntityAttribute(Integer dbData) {
        switch (dbData) {
            case 0:
                return SituacaoEtapa.NONE;
            case 1:
                return SituacaoEtapa.INICIADA;
            case 2:
                return SituacaoEtapa.EM_ANDAMENTO;
            case 3:
                return SituacaoEtapa.ATRASADA;
            case 4:
                return SituacaoEtapa.NAO_CONCLUIDA;
            case 5:
                return SituacaoEtapa.CONCLUIDA_ATRASO;
            case 6:
                return SituacaoEtapa.CONCLUIDA;
            default:
                throw new IllegalArgumentException(
                        "Valor armazenado no banco de dados nÃ£o atende a regra do enumerador:" + dbData);
        }
    }

}

