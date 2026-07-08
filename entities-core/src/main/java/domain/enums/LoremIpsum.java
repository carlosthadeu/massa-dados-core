package domain.enums;

/**
 * Enum para geração de dados fictícios (Lorem Ipsum).
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
public enum LoremIpsum {
  LOREMIPSUMPAR1 (1,
      "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Etiam porta mauris ipsum, ac pretium felis tempus vel. Sed vehicula, magna vitae interdum dignissim, enim nibh condimentum turpis, a pretium risus ante ac dolor. Etiam vitae rhoncus libero. Ut accumsan, metus eu dapibus lacinia, tellus erat egestas libero, eget molestie dolor ante vitae risus. Suspendisse congue massa euismod justo tempor, quis pretium arcu vestibulum. In non nisl facilisis, hendrerit ante in, dictum ante. Aliquam massa odio, tempor ac facilisis vel, dictum id tellus. Nunc blandit justo lorem, sed consequat eros luctus ac. Suspendisse non purus interdum, commodo ex in, congue velit. Integer urna urna, scelerisque quis gravida vel, iaculis quis augue. Aliquam laoreet elementum tortor. Cras sodales lacus leo, nec molestie enim auctor vel. Vivamus ut auctor risus. In quis ipsum vulputate, aliquet odio quis, ultricies neque. Praesent tincidunt sit amet nisi ac rutrum. Aliquam egestas rutrum dolor non vestibulum."),
  LOREMIPSUMPAR2 (2,
      "Aliquam porta nunc felis, vel malesuada elit fringilla quis. Proin enim metus, iaculis sit amet felis sit amet, feugiat imperdiet nisl. Fusce eget massa erat. Proin eget metus bibendum, pretium elit ac, vestibulum eros. Nulla facilisi. Vivamus et ex ac sapien sollicitudin finibus. Fusce eu porta ipsum, id molestie tellus. Aenean laoreet, lorem sed condimentum iaculis, purus ex fringilla enim, vitae lacinia enim sem eget ipsum. Nunc nec aliquet magna. Morbi magna ligula, vestibulum nec dictum quis, mollis quis dolor. In tempor varius dapibus. Curabitur vel urna id ex luctus rutrum nec ut lacus. Curabitur aliquam purus nisi, ut auctor ex porttitor non. Suspendisse pharetra sed orci sit amet sollicitudin. Integer tristique, neque quis cursus sagittis, nisl ipsum sodales lacus, eget commodo metus urna at velit. Suspendisse venenatis eleifend ipsum eget varius.");
  
  private int codigo;
  private String paragrafo;
  
  private LoremIpsum(int codigo, String paragrafo) {
    this.codigo = codigo;
    this.paragrafo = paragrafo; 
  }

  public int getCodigo() {
    return codigo;
  }

  public void setCodigo(int codigo) {
    this.codigo = codigo;
  }

  public String getParagrafo() {
    return paragrafo;
  }

  public void setParagrafo(String paragrafo) {
    this.paragrafo = paragrafo;
  }
  
  
}

