package domain.enums.converters;

import domain.enums.Situacao;
import domain.enums.SituacaoReport;

import jakarta.persistence.AttributeConverter;

/**
 * JPA {\@code AttributeConverter} para o enum {@link domain.enums.SituacaoReport}.
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
public class SituacaoReportConverter implements AttributeConverter<SituacaoReport, Integer> {

	@Override
	public Integer convertToDatabaseColumn(SituacaoReport attribute) {
		return attribute == null ? null : Integer.valueOf(attribute.getCodigo());
	}

	@Override
	public SituacaoReport convertToEntityAttribute(Integer dbData) {
		switch (dbData) {
		case 1:
			return SituacaoReport.ANALISE_AMBIENTE_PLANEJAMENTO;
		case 2:
			return SituacaoReport.ACATADO_AMBIENTE_PLANEJAMENTO;
		case 3:
			return SituacaoReport.REGISTRADO_SISTEMA;
		case 4:
			return SituacaoReport.REVISAO_UNIDADE;
		case 5:
			return SituacaoReport.PENDENTE;
		default:
			throw new IllegalArgumentException(
					"Valor armazenado no banco de dados nÃ£o atende a regra do enumerador:" + dbData);
		}
	}

}

