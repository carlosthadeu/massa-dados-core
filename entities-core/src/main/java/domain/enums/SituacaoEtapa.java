package domain.enums;

/**
 * Situação de uma {\@link domain.entity.EtapaAcaoEstrategica}.
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
public enum SituacaoEtapa {
    NONE(0, "None"),
    INICIADA(1, "NÃ£o iniciada"),
    EM_ANDAMENTO(2, "Em andamento"),
    ATRASADA(3, "Atrasada"),
    NAO_CONCLUIDA(4, "NÃ£o ConcluÃ­da"),
    CONCLUIDA_ATRASO(5, "ConcluÃ­da com Atraso"),
    CONCLUIDA(6, "ConcluÃ­da");
    private int codigo;
    private String descricao;

    private SituacaoEtapa(int codigo, String descricao) {
        this.codigo = codigo;
        this.descricao = descricao;
    }

    public int getCodigo() {
        return codigo;
    }

    public String getDescricao() {
        return descricao;
    }
}

