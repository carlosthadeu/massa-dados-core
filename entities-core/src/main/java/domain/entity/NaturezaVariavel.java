package domain.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


/**
 * Natureza de uma variável (ex: financeira, quantitativa, qualitativa).
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
@Table(name="T696NATV")
public class NaturezaVariavel implements Serializable, Comparable<NaturezaVariavel> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 975148493076318404L;
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long sq_nat;
	
	@Column(name="NM_NAT", length=500, nullable=true, unique=false)
	private String descricao;
	
	
	@Column(name="MT_USU_ALT", length=10, nullable=false, unique=false)
	private String matriculaUsuarioAlteracao;
	
	@Column(name="NM_USU_ALT", length=100, nullable=false, unique=false)
	private String nomeUsuarioAlteracao; 
	
	@Column(name="DH_ALT", nullable=false, columnDefinition = "DATETIME")
	private LocalDateTime dataAtualizacao;
	
	@Column(name="FT_DIV", nullable=true, columnDefinition = "INT")
	private Integer fatorDivisor;
	
	@Column(name="NM_NAT_RLT", nullable=true, length=100, unique=false)
	private String nomeNaturezaRelatorio;

	public long getSq_nat() {
		return sq_nat;
	}

	public void setSq_nat(long sq_nat) {
		this.sq_nat = sq_nat;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
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

	public Integer getFatorDivisor() {
		return fatorDivisor;
	}

	public void setFatorDivisor(Integer fatorDivisor) {
		this.fatorDivisor = fatorDivisor;
	}

	public String getNomeNaturezaRelatorio() {
		return nomeNaturezaRelatorio;
	}

	public void setNomeNaturezaRelatorio(String nomeNaturezaRelatorio) {
		this.nomeNaturezaRelatorio = nomeNaturezaRelatorio;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	@Override
	public int compareTo(NaturezaVariavel o) {
		return this.descricao.compareTo(o.descricao);
	}
}

