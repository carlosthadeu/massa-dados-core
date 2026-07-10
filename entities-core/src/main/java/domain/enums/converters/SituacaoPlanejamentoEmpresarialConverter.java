package domain.enums.converters;

import jakarta.persistence.AttributeConverter;

import domain.enums.SituacaoPlanejamentoEmpresarial;

/**
 * JPA {\@code AttributeConverter} para o enum {@link domain.enums.SituacaoPlanejamentoEmpresarial}.
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
public class SituacaoPlanejamentoEmpresarialConverter implements AttributeConverter<SituacaoPlanejamentoEmpresarial, Integer> {

	@Override
	public Integer convertToDatabaseColumn(SituacaoPlanejamentoEmpresarial attribute) {
		return Integer.valueOf(attribute.getCodigo());
	}

	@Override
	public SituacaoPlanejamentoEmpresarial convertToEntityAttribute(Integer dbData) {
		switch(dbData) {
		case 1: return SituacaoPlanejamentoEmpresarial.EM_ELABORACAO;
		case 2: return SituacaoPlanejamentoEmpresarial.AGUARDANDO_LIBERACAO;
		case 3: return SituacaoPlanejamentoEmpresarial.EM_EXECUCAO;
		case 4: return SituacaoPlanejamentoEmpresarial.EM_ENCERRAMENTO;
		case 5: return SituacaoPlanejamentoEmpresarial.ENCERRADO;
		case 7: return SituacaoPlanejamentoEmpresarial.CONCLUIR_ELABORACAO;
		case 10: return SituacaoPlanejamentoEmpresarial.INICIAR_ENCERRAMENTO;
		default: throw new IllegalArgumentException(
				"Valor armazenado no banco de dados nÃ£o atende a regra do enumerador: " + dbData);
		}
	}

}

