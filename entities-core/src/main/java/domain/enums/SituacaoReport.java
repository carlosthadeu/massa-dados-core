package domain.enums;

/**
 * Situação de um report/relatório.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Definir as constantes do domínio.</li>
 *   <li>Fornecer acesso aos atributos associados.</li>
 * </ul>
 *
 * @author Thadeu Garrido
 * @version 1.0
 */
public enum SituacaoReport {
    NONE(0, "None"),
    ANALISE_AMBIENTE_PLANEJAMENTO (1, "AnÃ¡lise do Ambiente de Planejamento"),
    ACATADO_AMBIENTE_PLANEJAMENTO (2, "Acatado pelo Ambiente de Planejamento"),
    REGISTRADO_SISTEMA (3, "Registrado pelo sistema"),
    REVISAO_UNIDADE (4, "RevisÃ£o da unidade"),
    PENDENTE(5, "Pendente");

    private int codigo;
    private String nome;
    private SituacaoReport(int codigo, String nome) {
        this.codigo = codigo;
        this.nome = nome;
    }

    public int getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }
}

