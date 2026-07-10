package domain.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;


/**
 * Associação entre {\@link AcaoEstrategica} e {\@link DominioQualificacao}.
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
@Table(name = "T696DOAC")
public class AcaoDominioQualificacao implements Serializable, Comparable<AcaoDominioQualificacao> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8150684452233412308L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long sq_id_dmn;
	
	@ManyToOne
	@JoinColumn(name = "sq_aco_ett")
	private AcaoEstrategica acaoEstrategica;
	
	@ManyToOne
	@JoinColumn(name = "sq_dmn")
	private DominioQualificacao dominioQualificacao;

	public AcaoEstrategica getAcaoEstrategica() {
		return acaoEstrategica;
	}

	public void setAcaoEstrategica(AcaoEstrategica acaoEstrategica) {
		this.acaoEstrategica = acaoEstrategica;
	}

	public DominioQualificacao getDominioQualificacao() {
		return dominioQualificacao;
	}

	public void setDominioQualificacao(DominioQualificacao dominioQualificacao) {
		this.dominioQualificacao = dominioQualificacao;
	}

	public Long getSq_id_dmn() {
		return sq_id_dmn;
	}

	public void setSq_id_dmn(Long sq_id_dmn) {
		this.sq_id_dmn = sq_id_dmn;
	}

	public static final long getSerialversionuid() {
		return serialVersionUID;
	}

	@Override
	public int compareTo(AcaoDominioQualificacao o) {
	    int compareAcaoEstrategica = this.acaoEstrategica.compareTo(o.getAcaoEstrategica());
	    if (compareAcaoEstrategica != 0) {
	        return compareAcaoEstrategica;
	    }
	    return this.dominioQualificacao.compareTo(o.getDominioQualificacao());
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(acaoEstrategica, dominioQualificacao);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AcaoDominioQualificacao other = (AcaoDominioQualificacao) obj;
		return Objects.equals(acaoEstrategica, other.acaoEstrategica)
				&& Objects.equals(dominioQualificacao, other.dominioQualificacao);
	}
}
