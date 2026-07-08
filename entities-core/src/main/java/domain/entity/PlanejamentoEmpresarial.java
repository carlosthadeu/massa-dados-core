package domain.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Comparator;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.NamedQuery;
import org.hibernate.annotations.Nationalized;
import domain.enums.SituacaoPlanejamentoEmpresarial;


/**
 * Planejamento empresarial (ex: PE 2025-2029).
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
@Table(name="T696PLEM")
@NamedQuery(name = "PlanejamentoEmpresarialMaisAtual", 
query = "SELECT pe FROM PlanejamentoEmpresarial pe  "
		+ "WHERE pe.inicioVigencia = (SELECT MAX(m.inicioVigencia) FROM PlanejamentoEmpresarial m)")
@NamedQuery(name = "PlanejamentosEmpresariaisNaoEncerrados", 
query = "SELECT pe FROM PlanejamentoEmpresarial pe  "
		+ "WHERE pe.situacao != :situacaoo")
public class PlanejamentoEmpresarial implements Serializable, Comparable<PlanejamentoEmpresarial> {
	/**
	 * 
	 */
	private static final long serialVersionUID = -563432254225793068L;

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long sq_plj_emp;

	@Column(name = "NM_PLJ_EMP", length = 100, nullable = false, unique = false)
	private String nome;
	
	@Column(name="DT_INI_VIG", nullable=false, columnDefinition = "DATETIME")
	private LocalDateTime inicioVigencia;
	
	@Column(name="DT_FIN_VIG", nullable=false, columnDefinition = "DATETIME")
	private LocalDateTime fimVigencia;
	
	@Nationalized
	@Column(name="DE_HST", length = Integer.MAX_VALUE, nullable=false, unique=false)
	private String historico;
	
	@Column(name="MT_USU_ALT", length=10, nullable=false, unique=false)
	private String matriculaUsuarioAlteracao;
	
	@Column(name="NM_USU_ALT", length=10, nullable=false, unique=false)
	private String nomeUsuarioAlteracao; 
	
	@Column(name="DH_ALT", nullable=false, columnDefinition = "DATETIME")
	private LocalDateTime dataAtualizacao;
	
	@Convert(converter = domain.enums.converters.SituacaoPlanejamentoEmpresarialConverter.class)
	@Column(name = "ST_PLJ_EMP", nullable=false, columnDefinition = "INT")
	private SituacaoPlanejamentoEmpresarial situacao;

	public Long getSq_plj_emp() {
		return sq_plj_emp;
	}

	public void setSq_plj_emp(Long sq_plj_emp) {
		this.sq_plj_emp = sq_plj_emp;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public LocalDateTime getInicioVigencia() {
		return inicioVigencia;
	}
	
	

	public void setInicioVigencia(LocalDateTime inicioVigencia) {
		this.inicioVigencia = inicioVigencia;
	}

	public LocalDateTime getFimVigencia() {
		return fimVigencia;
	}

	public void setFimVigencia(LocalDateTime fimVigencia) {
		this.fimVigencia = fimVigencia;
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

	public SituacaoPlanejamentoEmpresarial getSituacao() {
		return situacao;
	}

	public void getSituacao(SituacaoPlanejamentoEmpresarial situacao) {
		this.situacao = situacao;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        PlanejamentoEmpresarial other = (PlanejamentoEmpresarial) obj;
        if (sq_plj_emp == null) {
            if (other.sq_plj_emp != null)
                return false;
        } else if (!sq_plj_emp.equals(other.sq_plj_emp))
            return false;
        return true;
	}

	public void setSituacao(SituacaoPlanejamentoEmpresarial situacao) {
		this.situacao = situacao;
	}

	@Override
	public int compareTo(PlanejamentoEmpresarial o) {
		return Comparator
				.comparing(PlanejamentoEmpresarial::getSq_plj_emp)
				.compare(this, o);
	}
	
}

