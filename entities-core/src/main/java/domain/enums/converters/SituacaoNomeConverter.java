package domain.enums.converters;

import javax.persistence.AttributeConverter;

import domain.enums.Situacao;

/**
 * JPA {\@code AttributeConverter} para o enum {@link domain.enums.Situacao} pelo nome.
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
public class SituacaoNomeConverter implements AttributeConverter<Situacao, String> {

	@Override
	public String convertToDatabaseColumn(Situacao attribute) {
		return attribute.getNome();
	}

	@Override
	public Situacao convertToEntityAttribute(String dbData) {
		for (Situacao s : Situacao.values()) {
			if (s.getNome().equalsIgnoreCase(dbData)) {
				return s;
			}
		}
		// either throw the IAE or return null, your choice.
		throw new IllegalArgumentException(String.valueOf(dbData));

	}

}

