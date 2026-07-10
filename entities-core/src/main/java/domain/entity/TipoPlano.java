package domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import domain.enums.Situacao;

import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * Tipo de plano (ex: tático, operacional).
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
@Table(name="T696TPPL")
public class TipoPlano implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8779114195581162726L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long sq_tp_pln;
	
	@Column(name = "NM_TP_PLN", length=100, nullable = false)
	private String name;
	
	@Column(name = "NR_NIV", nullable=false, columnDefinition="SMALLINT")
	private short NumeroNivel;
	
	@Column(name="MT_USU_ALT", length=10, nullable=false, unique=false)
	private String matriculaUsuarioAlteracao;
	
	@Column(name="NM_USU_ALT", length=100, nullable=false, unique=false)
	private String nomeUsuarioAlteracao; 
	
	@Column(name="DH_ALT", nullable=false, columnDefinition = "DATETIME")
	private LocalDateTime dataAtualizacao;
	
	@Convert(converter = domain.enums.converters.SituacaoConverter.class)
	@Column(name = "ST_TP_PLN", nullable=false, columnDefinition = "INT")
	private Situacao situacao;

	public Long getSq_tp_pln() {
		return sq_tp_pln;
	}

	public void setSq_tp_pln(Long sq_tp_pln) {
		this.sq_tp_pln = sq_tp_pln;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public short getNumeroNivel() {
		return NumeroNivel;
	}

	public void setNumeroNivel(short numeroNivel) {
		NumeroNivel = numeroNivel;
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

	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	
	

}

