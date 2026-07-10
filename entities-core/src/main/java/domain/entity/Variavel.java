package domain.entity;

import domain.enums.Situacao;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;


/**
 * Variável associada a um {\@link Indicador}.
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
@Table(name="T696VARI")
public class Variavel implements Serializable, Comparable<Variavel> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6094308872776954273L;

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long sq_vrv;
	
	@ManyToOne
	@JoinColumn(name="SQ_NAT")
	private NaturezaVariavel naturezaVariavel;
	
	@Column(name="NM_VRV", length=100, nullable=false, unique=false)
	private String nome;
	
	@Column(name="MT_USU_ALT", length=10, nullable=false, unique=false)
	private String matriculaUsuarioAlteracao;
	
	@Column(name="NM_USU_ALT", length=100, nullable=false, unique=false)
	private String nomeUsuarioAlteracao; 
	
	@Column(name="DH_ALT", nullable=false, columnDefinition = "DATETIME")
	private LocalDateTime dataAtualizacao;
	
	@Convert(converter = domain.enums.converters.SituacaoConverter.class)
	@Column(name = "ST_VRV", nullable=false, columnDefinition = "INT")
	private Situacao situacao;
	
	@Column(name="SG_SIS_ORI", length=20, nullable=true, unique=false)
	private String siglaSistemaOrigem;

	public long getSq_vrv() {
		return sq_vrv;
	}

	public void setSq_vrv(long sq_vrv) {
		this.sq_vrv = sq_vrv;
	}

	public NaturezaVariavel getNaturezaVariavel() {
		return naturezaVariavel;
	}

	public void setNaturezaVariavel(NaturezaVariavel naturezaVariavel) {
		this.naturezaVariavel = naturezaVariavel;
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

	public Situacao getSituacao() {
		return situacao;
	}

	public void setSituacao(Situacao situacao) {
		this.situacao = situacao;
	}

	public String getSiglaSistemaOrigem() {
		return siglaSistemaOrigem;
	}

	public void setSiglaSistemaOrigem(String siglaSistemaOrigem) {
		this.siglaSistemaOrigem = siglaSistemaOrigem;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	@Override
	public int compareTo(Variavel o) {
		return this.nome.compareTo(o.nome);
	}

	@Override
	public int hashCode() {
		return Objects.hash(sq_vrv);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Variavel other = (Variavel) obj;
		return Objects.equals(sq_vrv, other.sq_vrv);
	}
}

