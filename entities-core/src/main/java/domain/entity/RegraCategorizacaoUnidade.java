package domain.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Comparator;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Table;
import domain.enums.AbrangenciaConsolidacaoValor;
import domain.enums.Situacao;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;


/**
 * Regra de categorização de unidade operacional.
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
@Table(name = "T696RCTU")
public class RegraCategorizacaoUnidade implements Serializable, Comparable<RegraCategorizacaoUnidade> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6602335746949127006L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long sq_rgr_cat;

	@ManyToOne
	@JoinColumn(name = "SQ_DMN")
	private DominioQualificacao dominioQualificacao;

	@ManyToOne
	@JoinColumn(name = "SQ_ID_NT_CAT")
	private Indicador indicadorNotaCategorizacoes;

	@Column(name = "NM_RGR_CAT", length = 100, nullable = false, unique = false)
	private String nomeRegraCategorizacao;

	@Convert(converter = domain.enums.converters.AbrangenciaConsolidacaoValorConverter.class)
	@Column(name = "TP_ABG_NT_CAT", columnDefinition = "INT", nullable = true)
	private AbrangenciaConsolidacaoValor abrangenciaNotaCategorizacao;

	@Column(name = "NM_USU_ALT", length = 100, nullable = false, unique = false)
	private String nomeUsuarioAlteracao;

	@Column(name = "DH_ALT", nullable = false, columnDefinition = "DATETIME")
	private LocalDateTime dataAtualizacao;

	@Convert(converter = domain.enums.converters.SituacaoConverter.class)
	@Column(name = "ST_RGR_CAT", nullable = false, columnDefinition = "INT")
	private Situacao situacao;
	
	@Column(name = "MT_USU_ALT", length = 100, nullable = false, unique = false)
	private String matriculaUsuarioAlteracao;

	public Long getSq_rgr_cat() {
		return sq_rgr_cat;
	}

	public void setSq_rgr_cat(Long sq_rgr_cat) {
		this.sq_rgr_cat = sq_rgr_cat;
	}

	public DominioQualificacao getDominioQualificacao() {
		return dominioQualificacao;
	}

	public void setDominioQualificacao(DominioQualificacao dominioQualificacao) {
		this.dominioQualificacao = dominioQualificacao;
	}

	public Indicador getIndicadorNotaCategorizacoes() {
		return indicadorNotaCategorizacoes;
	}

	public void setIndicadorNotaCategorizacoes(Indicador indicadorNotaCategorizacoes) {
		this.indicadorNotaCategorizacoes = indicadorNotaCategorizacoes;
	}

	public String getNomeRegraCategorizacao() {
		return nomeRegraCategorizacao;
	}

	public void setNomeRegraCategorizacao(String nomeRegraCategorizacao) {
		this.nomeRegraCategorizacao = nomeRegraCategorizacao;
	}

	public AbrangenciaConsolidacaoValor getAbrangenciaNotaCategorizacao() {
		return abrangenciaNotaCategorizacao;
	}

	public void setAbrangenciaNotaCategorizacao(AbrangenciaConsolidacaoValor abrangenciaNotaCategorizacao) {
		this.abrangenciaNotaCategorizacao = abrangenciaNotaCategorizacao;
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

	public String getMatriculaUsuarioAlteracao() {
		return matriculaUsuarioAlteracao;
	}

	public void setMatriculaUsuarioAlteracao(String matriculaUsuarioAlteracao) {
		this.matriculaUsuarioAlteracao = matriculaUsuarioAlteracao;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	@Override
	public int compareTo(RegraCategorizacaoUnidade o) {
		return Comparator
                .comparing(RegraCategorizacaoUnidade::getSq_rgr_cat)
                .compare(this,  o);
	}
	
	

	

}

