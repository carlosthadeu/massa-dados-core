package domain.enums;

/**
 * Mensagens padronizadas do sistema.
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
public enum Mensagem {
  
  MSG001("Nenhum registro encontrado!"),
  MSG002("O campo \"%s\" Ã© de preenchimento obrigatÃ³rio. Gentileza, verificar."),
  MSG003("O campo \"%s\" Ã© de preenchimento obrigatÃ³rio, devendo ter no mÃ­nimo 20 caracteres. Gentileza, verificar."),
  MSG004("JÃ¡ existe um item cadastrado com este mesmo nome. Gentileza, verificar."),
  MSG005("Diretriz EstratÃ©gica cadastrada com sucesso!"),
  MSG006("OrientaÃ§Ã£o EstratÃ©gica alterada com sucesso!"),
  MSG007("Diretriz EstratÃ©gica alterada com sucesso!"),
  MSG008("Diretriz EstratÃ©gica excluÃ­da com sucesso!"),
  MSG009("Existe OrientaÃ§Ã£o EstratÃ©gica vinculada a esta Diretriz. Primeiro, exclua as OrientaÃ§Ãµes vinculadas para depois prosseguir com a exclusÃ£o da Diretriz EstratÃ©gica."),
  MSG010("OrientaÃ§Ã£o EstratÃ©gica cadastrada com sucesso!"),
  MSG011("OrientaÃ§Ã£o EstratÃ©gica excluÃ­da com sucesso!"),
  MSG012("Existe AÃ§Ã£o EstratÃ©gica vinculada a esta Origem de AÃ§Ã£o. Primeiro, exclua as AÃ§Ãµes EstratÃ©gicas vinculadas, para depois prosseguir com a exclusÃ£o da Origem da AÃ§Ã£o."),
  MSG013("Origem excluÃ­da com sucesso!"),
  MSG014("JÃ¡ existe uma Origem de AÃ§Ã£o com o mesmo nome informado. Gentileza, verificar!"),
  MSG015("Origem alterada com sucesso!"),
  MSG016("O campo \"%s\" Ã© de preenchimento obrigatÃ³rio, devendo ter no mÃ­nimo 5 caracteres. Gentileza, verificar."),
  MSG017("Origem cadastrada com sucesso!"),
  MSG018("Proposta de AÃ§Ã£o EstratÃ©gica cadastrada com sucesso!"),
  MSG019("JÃ¡ existe uma AÃ§Ã£o EstratÃ©tica com o mesmo nome informado. Gentileza, verificar!"),
  MSG020("Proposta de AÃ§Ã£o EstratÃ©gica alterada com sucesso!"),
  MSG021("Proposta de AÃ§Ã£o EstratÃ©gica excluÃ­da com sucesso!"),
  MSG022("Pelo menos uma etapa deve ser cadastrada. Gentileza, verificar!"),
  MSG023("A(s) Etapa(s) cadastradas possuem as Datas de inicio e/ou fim maior que a Data Limite. Gentileza, verificar!"),
  MSG024("O percentual da(s) etapa(s) cadastrada(s) estÃ¡ diferente de 100%. Gentileza, verificar!"),
  MSG025("Proposta de AÃ§Ã£o EstratÃ©gica inativada com sucesso."),
  MSG026("OperaÃ§Ã£o realizada com sucesso."),
  MSG027("Nenhuma etapa cadastrada"),
  MSG028("Etapa excluÃ­da com sucesso!"),
  MSG029("Etapa alterada com sucesso!"),
  MSG030("Etapa cadastrada com sucesso!"),
  MSG031("Foi excedida a quantidade mÃ¡xima de etapas. Gentileza verificar."),
  MSG032("PortfÃ³lio cadastrado com sucesso!"),
  MSG033("PortfÃ³lio alterado com sucesso!"),
  MSG034("PortfÃ³lio excluÃ­do com sucesso!");
  
  
  private String mensagem;
  
  private Mensagem(String mensagem) {
    this.mensagem = mensagem;
  }
  
  public String getMensagem() {
    return this.mensagem;
  }
  
  public String getMensagem(String substituicao) {
    return String.format(this.mensagem, substituicao);
  }

}

