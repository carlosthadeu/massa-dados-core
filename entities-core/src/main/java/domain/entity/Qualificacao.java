package domain.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import domain.enums.TipoQualificacao;


/**
 * Qualificação atribuída a uma entidade.
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
@Table(name="T696QUAL")
public class Qualificacao implements Serializable, Comparable<Qualificacao> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2533593794095529781L;
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long sq_qlf;
	
	@Column(name="NM_QLF", length=100, nullable=false, unique=false)
	private String nome;
	
	@Column(name="DE_QLF", length=500, nullable=true, unique=false)
	private String descricao;
	
	@Convert(converter = domain.enums.converters.TipoQualificacaoConvert.class)
	@Column(name = "TP_QLF", length = 1, columnDefinition = "CHAR")
	private TipoQualificacao tipoQualificacao;
	
	@Column(name="MT_USU_ALT", length=10, nullable=false, unique=false)
	private String matriculaUsuarioAlteracao;
	
	@Column(name="NM_USU_ALT", length=100, nullable=false, unique=false)
	private String nomeUsuarioAlteracao; 
	
	@Column(name="DH_ALT", nullable=false, columnDefinition = "DATETIME")
	private LocalDateTime dataAtualizacao;

	public Long getSq_qlf() {
		return sq_qlf;
	}

	public void setSq_qlf(Long sq_qlf) {
		this.sq_qlf = sq_qlf;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	public TipoQualificacao getTipoQualificacao() {
		return tipoQualificacao;
	}

	public void setTipoQualificacao(TipoQualificacao tipoQualificacao) {
		this.tipoQualificacao = tipoQualificacao;
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
	public int compareTo(Qualificacao o) {
		return this.getNome().compareTo(o.getNome());
	}
	
	
}

