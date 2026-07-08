package domain.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Comparator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Id;


/**
 * Parâmetro configurável do sistema.
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
@Table(name = "T696PARA")
public class Parametro implements Serializable, Comparable<Parametro> {
	@Id
	@Column(name = "CH_PRM", length = 15, nullable = false, columnDefinition = "varchar")
	private String chave;

	@Column(name = "NM_PRM", length = 100, nullable = false, columnDefinition = "varchar")
	private String nome;

	@Column(name = "DE_PRM", length = 255, nullable = true, columnDefinition = "varchar")
	private String descricao;

	@Column(name = "TP_PRM", nullable = true, columnDefinition = "int")
	private int tipoParametro;

	@Column(name = "VR_PRM", length = 200, nullable = true, columnDefinition = "varchar")
	private String valor;

	@Column(name = "MT_USU_ALT", length = 10, nullable = false, unique = false)
	private String matriculaUsuarioAlteracao;

	@Column(name = "NM_USU_ALT", length = 100, nullable = false, unique = false)
	private String nomeUsuarioAlteracao;

	@Column(name = "DH_ALT", nullable = false, columnDefinition = "DATETIME")
	private LocalDateTime dataAtualizacao;
	
	

	public String getChave() {
		return chave;
	}


	public void setChave(String chave) {
		this.chave = chave;
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


	public int getTipoParametro() {
		return tipoParametro;
	}


	public void setTipoParametro(int tipoParametro) {
		this.tipoParametro = tipoParametro;
	}


	public String getValor() {
		return valor;
	}


	public void setValor(String valor) {
		this.valor = valor;
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
	public int compareTo(Parametro o) {
		return Comparator
                .comparing(Parametro::getChave)
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

        Parametro other = (Parametro) obj;
        if (this.chave == null) {
            if (other.chave != null)
                return false;
        } else if (!this.chave.equals(other.chave))
            return false;
        return true;
	}

}

