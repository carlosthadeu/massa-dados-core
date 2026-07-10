package domain.enums.converters;

import jakarta.persistence.AttributeConverter;

import domain.enums.SituacaoPlano;

/**
 * JPA {\@code AttributeConverter} para o enum {@link domain.enums.SituacaoPlano}.
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
public class SituacaoPlanoConverter implements AttributeConverter<SituacaoPlano, Integer> {

	@Override
	public Integer convertToDatabaseColumn(SituacaoPlano attribute) {
		return Integer.valueOf(attribute.getCodigo());
	}

	@Override
	public SituacaoPlano convertToEntityAttribute(Integer dbData) {
		switch(dbData) {
		case 1: return SituacaoPlano.EM_ELABORACAO;
		case 2: return SituacaoPlano.AGUARDANDO_APROVACAO;
		case 3: return SituacaoPlano.DEFININDO_METAS;
		case 4: return SituacaoPlano.AGUARDANDO_LIBERACAO;
		case 5: return SituacaoPlano.EM_EXECUCAO;
		case 6: return SituacaoPlano.REPLANEJANDO_INDICADORES;
		case 7: return SituacaoPlano.REPLANEJANDO_METAS;
		case 8: return SituacaoPlano.EM_ENCERRAMENTO;
		case 9: return SituacaoPlano.ENCERRADO;
		
		default: throw new IllegalArgumentException(
				"Valor armazenado no banco de dados nÃ£o atende a regra do enumerador: " + dbData);
		}
	}

}

