package domain.enums;

/**
 * Tipo de cálculo aplicado a indicadores.
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
public enum TipoCalculo {
  NAO_DETERMINADO(0,"NÃ£o determinado"),
  SOMA(1, "Soma"),
  MEDIA(2, "MÃ©dia AritmÃ©tica");
  
  private int codigo;
  private String descricao;
  
  private TipoCalculo(int codigo, String descricao) {
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

