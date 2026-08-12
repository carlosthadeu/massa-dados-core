package domain.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import domain.enums.AbrangenciaConsolidacaoValor;
import domain.enums.TipoCalculo;
import domain.enums.TipoCalculoPerformance;
import domain.enums.TipoRegistroValorRealizado;
import domain.enums.converters.TipoRegistroValorRealizadoConverter;


/**
 * Indicador de desempenho ou monitoramento.
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
@Table(name="T696INDI")
public class Indicador implements  Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3502898592859156289L;

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long sq_id;
	
	@ManyToOne
	@JoinColumn(name = "SQ_PLN")
	private Plano plano;
	
	@ManyToOne
	@JoinColumn(name = "SQ_ID_PAI")
	private Indicador indicadorPai;
	
	@ManyToOne
	@JoinColumn(name="SQ_VRV")
	private Variavel variavel;
	
	@Column(name="NR_ORD_EXB", nullable=false, columnDefinition="INT")
	private Integer NumeroOrdemExibicao;
	
	@Column(name="CD_UND_GES_ID", nullable=false, columnDefinition="INT")
	private Integer codigoUnidadeGestor;
	
	@Column(name="VR_PSO", nullable=false, columnDefinition="decimal", precision=5, scale=2)
	private BigDecimal Peso;
	
	@Column(name="VR_PSO_MIN", nullable=false, columnDefinition="decimal", precision=5, scale=2)
	private BigDecimal valorPiso;
	
	@Column(name="VR_TET_MAX", nullable=false, columnDefinition="decimal", precision=5, scale=2)
	private BigDecimal valorTeto;
	
	@Column(name = "ID_TEM_MTA", columnDefinition = "BIT")
	private Boolean temMetaSoN;
	
	@Column(name = "ID_VSB", columnDefinition = "BIT")
	private Boolean visibilidade;
	
	@Convert(converter=TipoRegistroValorRealizadoConverter.class)
	@Column(name="TP_REG_VR_REA", columnDefinition="INT", nullable=false)
	private TipoRegistroValorRealizado tipoRegistroValorRealizado;
	
	@ManyToOne
	@JoinColumn(name = "SQ_PLN_CSD_VR")
	private Plano planoCsdVr;
	
	@Convert(converter=domain.enums.converters.AbrangenciaConsolidacaoValorConverter.class)
	@Column(name="TP_ABG_CSD_VR", columnDefinition="INT", nullable=false)
	private AbrangenciaConsolidacaoValor abrangenciaConsolidacaoValor;
	
	@Convert(converter = domain.enums.converters.TipoCalculoConverter.class)
	@Column(name="TP_CLC", columnDefinition="INT", nullable=true)
	private TipoCalculo tipoCalculo;
	
	@Convert(converter = domain.enums.converters.TipoCalculoPerformanceConverter.class)
	@Column(name="TP_CLC_PFM", columnDefinition="INT", nullable=true)
	private TipoCalculoPerformance tipoCalculoPerformance;
	
	@Column(name="MT_USU_ALT", length=10, nullable=false, unique=false)
	private String matriculaUsuarioAlteracao;
	
	@Column(name="NM_USU_ALT", length=100, nullable=false, unique=false)
	private String nomeUsuarioAlteracao; 
	
	@Column(name="DH_ALT", nullable=false, columnDefinition = "DATETIME")
	private LocalDateTime dataAtualizacao;
	
	@Column(name="VR_DSP_MAX_ESP", nullable=false, columnDefinition="decimal", precision=5, scale=2)
	private BigDecimal valorDesempenhoMaximoEsperado;
	
	@Column(name="VR_DSP_MNM_RQR", nullable=false, columnDefinition="decimal", precision=5, scale=2)
	private BigDecimal valorDesempenhoMinimoRequerido;

	public Long getSq_id() {
		return sq_id;
	}

	public void setSq_id(Long sq_id) {
		this.sq_id = sq_id;
	}

	public Plano getPlano() {
		return plano;
	}

	public void setPlano(Plano plano) {
		this.plano = plano;
	}

	public Indicador getIndicadorPai() {
		return indicadorPai;
	}

	public void setIndicadorPai(Indicador indicadorPai) {
		this.indicadorPai = indicadorPai;
	}

	public Variavel getVariavel() {
		return variavel;
	}

	public void setVariavel(Variavel variavel) {
		this.variavel = variavel;
	}

	public Integer getNumeroOrdemExibicao() {
		return NumeroOrdemExibicao;
	}

	public void setNumeroOrdemExibicao(Integer numeroOrdemExibicao) {
		NumeroOrdemExibicao = numeroOrdemExibicao;
	}

	public Integer getCodigoUnidadeGestor() {
		return codigoUnidadeGestor;
	}

	public void setCodigoUnidadeGestor(Integer codigoUnidadeGestor) {
		this.codigoUnidadeGestor = codigoUnidadeGestor;
	}

	public BigDecimal getPeso() {
		return Peso;
	}

	public void setPeso(BigDecimal peso) {
		Peso = peso;
	}

	public BigDecimal getValorPiso() {
		return valorPiso;
	}

	public void setValorPiso(BigDecimal valorPiso) {
		this.valorPiso = valorPiso;
	}

	public BigDecimal getValorTeto() {
		return valorTeto;
	}

	public void setValorTeto(BigDecimal valorTeto) {
		this.valorTeto = valorTeto;
	}

	public Boolean getTemMetaSoN() {
		return temMetaSoN;
	}

	public void setTemMetaSoN(Boolean temMetaSoN) {
		this.temMetaSoN = temMetaSoN;
	}

	public Boolean getVisibilidade() {
		return visibilidade;
	}

	public void setVisibilidade(Boolean visibilidade) {
		this.visibilidade = visibilidade;
	}

	public TipoRegistroValorRealizado getTipoRegistroValorRealizado() {
		return tipoRegistroValorRealizado;
	}

	public void setTipoRegistroValorRealizado(TipoRegistroValorRealizado tipoRegistroValorRealizado) {
		this.tipoRegistroValorRealizado = tipoRegistroValorRealizado;
	}

	public Plano getPlanoCsdVr() {
		return planoCsdVr;
	}

	public void setPlanoCsdVr(Plano planoCsdVr) {
		this.planoCsdVr = planoCsdVr;
	}

	public AbrangenciaConsolidacaoValor getAbrangenciaConsolidacaoValor() {
		return abrangenciaConsolidacaoValor;
	}

	public void setAbrangenciaConsolidacaoValor(AbrangenciaConsolidacaoValor abrangenciaConsolidacaoValor) {
		this.abrangenciaConsolidacaoValor = abrangenciaConsolidacaoValor;
	}

	public TipoCalculo getTipoCalculo() {
		return tipoCalculo;
	}

	public void setTipoCalculo(TipoCalculo tipoCalculo) {
		this.tipoCalculo = tipoCalculo;
	}

	public TipoCalculoPerformance getTipoCalculoPerformance() {
		return tipoCalculoPerformance;
	}

	public void setTipoCalculoPerformance(TipoCalculoPerformance tipoCalculoPerformance) {
		this.tipoCalculoPerformance = tipoCalculoPerformance;
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

	public BigDecimal getValorDesempenhoMaximoEsperado() {
		return valorDesempenhoMaximoEsperado;
	}

	public void setValorDesempenhoMaximoEsperado(BigDecimal valorDesempenhoMaximoEsperado) {
		this.valorDesempenhoMaximoEsperado = valorDesempenhoMaximoEsperado;
	}

	public BigDecimal getValorDesempenhoMinimoRequerido() {
		return valorDesempenhoMinimoRequerido;
	}

	public void setValorDesempenhoMinimoRequerido(BigDecimal valorDesempenhoMinimoRequerido) {
		this.valorDesempenhoMinimoRequerido = valorDesempenhoMinimoRequerido;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	
	
}

