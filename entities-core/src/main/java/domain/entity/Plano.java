package domain.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Nationalized;

import domain.enums.SituacaoPlano;


/**
 * Plano vinculado a um {\@link Portfolio}.
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
@Table(name="T696PLAN")
public class Plano implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 6089919783804951635L;
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long sq_pln;
	
	@ManyToOne
    @JoinColumn(name = "SQ_PLJ_EMP")
	private PlanejamentoEmpresarial planejamentoEmpresarial;
	
	@ManyToOne
    @JoinColumn(name = "SQ_GP_UND")
	private GrupoUnidade grupoUnidade;
	
	@ManyToOne
    @JoinColumn(name = "SQ_TP_PLN")
	private TipoPlano tipoPlano;
	
	@Column(name = "NM_PLN", length=100, nullable=false, unique=false)
	private String nome;
	
	@Column(name = "AA_PLN", nullable=false, columnDefinition="SMALLINT")
	private short anoPlano;
	
	@Nationalized
	@Column(name="DE_HST", length = Integer.MAX_VALUE, nullable=false, unique=false)
	private String historico;
	
	@Column(name="DT_POS", nullable=false, columnDefinition = "DATETIME")
	private LocalDate dataPosicao;
	
	@Column(name="MT_USU_ALT", length=10, nullable=false, unique=false)
	private String matriculaUsuarioAlteracao;
	
	@Column(name="NM_USU_ALT", length=100, nullable=false, unique=false)
	private String nomeUsuarioAlteracao; 
	
	@Column(name="DH_ALT", nullable=false, columnDefinition = "DATETIME")
	private LocalDateTime dataAtualizacao;
	
	@Convert(converter = domain.enums.converters.SituacaoPlanoConverter.class)
	@Column(name = "ST_PLN", nullable=false, columnDefinition = "INT")
	private SituacaoPlano situacaoPlano;

	public Long getSq_pln() {
		return sq_pln;
	}

	public void setSq_pln(Long sq_pln) {
		this.sq_pln = sq_pln;
	}

	public PlanejamentoEmpresarial getPlanejamentoEmpresarial() {
		return planejamentoEmpresarial;
	}

	public void setPlanejamentoEmpresarial(PlanejamentoEmpresarial planejamentoEmpresarial) {
		this.planejamentoEmpresarial = planejamentoEmpresarial;
	}

	public GrupoUnidade getGrupoUnidade() {
		return grupoUnidade;
	}

	public void setGrupoUnidade(GrupoUnidade grupoUnidade) {
		this.grupoUnidade = grupoUnidade;
	}

	public TipoPlano getTipoPlano() {
		return tipoPlano;
	}

	public void setTipoPlano(TipoPlano tipoPlano) {
		this.tipoPlano = tipoPlano;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public short getAnoPlano() {
		return anoPlano;
	}

	public void setAnoPlano(short anoPlano) {
		this.anoPlano = anoPlano;
	}

	public String getHistorico() {
		return historico;
	}

	public void setHistorico(String historico) {
		this.historico = historico;
	}

	public LocalDate getDataPosicao() {
		return dataPosicao;
	}

	public void setDataPosicao(LocalDate dataPosicao) {
		this.dataPosicao = dataPosicao;
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

	public SituacaoPlano getSituacaoPlano() {
		return situacaoPlano;
	}

	public void setSituacaoPlano(SituacaoPlano situacaoPlano) {
		this.situacaoPlano = situacaoPlano;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}
}

