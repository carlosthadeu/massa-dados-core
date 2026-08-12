package domain.entity;

import domain.enums.SituacaoAcaoEstrategica;
import domain.enums.SituacaoEtapa;
import domain.enums.SituacaoReport;
import domain.enums.converters.SituacaoReportConverter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.*;



/**
 * Etapa de uma {\@link AcaoEstrategica}.
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
@Table(name="T696ETAC")
public class EtapaAcaoEstrategica implements Serializable, Comparable<EtapaAcaoEstrategica>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 4644358239895988834L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long sq_etp_aco;
	
	@ManyToOne
	@JoinColumn(name="SQ_ACO_ETT")
	private AcaoEstrategica acaoEstrategica;
	
	@ManyToOne
	@JoinColumn(name="CD_UND")
	private UnidadeOperacional unidadeOperacional;

	public List<Entrega> getEntregas() {
		return entregas;
	}

	public void setEntregas(List<Entrega> entregas) {
		this.entregas = entregas;
	}

	@OneToMany(mappedBy = "etapaAcaoEstrategica", cascade = CascadeType.ALL, orphanRemoval = true)
	List<Entrega> entregas;

	@Column(name = "DE_ETP_ACO", length = 100, nullable = false, unique = false)
	private String descricao;
	
	@Column(name = "DE_ENT_ETP_ACO", length = 100, nullable = false, unique = false)
	private String entrega;
	
	@Column(name = "MT_USU_ALT", length = 10, nullable = false, unique = false)
	private String matriculaUsuarioAlteracao;

	@Column(name = "NM_USU_ALT", length = 100, nullable = false, unique = false)
	private String nomeUsuarioAlteracao;

	@Column(name = "DH_ALT", nullable = false, columnDefinition = "DATETIME")
	private LocalDateTime dataAtualizacao;

	@Convert(converter = domain.enums.converters.SituacaoEtapaConverter.class)
	@Column(name = "ST_ETP_ACO", nullable = false, columnDefinition = "INT")
	private SituacaoEtapa situacao;

	@Column(name = "AA_REA", nullable = false, columnDefinition = "INT")
	private Integer anoRealizacao;

	@Column(name = "PC_REA_TOT", nullable = false, columnDefinition = "DECIMAL", precision = 5, scale = 2)
	private Integer percentualRealizado;

	@Convert(converter = SituacaoReportConverter.class)
	@Column(name = "ST_REP", nullable = false, columnDefinition = "INT")
	private SituacaoReport situacaoReport;

	public SituacaoReport getSituacaoReport() {
		return situacaoReport;
	}

	public void setSituacaoReport(SituacaoReport situacaoReport) {
		this.situacaoReport = situacaoReport;
	}

	public Integer getPercentualRealizado() {
		return percentualRealizado;
	}

	public void setPercentualRealizado(Integer percentualRealizado) {
		this.percentualRealizado = percentualRealizado;
	}

	public Integer getAnoRealizacao() {
		return anoRealizacao;
	}

	public void setAnoRealizacao(Integer anoRealizacao) {
		this.anoRealizacao = anoRealizacao;
	}

	public Long getSq_etp_aco() {
		return sq_etp_aco;
	}

	public void setSq_etp_aco(Long sq_etp_aco) {
		this.sq_etp_aco = sq_etp_aco;
	}

	public AcaoEstrategica getAcaoEstrategica() {
		return acaoEstrategica;
	}

	public void setAcaoEstrategica(AcaoEstrategica acaoEstrategica) {
		this.acaoEstrategica = acaoEstrategica;
	}

	public UnidadeOperacional getUnidadeOperacional() {
		return unidadeOperacional;
	}

	public void setUnidadeOperacional(UnidadeOperacional unidadeOperacional) {
		this.unidadeOperacional = unidadeOperacional;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	public String getEntrega() {
		return entrega;
	}

	public void setEntrega(String entrega) {
		this.entrega = entrega;
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

	public SituacaoEtapa getSituacao() {
		return situacao;
	}

	public void setSituacao(SituacaoEtapa situacao) {
		this.situacao = situacao;
	}

	@Override
	public int hashCode() {
		return Objects.hash(acaoEstrategica, descricao);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EtapaAcaoEstrategica other = (EtapaAcaoEstrategica) obj;
		return Objects.equals(acaoEstrategica, other.acaoEstrategica) && Objects.equals(descricao, other.descricao);
	}

	@Override
	public int compareTo(EtapaAcaoEstrategica o) {
		return this.descricao.compareTo(o.descricao);
	}

}

