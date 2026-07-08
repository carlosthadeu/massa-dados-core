package domain.enums.converters;

import javax.persistence.AttributeConverter;

import domain.enums.TipoQualificacao;

/**
 * JPA {\@code AttributeConverter} para o enum {@link domain.enums.TipoQualificacao}.
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
public class TipoQualificacaoConvert implements AttributeConverter<TipoQualificacao, Character> {

	@Override
	public Character convertToDatabaseColumn(TipoQualificacao attribute) {
		return Character.valueOf(attribute.getCodigo());
	}

	@Override
	public TipoQualificacao convertToEntityAttribute(Character dbData) {
		switch (dbData) {
		case '1':
			return TipoQualificacao.INDICADOR;
		case '2':
			return TipoQualificacao.UNIDADE;
		case '3':
			return TipoQualificacao.ACAO_ESTRATEGICA;
		default:
			throw new IllegalArgumentException(
					"Valor armazenado no banco de dados nÃ£o atende a regra do enumerador: " + dbData);
		}
	}

}

