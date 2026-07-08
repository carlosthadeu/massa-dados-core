package domain.enums;

/**
 * Situação de um {\@link domain.entity.PlanejamentoEmpresarial}.
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
public enum SituacaoPlanejamentoEmpresarial {
  
  EM_ELABORACAO(1, "Em ElaboraÃ§Ã£o"),
  AGUARDANDO_LIBERACAO(2, "Aguardando liberaÃ§Ã£o para execuÃ§Ã£o"),
  EM_EXECUCAO(3,"Em ExecuÃ§Ã£o"),
  EM_ENCERRAMENTO(4,"Em Encerramento"),
  ENCERRADO(5,"Encerrado"),
  CONCLUIR_ELABORACAO(7, "Concluir ElaboraÃ§Ã£o"),
  INICIAR_ENCERRAMENTO(10, "Iniciar Encerramento");
  
  private int codigo;
  private String descricao;
  
  private SituacaoPlanejamentoEmpresarial(int codigo, String descricao) {
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

