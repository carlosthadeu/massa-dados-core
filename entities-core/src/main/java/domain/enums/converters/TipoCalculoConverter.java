package domain.enums.converters;

import javax.persistence.AttributeConverter;

import domain.enums.TipoCalculo;

/**
 * JPA {\@code AttributeConverter} para o enum {@link domain.enums.TipoCalculo}.
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
public class TipoCalculoConverter implements AttributeConverter<TipoCalculo, Integer> {

	@Override
	public Integer convertToDatabaseColumn(TipoCalculo attribute) {
		return Integer.valueOf(attribute.getCodigo());
	}

	@Override
	public TipoCalculo convertToEntityAttribute(Integer dbData) {
		switch (dbData) {
		case 0:
			return TipoCalculo.NAO_DETERMINADO;
		case 1:
			return TipoCalculo.SOMA;
		case 2:
			return TipoCalculo.MEDIA;
		default:
			throw new IllegalArgumentException(
					"Valor armazenado no banco de dados nÃ£o atende a regra do enumerador: " + dbData);
		}
	}

}

