package domain.enums;

/**
 * Indicadores de avaliação de desempenho.
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
public enum IndicadorAvaliacaoDesempenho {
  
  NAO_REQUER(1, "NÃ£o Requer AvaliaÃ§Ã£o de Desempenho"),
  REQUER_SEMPRE(2, "Requer sempre AvaliaÃ§Ã£o de Desempenho na periodicidade"),
  REQUER_QUANDO(3, "Requer avaliaÃ§Ã£o quando");
  
  private int codigo;
  private String descricao;
  
  private IndicadorAvaliacaoDesempenho(int codigo, String descricao) {
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

