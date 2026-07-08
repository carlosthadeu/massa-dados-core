package domain.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * Usuário do sistema.
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
public class Usuario {
	
	private String matricula;
	private String nome;
	private String senha;
	List<String> perfis = new ArrayList<String>();
	
	public String getMatricula() {
		return matricula;
	}
	public String getNome() {
		return nome;
	}
	public void setNome(String nome) {
		this.nome = nome;
	}
	public String getSenha() {
		return senha;
	}

	
	public void addPerfil(String perfil) {
		this.perfis.add(perfil);
	}
	
	public Usuario(String matricula, String nome, String senha) {
		super();
		this.matricula = matricula;
		this.nome = nome;
		this.senha = senha;
	}

	public String getSenhaUrl(){
		return senha.replace("@","%40")
				.replace(":","%3A")
				.replace("/","%2F")
				.replace("?","%3F")
				.replace("=","%3D")
				.replace("#","%23");
	}

	@Override
	public int hashCode() {
		return Objects.hash(matricula);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Usuario other = (Usuario) obj;
		return Objects.equals(matricula, other.matricula);
	}
	
	

}

