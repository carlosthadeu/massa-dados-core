package domain.enums.converters;

import jakarta.persistence.AttributeConverter;

import domain.enums.Situacao;

/**
 * JPA {\@code AttributeConverter} para o enum {@link domain.enums.Situacao}.
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
public class SituacaoConverter implements AttributeConverter<Situacao, Integer> {

	@Override
	public Integer convertToDatabaseColumn(Situacao attribute) {
		return attribute == null ? null : Integer.valueOf(attribute.getCodigo());
	}

	@Override
	public Situacao convertToEntityAttribute(Integer dbData) {
		switch (dbData) {
		case 1:
			return Situacao.INATIVO;
		case 2:
			return Situacao.ATIVO;
		default:
			throw new IllegalArgumentException(
					"Valor armazenado no banco de dados nÃ£o atende a regra do enumerador:" + dbData);
		}
	}

}

