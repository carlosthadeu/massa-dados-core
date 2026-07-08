package domain.enums;

/**
 * Situação de um {\@link domain.entity.Plano}.
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
public enum SituacaoPlano {
  EM_ELABORACAO (1, "Em ElaboraÃ§Ã£o"),
  AGUARDANDO_APROVACAO(2, "Aguardando AprovaÃ§Ã£o"),
  DEFININDO_METAS(3,"Definindo Metas"),
  AGUARDANDO_LIBERACAO(4,"Aguardando LiberaÃ§Ã£o para ExecuÃ§Ã£o"),
  EM_EXECUCAO(5, "Em ExecuÃ§Ã£o"),
  REPLANEJANDO_INDICADORES(6, "Replanejando Indicadores"),
  REPLANEJANDO_METAS(7,"Replanejando metas"),
  EM_ENCERRAMENTO(8, "Em Encerramento"),
  ENCERRADO(9, "Encerrado");
  
  private int codigo;
  private String descricao;
  
  private SituacaoPlano (int codigo, String descricao) {
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

