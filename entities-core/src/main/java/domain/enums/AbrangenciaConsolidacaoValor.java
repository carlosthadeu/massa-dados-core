package domain.enums;

/**
 * Abrangência para consolidação de valor (ex: global, por unidade).
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
public enum AbrangenciaConsolidacaoValor {
  NENHUMA(0, ""),
  TODAS(1, "Todas as Unidades"),
  SUBORDINADA(2, "Unidades Subordinadas");
  
  private int codigo;
  private String descricao;
  
  private AbrangenciaConsolidacaoValor(int codigo, String descricao) {
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

