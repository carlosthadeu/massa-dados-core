package domain.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;


/**
 * Domínio de qualificação utilizado para categorização.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Mapear a tabela correspondente no banco de dados.</li>
 *   <li>Fornecer acesso aos atributos via getters e setters.</li>
 * </ul>
 *
 * @author Thadeu Garrido
 * @version 1.0
 */
@Entity
@Table(name="T696DOMI")
public class DominioQualificacao implements Serializable, Comparable<DominioQualificacao> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2268502762851504215L;
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long sq_dmn;
	
	@ManyToOne
    @JoinColumn(name = "sq_qlf")
	private Qualificacao qualificacao;
	
	@Column(name="NM_DMN", length=500, nullable=true, unique=false)
	private String nome;
	
	@Column(name="MT_USU_ALT", length=10, nullable=false, unique=false)
	private String matriculaUsuarioAlteracao;
	
	@Column(name="NM_USU_ALT", length=100, nullable=false, unique=false)
	private String nomeUsuarioAlteracao; 
	
	@Column(name="DH_ALT", nullable=false, columnDefinition = "DATETIME")
	private LocalDateTime dataAtualizacao;

	public Long getSq_dmn() {
		return sq_dmn;
	}

	public void setSq_dmn(Long sq_dmn) {
		this.sq_dmn = sq_dmn;
	}

	public Qualificacao getQualificacao() {
		return qualificacao;
	}

	public void setQualificacao(Qualificacao qualificacao) {
		this.qualificacao = qualificacao;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getMatriculaUsuarioAlteracao() {
		return matriculaUsuarioAlteracao;
	}

	public void setMatriculaUsuarioAlteracao(String matriculaUsuarioAlteracao) {
		this.matriculaUsuarioAlteracao = matriculaUsuarioAlteracao;
	}

	public String getNomeUsuarioAlteracao() {
		return nomeUsuarioAlteracao;
	}

	public void setNomeUsuarioAlteracao(String nomeUsuarioAlteracao) {
		this.nomeUsuarioAlteracao = nomeUsuarioAlteracao;
	}

	public LocalDateTime getDataAtualizacao() {
		return dataAtualizacao;
	}

	public void setDataAtualizacao(LocalDateTime dataAtualizacao) {
		this.dataAtualizacao = dataAtualizacao;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	
	@Override
	public int compareTo(DominioQualificacao o) {
	    int compareAcaoEstrategica = this.qualificacao.compareTo(o.getQualificacao());
	    if (compareAcaoEstrategica != 0) {
	        return compareAcaoEstrategica;
	    }
	    return this.nome.compareTo(o.getNome());
	}

}

