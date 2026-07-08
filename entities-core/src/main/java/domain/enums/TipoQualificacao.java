package domain.enums;

/**
 * Tipo de qualificação.
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
public enum TipoQualificacao {
  UNIDADE ('2', "Unidade"),
  INDICADOR ('1', "Indicador"),
  ACAO_ESTRATEGICA('3', "AÃ§Ã£o EstratÃ©gica");
  
  private char codigo;

  private String nome;
  
  private TipoQualificacao (char codigo, String nome) {
    this.codigo = codigo;
    this.nome = nome;
  }
  
  public char getCodigo() {
    return codigo;
  }

  public String getNome() {
    return nome;
  }

}

