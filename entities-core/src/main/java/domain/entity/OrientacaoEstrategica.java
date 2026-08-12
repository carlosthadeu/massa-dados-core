package domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.Nationalized;

import domain.enums.Situacao;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Comparator;


/**
 * Orientação estratégica vinculada a uma {\@link DiretrizEstrategica}.
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
@Table(name="T696ORES")
public class OrientacaoEstrategica implements Serializable, Comparable<OrientacaoEstrategica> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1306946897850765259L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long sq_ort_ett;
	
	@ManyToOne
    @JoinColumn(name = "SQ_DRT_ETT")
	private DiretrizEstrategica diretrizEstrategica;
	
	@Nationalized
	@Column(name="DE_ORT_ETT", length = Integer.MAX_VALUE, nullable=false, unique=true)
	private String descricao;
	
	@Convert(converter = domain.enums.converters.SituacaoConverter.class)
	@Column(name = "ST_ORT_ETT", nullable=false, columnDefinition = "INT")
	private Situacao situacao;
	
	@Column(name="MT_USU_ALT", length=10, nullable=false, unique=false)
	private String matriculaUsuarioAlteracao;
	
	@Column(name="NM_USU_ALT", length=100, nullable=false, unique=false)
	private String nomeUsuarioAlteracao; 
	
	@Column(name="DH_ALT", nullable=false, columnDefinition = "DATETIME")
	private LocalDateTime dataAtualizacao;

	public Long getSq_ort_ett() {
		return sq_ort_ett;
	}

	public void setSq_ort_ett(Long sq_ort_ett) {
		this.sq_ort_ett = sq_ort_ett;
	}

	public DiretrizEstrategica getDiretrizEstrategica() {
		return diretrizEstrategica;
	}

	public void setDiretrizEstrategica(DiretrizEstrategica diretrizEstrategica) {
		this.diretrizEstrategica = diretrizEstrategica;
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

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	@Override
	public int compareTo(OrientacaoEstrategica o) {
		return Comparator
				.comparing(OrientacaoEstrategica::getSq_ort_ett)
				.compare(this, o);
	}

}

