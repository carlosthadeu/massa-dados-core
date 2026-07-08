package domain.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import domain.enums.Situacao;


/**
 * Origem de uma {\@link AcaoEstrategica} (ex: demanda, planejamento).
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
@Table(name="T696ORAC")
public class OrigemAcaoEstrategica implements Serializable, Comparable<OrigemAcaoEstrategica>  {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8913284356809359991L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long sq_ori_aco;
	
	@Column(name = "NM_ORI_ACO", length=100, nullable = false)
	private String nome;
	
	@Column(name = "DE_ORI_ACO", length=100, nullable = false)
	private String descricao;
	
	@Convert(converter = domain.enums.converters.SituacaoConverter.class)
	@Column(name = "ST_ORI_ACO", nullable=false, columnDefinition = "INT")
	private Situacao situacao;
	
	@Column(name="MT_USU_ALT", length=10, nullable=false, unique=false)
	private String matriculaUsuarioAlteracao;
	
	@Column(name="NM_USU_ALT", length=100, nullable=false, unique=false)
	private String nomeUsuarioAlteracao; 
	
	@Column(name="DH_ALT", nullable=false, columnDefinition = "DATETIME")
	private LocalDateTime dataAtualizacao;

	public Long getSq_ori_aco() {
		return sq_ori_aco;
	}

	public void setSq_ori_aco(Long sq_ori_aco) {
		this.sq_ori_aco = sq_ori_aco;
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

	public Situacao getSituacao() {
		return situacao;
	}

	public void setSituacao(Situacao situacao) {
		this.situacao = situacao;
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

	

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OrigemAcaoEstrategica other = (OrigemAcaoEstrategica) obj;
		return Objects.equals(sq_ori_aco, other.sq_ori_aco);
	}

	@Override
	public int compareTo(OrigemAcaoEstrategica o) {
		return Comparator
				.comparing(OrigemAcaoEstrategica::getSq_ori_aco)
				.compare(this, o);
	}

}

