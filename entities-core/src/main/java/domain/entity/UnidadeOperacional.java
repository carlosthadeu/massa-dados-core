package domain.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.NamedQuery;


/**
 * Unidade operacional da organização.
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
@Immutable
@Table(name = "V400UNOP")
@NamedQuery( name = "UnidadesAcaoEstrategica", 
query = "SELECT u FROM UnidadeOperacional u WHERE u.indicadorSituacao = 'A' and u.identificadorTipoOrgao IN :indicadores" )
public class UnidadeOperacional implements Serializable, Comparable<UnidadeOperacional> {
	
	@Id
	@Column(name="CD_UND_FIS", columnDefinition = "INT")
	private Long codigoUnidadeFisisca;
	
	@Column(name = "NM_UND", length = 45, columnDefinition = "CHAR")
    private String nomeUnidade;
	
	@Column(name = "CD_CAT_ORG", precision = 1, scale = 0, nullable = true)
	private BigDecimal codigoCategoriaOrgao;
	
	@Column(name="CD_NIV_HIE", columnDefinition = "INT")
	private Integer codigoNivelHierarquia;
	
	@Column(name="CD_ORG_PAI", columnDefinition = "INT") 
    private Integer codigoOrgaoPai;
	
	@Column(name = "ID_SIT", length = 45, columnDefinition = "CHAR")
    private String indicadorSituacao;
	
	@Column(name="DT_FIM_ATV", columnDefinition = "DATETIME")
	private LocalDate dataInicioAtividade;
     
	@Column(name = "ID_RSP_CCU", length = 1, columnDefinition = "CHAR") 
    private String indicadorResponsavelCusto;
	
	@Column(name = "ID_TP_ORG", precision = 1, scale = 0, nullable = true)
	private BigDecimal identificadorTipoOrgao;
	
	
	public Long getCodigoUnidadeFisisca() {
		return codigoUnidadeFisisca;
	}


	public String getNomeUnidade() {
		return nomeUnidade;
	}


	public BigDecimal getCodigoCategoriaOrgao() {
		return codigoCategoriaOrgao;
	}


	public Integer getCodigoNivelHierarquia() {
		return codigoNivelHierarquia;
	}


	public Integer getCodigoOrgaoPai() {
		return codigoOrgaoPai;
	}


	public String getIndicadorSituacao() {
		return indicadorSituacao;
	}


	public LocalDate getDataInicioAtividade() {
		return dataInicioAtividade;
	}


	public String getIndicadorResponsavelCusto() {
		return indicadorResponsavelCusto;
	}


	public BigDecimal getIdentificadorTipoOrgao() {
		return identificadorTipoOrgao;
	}


	@Override
	public int compareTo(UnidadeOperacional o) {
		return Comparator
                .comparing(UnidadeOperacional::getCodigoUnidadeFisisca)
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

        UnidadeOperacional other = (UnidadeOperacional) obj;
        if (this.codigoUnidadeFisisca == null) {
            if (other.codigoUnidadeFisisca != null)
                return false;
        } else if (!this.codigoUnidadeFisisca.equals(other.codigoUnidadeFisisca))
            return false;
        return true;
	}
    

}

