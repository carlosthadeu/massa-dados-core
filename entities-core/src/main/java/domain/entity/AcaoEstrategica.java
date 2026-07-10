package domain.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.Nationalized;

import domain.enums.SituacaoAcaoEstrategica;


/**
 * Ação estratégica com ciclo de vida, metas, prazos, responsáveis e acompanhamento.
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
@Table(name = "T696ACES")
@NamedQuery(name = "consultaFiltros", query = "SELECT a FROM AcaoEstrategica a WHERE"
        + " (:nome IS NULL OR a.nome LIKE :nome) "
        + "AND (:origem IS NULL OR a.origemAcaoEstrategica = :origem) "
        + "AND (:unidade IS NULL OR a.unidadeOperacional = :unidade) "
        + "AND (:unidadeAprovacao IS NULL OR a.unidadeAprovacao = :unidadeAprovacao) "
        + "AND (:portfolio IS NULL OR a.portfolio = :portfolio) "
        + "AND (:situacao IS NULL OR a.situacao = :situacao) "
        + "AND ((:dataInicial IS NULL AND :dataFinal IS NULL) OR (a.dataLimite BETWEEN :dataInicial AND :dataFinal))")
@NamedQuery(name = "consultaFiltrosSemLimitantes", query = "SELECT a FROM AcaoEstrategica a WHERE"
        + " (:nome IS NULL OR a.nome LIKE :nome) "
        + "AND (:origem IS NULL OR a.origemAcaoEstrategica = :origem) "
        + "AND (:unidade IS NULL OR a.unidadeOperacional = :unidade) "
        + "AND (:unidadeAprovacao IS NULL OR a.unidadeAprovacao = :unidadeAprovacao) "
        + "AND (:portfolio IS NULL OR a.portfolio = :portfolio) "
        + "AND (:situacao IS NULL OR a.situacao = :situacao) "
        + "AND (a.situacao NOT IN (1,3,5)) "
        + "AND ((:dataInicial IS NULL AND :dataFinal IS NULL) OR (a.dataLimite BETWEEN :dataInicial AND :dataFinal))")
@NamedQuery(name = "consultaUnidadeSemlimitante", query = "SELECT a FROM AcaoEstrategica a WHERE "
        + "(a.unidadeOperacional = :unidade) "
        + "AND (a.portfolio = :portfolio) "
        + "AND (a.situacao NOT IN (1,3,5)) ")

public class AcaoEstrategica implements Serializable, Comparable<AcaoEstrategica> {
    /**
     *
     */
    private static final long serialVersionUID = -4805327755116079583L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sq_aco_ett;

    @Column(name = "NM_ACO_ETT", length = 100, nullable = false, unique = false)
    private String nome;

    @Column(name = "DE_ACO_ETT", length = 700, nullable = false, unique = false)
    private String descricao;

    @ManyToOne
    @JoinColumn(name = "SQ_ORI_ACO")
    private OrigemAcaoEstrategica origemAcaoEstrategica;

    @ManyToOne
    @JoinColumn(name = "SQ_PTF_ACO")
    private Portfolio portfolio;


    @Column(name = "DH_LIM", nullable = true, columnDefinition = "DATETIME")
    private LocalDate dataLimite;

    @ManyToOne
    @JoinColumn(name = "CD_UND")
    private UnidadeOperacional unidadeOperacional;

    @OneToMany(mappedBy = "acaoEstrategica", cascade = CascadeType.ALL, orphanRemoval = true)
    List<EtapaAcaoEstrategica> etapas;

    @OneToMany(mappedBy = "acaoEstrategica", cascade = CascadeType.ALL, orphanRemoval = true)
    List<VinculacaoAcaoEstrategica> vinculacaoAcaoEstrategicas;

    @Column(name = "MT_USU_ALT", length = 10, nullable = false, unique = false)
    private String matriculaUsuarioAlteracao;

    @Column(name = "NM_USU_ALT", length = 100, nullable = false, unique = false)
    private String nomeUsuarioAlteracao;

    @Column(name = "MT_USU_RSP", length = 10, nullable = true, unique = false)
    private String matriculaUsuarioResponsavel;

    @Column(name = "DH_ALT", nullable = false, columnDefinition = "DATETIME")
    private LocalDateTime dataAtualizacao;

    @Convert(converter = domain.enums.converters.SituacaoAcaoEstrategicaConverter.class)
    @Column(name = "ST_ACO_ETT", nullable = false, columnDefinition = "INT")
    private SituacaoAcaoEstrategica situacao;

    @Nationalized
    @Column(name = "DE_HST", length = Integer.MAX_VALUE, nullable = false, unique = false)
    private String historico;

    @ManyToOne
    @JoinColumn(name = "CD_UND_APR")
    private UnidadeOperacional unidadeAprovacao;

    public Long getSq_aco_ett() {
        return sq_aco_ett;
    }

    public void setSq_aco_ett(Long sq_aco_ett) {
        this.sq_aco_ett = sq_aco_ett;
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

    public OrigemAcaoEstrategica getOrigemAcaoEstrategica() {
        return origemAcaoEstrategica;
    }

    public void setOrigemAcaoEstrategica(OrigemAcaoEstrategica origemAcaoEstrategica) {
        this.origemAcaoEstrategica = origemAcaoEstrategica;
    }

    public Portfolio getPortfolio() {
        return portfolio;
    }

    public void setPortfolio(Portfolio portfolio) {
        this.portfolio = portfolio;
    }


    public LocalDate getDataLimite() {
        return dataLimite;
    }

    public void setDataLimite(LocalDate dataLimite) {
        this.dataLimite = dataLimite;
    }

    public UnidadeOperacional getUnidadeOperacional() {
        return unidadeOperacional;
    }

    public void setUnidadeOperacional(UnidadeOperacional unidadeOperacional) {
        this.unidadeOperacional = unidadeOperacional;
    }

    public SituacaoAcaoEstrategica getSituacao() {
        return situacao;
    }

    public void setSituacao(SituacaoAcaoEstrategica situacao) {
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

    public String getHistorico() {
        return historico;
    }

    public void setHistorico(String historico) {
        this.historico = historico;
    }

    public String getMatriculaUsuarioResponsavel() {
        return matriculaUsuarioResponsavel;
    }

    public void setMatriculaUsuarioResponsavel(String matriculaUsuarioResponsavelo) {
        this.matriculaUsuarioResponsavel = matriculaUsuarioResponsavelo;
    }

    public List<EtapaAcaoEstrategica> getEtapas() {
        if (this.etapas == null) this.etapas = new ArrayList<EtapaAcaoEstrategica>();
        return etapas;
    }


    public void setEtapas(List<EtapaAcaoEstrategica> etapas) {
        this.etapas = etapas;
    }

    public List<VinculacaoAcaoEstrategica> getVinculacaoAcaoEstrategicas() {
        return vinculacaoAcaoEstrategicas;
    }

    public void setVinculacaoAcaoEstrategicas(List<VinculacaoAcaoEstrategica> vinculacaoAcaoEstrategicas) {
        this.vinculacaoAcaoEstrategicas = vinculacaoAcaoEstrategicas;
    }

    public UnidadeOperacional getUnidadeAprovacao() {
        return unidadeAprovacao;
    }

    public void setUnidadeAprovacao(UnidadeOperacional unidadeAprovacao) {
        this.unidadeAprovacao = unidadeAprovacao;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    @Override
    public int compareTo(AcaoEstrategica o) {
        return Comparator.comparing(AcaoEstrategica::getSq_aco_ett).compare(this, o);
    }

}
