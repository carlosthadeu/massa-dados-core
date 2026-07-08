package domain.enums.converters;

import javax.persistence.AttributeConverter;

import domain.enums.TipoRegistroValorRealizado;

/**
 * JPA {\@code AttributeConverter} para o enum {@link domain.enums.TipoRegistroValorRealizado}.
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
public class TipoRegistroValorRealizadoConverter implements AttributeConverter<TipoRegistroValorRealizado, Integer> {

	@Override
	public Integer convertToDatabaseColumn(TipoRegistroValorRealizado attribute) {
		return Integer.valueOf(attribute.getCodigo());
	}

	@Override
	public TipoRegistroValorRealizado convertToEntityAttribute(Integer dbData) {
		switch (dbData) {
		case 1:
			return TipoRegistroValorRealizado.MANUAL;
		case 2:
			return TipoRegistroValorRealizado.CONSOLIDADO_OUTRO_PLANO;
		case 3:
			return TipoRegistroValorRealizado.INFORMADO_SISTEMA_ORIGEM;
		case 4:
			return TipoRegistroValorRealizado.CONSOLIDADO_SUBINDICADORES;
		case 5:
			return TipoRegistroValorRealizado.NAO_POSSUI;
		case 6:
			return TipoRegistroValorRealizado.CALCULADO;
		default:
			throw new IllegalArgumentException("Valor armazenado no banco de dados nÃ£o atende a regra do enumerador: " + dbData);
		}
	}

}

