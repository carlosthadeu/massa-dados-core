package domain.enums;

/**
 * Tipo de registro de valor realizado.
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
public enum TipoRegistroValorRealizado {
  MANUAL(1, "Manual"),
  CONSOLIDADO_OUTRO_PLANO(2, "Consolidado de outro Plano"),
  INFORMADO_SISTEMA_ORIGEM(3, "Informado pelo Sistema Origem"),
  CONSOLIDADO_SUBINDICADORES(4, "Consolidado pelos Subindicadores"),
  NAO_POSSUI(5, "NÃ£o Possui Valor Realizado"),
  CALCULADO(6, "Calculado Automaticamente pelo S696");
  
  private int codigo;
  private String descricao;
  
  private TipoRegistroValorRealizado(int codigo, String descricao) {
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

