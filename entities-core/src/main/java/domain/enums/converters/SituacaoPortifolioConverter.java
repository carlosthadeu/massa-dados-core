package domain.enums.converters;

import domain.enums.Situacao;
import domain.enums.SituacaoPortifolio;

import javax.persistence.AttributeConverter;

/**
 * JPA {\@code AttributeConverter} para o enum {@link domain.enums.SituacaoPortifolio}.
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
public class SituacaoPortifolioConverter implements AttributeConverter<SituacaoPortifolio, Integer> {

    @Override
    public Integer convertToDatabaseColumn(SituacaoPortifolio attribute) {
        return attribute == null ? null : attribute.getCodigo();
    }

    @Override
    public SituacaoPortifolio convertToEntityAttribute(Integer dbData) {
        switch (dbData) {
            case 0:
                return SituacaoPortifolio.NONE;
            case 1:
                return SituacaoPortifolio.ELABORACAO;
            case 2:
                return SituacaoPortifolio.EXECUCAO;
            case 3:
                return SituacaoPortifolio.REPLANEJAMENTO;
            case 4:
                return SituacaoPortifolio.ENCERRAMENTO;
            case 5:
                return SituacaoPortifolio.ENCERRADO;
            default:
                throw new IllegalArgumentException(
                        "Valor armazenado no banco de dados nÃ£o atende a regra do enumerador:" + dbData);
        }
    }

}

