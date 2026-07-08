package domain.enums;

/**
 * Ciclo de vida de uma {@link domain.entity.AcaoEstrategica}.
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
public enum SituacaoAcaoEstrategica {
  NONE (0, "None"),
  INATIVO (1, "Inativa"), 
  ATIVO(2, "Ativo"), 
  PROPOSTA_EM_EDICAO(3, "Proposta em EdiÃ§Ã£o"), 
  PROPOSTA_EM_ANALISE_UNIDADE(4,"Proposta em AnÃ¡lise - Unidade"),
  PROPOSTA_REJEITADA_PELA_UNIDADE(5, "Proposta Rejeitada pela Unidade"),
  PROPOSTA_EM_PLANEJAMENTO(6, "Proposta em Planejamento"),
  PROPOSTA_ANALISE_GESTOR(7, "Proposta em AnÃ¡lise - Gestor de Unidade" ),
  PROPOSTA_ANALISE_PLANEJAMENTO(8, "Proposta em AnÃ¡lise - Amb. de Planejamento" ),
  PROPOSTA_ANALISE_SUPER(9, "Proposta em AnÃ¡lise - SuperintendÃªncia" ),
  PROPOSTA_ANALISE_DIRETOR(10, "Proposta em AnÃ¡lise - Diretoria Executiva" ),
  REPROVADO_GESTOR_UNIDADE(11, "");
  
  
  private Integer codigo;
  private String nome;
  
  private SituacaoAcaoEstrategica(Integer codigo, String nome) {
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

