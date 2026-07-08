package domain.enums.converters;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import domain.enums.AbrangenciaConsolidacaoValor;

@Converter
/**
 * JPA {\@code AttributeConverter} para o enum {@link domain.enums.AbrangenciaConsolidacaoValor}.
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
public class AbrangenciaConsolidacaoValorConverter implements AttributeConverter<AbrangenciaConsolidacaoValor, Integer> {

	@Override
	public Integer convertToDatabaseColumn(AbrangenciaConsolidacaoValor attribute) {
		return Integer.valueOf(attribute.getCodigo()) ;
	}

	@Override
	public AbrangenciaConsolidacaoValor convertToEntityAttribute(Integer dbData) {
		switch (dbData) {
		case 0: return AbrangenciaConsolidacaoValor.NENHUMA;
		case 1: return AbrangenciaConsolidacaoValor.TODAS;
		case 2: return AbrangenciaConsolidacaoValor.SUBORDINADA;
		default:
            throw new IllegalArgumentException("Valor armazenado no banco de dados nÃ£o atende a regra do enumerador: " + dbData);
		}
	}


	

}

