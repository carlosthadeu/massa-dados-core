package domain.enums;

/**
 * Situação de um {\@link domain.entity.Portfolio}.
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
public enum SituacaoPortifolio {
    NONE(0,"None"),
    ELABORACAO(1, "Em ElaboraÃ§Ã£o"),
    EXECUCAO(2, "Em ExecuÃ§Ã£o"),
    REPLANEJAMENTO(3, "Em Replanejamento"),
    ENCERRAMENTO(4, "Em Encerramento"),
    ENCERRADO(5, "Encerrado");

    private Integer codigo;
    private String nome;

    private SituacaoPortifolio(Integer codigo, String nome ){
        this.codigo = codigo;
        this.nome = nome;
    }

    public Integer getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }
}

