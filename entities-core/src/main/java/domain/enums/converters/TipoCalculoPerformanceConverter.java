package domain.enums.converters;

import javax.persistence.AttributeConverter;

import domain.enums.TipoCalculoPerformance;

/**
 * JPA {\@code AttributeConverter} para o enum {@link domain.enums.TipoCalculoPerformance}.
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
public class TipoCalculoPerformanceConverter implements AttributeConverter<TipoCalculoPerformance, Integer> {

	@Override
	public Integer convertToDatabaseColumn(TipoCalculoPerformance attribute) {
		return null;
	}

	@Override
	public TipoCalculoPerformance convertToEntityAttribute(Integer dbData) {
		switch (dbData) {
		case 0:
			return TipoCalculoPerformance.NENHUM;
		case 1:
			return TipoCalculoPerformance.POSITIVA;
		case 2:
			return TipoCalculoPerformance.NEGATIVA;
		case 3:
			return TipoCalculoPerformance.INVERTIDA_POSITIVA;
		case 4:
			return TipoCalculoPerformance.INVERTIDA_NEGATIVA;
		case 5:
			return TipoCalculoPerformance.PONTUACAO_SUB_POS;
		case 6:
			return TipoCalculoPerformance.NAO_POSSUI;
		case 7:
			return TipoCalculoPerformance.PONTUACAO_SUB_NEG;
		default:
			throw new IllegalArgumentException(
					"Valor armazenado no banco de dados nÃ£o atende a regra do enumerador: " + dbData);
		}
	}

}

