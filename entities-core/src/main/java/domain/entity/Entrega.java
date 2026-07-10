package domain.entity;

import domain.enums.SituacaoEtapa;
import org.hibernate.annotations.Nationalized;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;


/**
 * Entrega vinculada a uma {\@link EtapaAcaoEstrategica}.
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
@Table(name="T696ENAC")
public class Entrega implements Serializable, Comparable<Entrega> {

    private static final long serialVersionUID = 202509260954L;

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long sq_ent_aco;

    @ManyToOne
    @JoinColumn(name="SQ_ETP_ACO")
    private EtapaAcaoEstrategica etapaAcaoEstrategica;

    @Column(name = "MM_ENT", nullable = false, columnDefinition = "INT")
    private Integer mesVigenciaEntrega;

    @Column(name="PC_PLJ_ENT", nullable=true, columnDefinition="decimal", precision=5, scale=2)
    private BigDecimal planejamentoEntrega;

    @Column(name="PC_REA_ENT", nullable=true, columnDefinition="decimal", precision=5, scale=2)
    private BigDecimal realizadoEntrega;

    @Nationalized
    @Column(name = "DE_REA_ENT", length = Integer.MAX_VALUE, nullable = false, unique = false)
    private String descricaoRealizado;

    @Convert(converter = domain.enums.converters.SituacaoEtapaConverter.class)
    @Column(name = "ST_ENT_ACO", nullable = false, columnDefinition = "INT")
    private SituacaoEtapa situacao;

    @Nationalized
    @Column(name = "DE_HST", length = Integer.MAX_VALUE, nullable = false, unique = false)
    private String historico;

    @Column(name = "MT_USU_ALT", length = 10, nullable = false, unique = false)
    private String matriculaUsuarioAlteracao;

    @Column(name = "NM_USU_ALT", length = 100, nullable = false, unique = false)
    private String nomeUsuarioAlteracao;

    @Column(name = "DH_ALT", nullable = false, columnDefinition = "DATETIME")
    private LocalDateTime dataAtualizacao;

    public Long getSq_ent_aco() {
        return sq_ent_aco;
    }

    public void setSq_ent_aco(Long sq_ent_aco) {
        this.sq_ent_aco = sq_ent_aco;
    }

    public EtapaAcaoEstrategica getEtapaAcaoEstrategica() {
        return etapaAcaoEstrategica;
    }

    public void setEtapaAcaoEstrategica(EtapaAcaoEstrategica etapaAcaoEstrategica) {
        this.etapaAcaoEstrategica = etapaAcaoEstrategica;
    }

    public Integer getMesVigenciaEntrega() {
        return mesVigenciaEntrega;
    }

    public void setMesVigenciaEntrega(Integer mesVigenciaEntrega) {
        this.mesVigenciaEntrega = mesVigenciaEntrega;
    }

    public BigDecimal getPlanejamentoEntrega() {
        return planejamentoEntrega;
    }

    public void setPlanejamentoEntrega(BigDecimal planejamentoEntrega) {
        this.planejamentoEntrega = planejamentoEntrega;
    }

    public BigDecimal getRealizadoEntrega() {
        return realizadoEntrega;
    }

    public void setRealizadoEntrega(BigDecimal realizadoEntrega) {
        this.realizadoEntrega = realizadoEntrega;
    }

    public String getDescricaoRealizado() {
        return descricaoRealizado;
    }

    public void setDescricaoRealizado(String descricaoRealizado) {
        this.descricaoRealizado = descricaoRealizado;
    }

    public SituacaoEtapa getSituacao() {
        return situacao;
    }

    public void setSituacao(SituacaoEtapa situacao) {
        this.situacao = situacao;
    }

    public String getHistorico() {
        return historico;
    }

    public void setHistorico(String historico) {
        this.historico = historico;
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
    public int compareTo(Entrega outraEntrega) {
        if (this.sq_ent_aco == null && outraEntrega.sq_ent_aco == null) return 0;
        if (this.sq_ent_aco == null) return -1;
        if (outraEntrega.sq_ent_aco == null) return 1;
        return this.sq_ent_aco.compareTo(outraEntrega.sq_ent_aco);

    }
}

