package domain.enums;

/**
 * Situação genérica (ativo/inativo).
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
public enum Situacao {
  
  INATIVO(1, "Inativo"),
  ATIVO(2, "Ativo");
  
  private int codigo;
  private String nome;
  
  private Situacao(int codigo, String nome) {
    this.codigo = codigo;
    this.nome = nome; 
  }

  public int getCodigo() {
    return codigo;
  }

  public String getNome() {
    return nome;
  }

}

