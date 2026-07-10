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
 * Vinculação entre ações estratégicas.
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
@Table(name="T696VIAC")
public class VinculacaoAcaoEstrategica implements Serializable, Comparable<VinculacaoAcaoEstrategica> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1236813528437462777L;
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long sq_vnc_aco;
	
	@ManyToOne
	@JoinColumn(name = "SQ_ACO_ETT")
	private AcaoEstrategica acaoEstrategica;
	
	@ManyToOne
	@JoinColumn(name = "SQ_ORT_ETT")
	private OrientacaoEstrategica orientacaoEstrategica;

	public Long getSq_vnc_aco() {
		return sq_vnc_aco;
	}

	public void setSq_vnc_aco(Long sq_vnc_aco) {
		this.sq_vnc_aco = sq_vnc_aco;
	}

	public AcaoEstrategica getAcaoEstrategica() {
		return acaoEstrategica;
	}

	public void setAcaoEstrategica(AcaoEstrategica acaoEstrategica) {
		this.acaoEstrategica = acaoEstrategica;
	}

	public OrientacaoEstrategica getOrientacaoEstrategica() {
		return orientacaoEstrategica;
	}

	public void setOrientacaoEstrategica(OrientacaoEstrategica orientacaoEstrategica) {
		this.orientacaoEstrategica = orientacaoEstrategica;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	@Override
	public int compareTo(VinculacaoAcaoEstrategica o) {
	    int result = this.acaoEstrategica.getPortfolio().compareTo(o.acaoEstrategica.getPortfolio());
	    if (result == 0) {
	        result = this.orientacaoEstrategica.getDescricao().compareTo(o.orientacaoEstrategica.getDescricao());
	    }
	    return result;
	}
	
	protected VinculacaoAcaoEstrategica() {
		
	}

	public VinculacaoAcaoEstrategica(AcaoEstrategica acaoEstrategica, OrientacaoEstrategica orientacaoEstrategica) {
		super();
		this.acaoEstrategica = acaoEstrategica;
		this.orientacaoEstrategica = orientacaoEstrategica;
	}

	@Override
	public int hashCode() {
		return Objects.hash(orientacaoEstrategica, sq_vnc_aco);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VinculacaoAcaoEstrategica other = (VinculacaoAcaoEstrategica) obj;
		return Objects.equals(orientacaoEstrategica, other.orientacaoEstrategica)
				&& Objects.equals(sq_vnc_aco, other.sq_vnc_aco);
	}

	
	
}

