package domain.enums.converters;

import java.util.Arrays;

import javax.persistence.AttributeConverter;
import domain.enums.SituacaoAcaoEstrategica;

/**
 * JPA {\@code AttributeConverter} para o enum {@link domain.enums.SituacaoAcaoEstrategica} pelo código.
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
public class SituacaoAcaoEstrategicaConverter implements AttributeConverter<SituacaoAcaoEstrategica, Integer>{

	@Override
	public Integer convertToDatabaseColumn(SituacaoAcaoEstrategica attribute) {
	return attribute == null ? null : Integer.valueOf(attribute.getCodigo());
	}

	@Override
	public SituacaoAcaoEstrategica convertToEntityAttribute(Integer dbData) {
		return Arrays.stream(SituacaoAcaoEstrategica.values()).filter(a -> a.getCodigo().equals(Integer.valueOf(dbData))).findFirst().orElse(null) ;
	}
}

	

