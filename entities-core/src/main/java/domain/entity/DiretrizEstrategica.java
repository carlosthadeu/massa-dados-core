package domain.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.NamedQuery;

import domain.enums.Situacao;
import jakarta.persistence.GenerationType;


/**
 * Diretriz estratégica vinculada a um {\@link PlanejamentoEmpresarial}.
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
@Table(name="T696DIES", uniqueConstraints = { @UniqueConstraint(columnNames = { "SQ_PLJ_EMP", "DE_DRT_ETT" }) })
@NamedQuery(name = "DiretrizEstrategicaPesquisarPelosFiltros", 
query = "SELECT de FROM DiretrizEstrategica de "
		+ "where (de.planejamentoEmpresarial = :planejamentoEmpresarial OR :planejamentoEmpresarial IS NULL) "
		+ "and (de.descricao LIKE :descricao OR  :descricao IS NULL) "
		+ "and (de.situacao = :situacao OR  :situacao IS NULL ) "
		+ "and (:descricaoOe IS NULL OR EXISTS ( select oe from de.orientacoesEstrategicas oe "
		+ "where oe.descricao like :descricaoOe and oe.diretrizEstrategica = de )) ")

@NamedQuery(name = "DiretrizEstrategicaQuantidadePelosFiltros", 
query = "SELECT count(de) FROM DiretrizEstrategica de "
		+ "where (de.planejamentoEmpresarial = :planejamentoEmpresarial OR :planejamentoEmpresarial IS NULL) "
		+ "and (de.descricao LIKE :descricao OR  :descricao IS NULL) "
		+ "and (de.situacao = :situacao OR  :situacao IS NULL ) "
		+  "and (:descricaoOe IS NULL OR EXISTS ( select oe from de.orientacoesEstrategicas oe "
		+ "where oe.descricao = :descricaoOe and oe.diretrizEstrategica = de )) ")
public class DiretrizEstrategica implements Serializable, Comparable<DiretrizEstrategica> {

	public void setOrientacoesEstrategicas(Set<OrientacaoEstrategica> orientacoesEstrategicas) {
		this.orientacoesEstrategicas = orientacoesEstrategicas;
	}


	/**
	 * 
	 */
	private static final long serialVersionUID = 9039752437941010089L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long sq_drt_ett;
	
	@ManyToOne
    @JoinColumn(name = "SQ_PLJ_EMP")
	private PlanejamentoEmpresarial planejamentoEmpresarial;
	
	@OneToMany(mappedBy = "diretrizEstrategica", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OrientacaoEstrategica> orientacoesEstrategicas = new HashSet<>();
	
	@Column(name = "DE_DRT_ETT", length=50, nullable=false)
	private String descricao;
	
	@Convert(converter = domain.enums.converters.SituacaoConverter.class)
	@Column(name = "ST_DRT_ETT", nullable=false, columnDefinition = "INT")
	private Situacao situacao;
	
	@Column(name="MT_USU_ALT", length=10, nullable=false, unique=false)
	private String matriculaUsuarioAlteracao;
	
	@Column(name="NM_USU_ALT", length=100, nullable=false, unique=false)
	private String nomeUsuarioAlteracao; 
	
	@Column(name="DH_ALT", nullable=false, columnDefinition = "DATETIME")
	private LocalDateTime dataAtualizacao;

	public Long getSq_drt_ett() {
		return sq_drt_ett;
	}

	public void setSq_drt_ett(Long sq_drt_ett) {
		this.sq_drt_ett = sq_drt_ett;
	}

	public PlanejamentoEmpresarial getPlanejamentoEmpresarial() {
		return planejamentoEmpresarial;
	}

	public void setPlanejamentoEmpresarial(PlanejamentoEmpresarial planejamentoEmpresarial) {
		this.planejamentoEmpresarial = planejamentoEmpresarial;
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

	public Set<OrientacaoEstrategica> getOrientacoesEstrategicas() {
		return orientacoesEstrategicas;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	@Override
	public int compareTo(DiretrizEstrategica o) {
		return Comparator
                .comparing(DiretrizEstrategica::getSq_drt_ett)
                .compare(this,  o);
	}

	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        DiretrizEstrategica other = (DiretrizEstrategica) obj;
        if (sq_drt_ett == null) {
            if (other.sq_drt_ett != null)
                return false;
        } else if (!sq_drt_ett.equals(other.sq_drt_ett))
            return false;
        return true;
	}
	
	
}

