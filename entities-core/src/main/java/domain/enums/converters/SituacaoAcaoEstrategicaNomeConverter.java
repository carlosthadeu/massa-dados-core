package domain.enums.converters;

import java.util.Arrays;

import jakarta.persistence.AttributeConverter;

import domain.enums.SituacaoAcaoEstrategica;

/**
 * JPA {\@code AttributeConverter} para o enum {@link domain.enums.SituacaoAcaoEstrategica} pelo nome.
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
public class SituacaoAcaoEstrategicaNomeConverter implements AttributeConverter<SituacaoAcaoEstrategica, String>{

	@Override
	public String convertToDatabaseColumn(SituacaoAcaoEstrategica attribute) {
		return attribute == null ? null : attribute.getNome();
	}

	@Override
	public SituacaoAcaoEstrategica convertToEntityAttribute(String dbData) {
		return Arrays.stream(SituacaoAcaoEstrategica.values()).filter(a -> a.getNome().equals(dbData)).findFirst().orElse(null) ;
	}

}

