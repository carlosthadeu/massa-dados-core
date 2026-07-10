package domain.entity;

import domain.enums.SituacaoPortifolio;
import org.hibernate.annotations.Nationalized;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Comparator;


/**
 * Portfólio ou carteira de ações estratégicas.
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
@Table(name="T696POAC")
public class Portfolio implements Serializable, Comparable<Portfolio>  {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sq_ptf_aco;

    @Column(name = "NM_PTF", length=100, nullable=false)
    private String nome;

    @ManyToOne
    @JoinColumn(name = "SQ_PLJ_EMP")
    private PlanejamentoEmpresarial planejamentoEmpresarial;

    @ManyToOne
    @JoinColumn(name = "SQ_VRV")
    private Variavel indicadorAssociado;

    @Column(name = "AA_REA", nullable = false, columnDefinition = "INT")
    private Integer anoRealizacao;

    @Nationalized
    @Column(name = "DE_HST", length = Integer.MAX_VALUE, nullable = false, unique = false)
    private String historico;

    @Convert(converter = domain.enums.converters.SituacaoPortifolioConverter.class)
    @Column(name = "ST_PTF_ACO", nullable = false, columnDefinition = "INT")
    private SituacaoPortifolio situacao;

    @Column(name = "MT_USU_ALT", length = 10, nullable = false, unique = false)
    private String matriculaUsuarioAlteracao;

    @Column(name = "NM_USU_ALT", length = 100, nullable = false, unique = false)
    private String nomeUsuarioAlteracao;

    @Column(name = "DH_ALT", nullable = false, columnDefinition = "DATETIME")
    private LocalDateTime dataAtualizacao;

    @Override
    public int compareTo(Portfolio o) {
    	 return Comparator.comparing(Portfolio::getAnoRealizacao)
                 .thenComparing(Portfolio::getNome)
                 .compare(this, o);

    }

    public Long getSq_ptf_aco() {
        return sq_ptf_aco;
    }

    public void setSq_ptf_aco(Long sq_ptf_aco) {
        this.sq_ptf_aco = sq_ptf_aco;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public PlanejamentoEmpresarial getPlanejamentoEmpresarial() {
        return planejamentoEmpresarial;
    }

    public void setPlanejamentoEmpresarial(PlanejamentoEmpresarial planejamentoEmpresarial) {
        this.planejamentoEmpresarial = planejamentoEmpresarial;
    }

    public Variavel getIndicadorAssociado() {
        return indicadorAssociado;
    }

    public void setIndicadorAssociado(Variavel indicadorAssociado) {
        this.indicadorAssociado = indicadorAssociado;
    }

    public Integer getAnoRealizacao() {
        return anoRealizacao;
    }

    public void setAnoRealizacao(Integer anoRealizacao) {
        this.anoRealizacao = anoRealizacao;
    }

    public String getHistorico() {
        return historico;
    }

    public void setHistorico(String historico) {
        this.historico = historico;
    }

    public SituacaoPortifolio getSituacao() {
        return situacao;
    }

    public void setSituacao(SituacaoPortifolio situacao) {
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
}

