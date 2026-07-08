package domain.enums;

/**
 * Tipo de cálculo de performance.
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
public enum TipoCalculoPerformance {
  NENHUM (0, ""),
  POSITIVA(1, "PadrÃ£o Positiva"),
  NEGATIVA(2, "PadrÃ£o Negativa"),
  INVERTIDA_POSITIVA(3, "Invertida Positiva"),
  INVERTIDA_NEGATIVA(4, "Invertida Negativa"),
  PONTUACAO_SUB_POS(5, "Soma da PontuaÃ§Ã£o dos Subindicadores - Valor Positivo"),
  NAO_POSSUI(6, "NÃ£o possui cÃ¡lculo"),
  PONTUACAO_SUB_NEG(7, "Soma da PontuaÃ§Ã£o dos Subindicadores - Valor Negativo");
  
  private int codigo;
  private String descricao;
  
  private TipoCalculoPerformance(int codigo, String descricao) {
    this.codigo = codigo;
    this.descricao= descricao;
  }

  public int getCodigo() {
    return codigo;
  }

  public String getDescricao() {
    return descricao;
  }
  
  

}

