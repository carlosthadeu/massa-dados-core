import os from 'node:os';
import { loadConfig } from '../config.js';
import { carregar } from './carga.js';
import {
  provedorPostgres,
  RepositorioConhecimento,
  type Provedor,
} from './repo.js';
import {
  formatarCarga,
  formatarCargas,
  formatarMigracao,
  formatarPendencias,
  formatarVerificacao,
} from './relatorio.js';

const AJUDA = `Uso: npm run conhecimento -- <comando> [opções]

Comandos:
  migrar                              Cria/atualiza as tabelas conhecimento_*
  carregar <caminho...>               Carrega arquivos .md/.txt/.sql (ou diretórios)
      --dry-run                       Compara com o banco sem gravar
      --forcar                        Recarrega mesmo com o arquivo inalterado
      --executor <nome>               Quem faz a carga (padrão: usuário do sistema)
      --json                          Saída em JSON
  pendencias [--estado <texto>]       Lista itens com lacunas
  listar-cargas [--limite <n>]        Últimas cargas
  verificar                           Checa a consistência do que foi carregado
`;

interface Argumentos {
  posicionais: string[];
  opcoes: Map<string, string | true>;
}

const COM_VALOR = new Set(['executor', 'estado', 'limite']);

function lerArgumentos(args: string[]): Argumentos {
  const posicionais: string[] = [];
  const opcoes = new Map<string, string | true>();
  for (let i = 0; i < args.length; i++) {
    const arg = args[i];
    if (!arg.startsWith('--')) {
      posicionais.push(arg);
      continue;
    }
    const nome = arg.slice(2);
    if (COM_VALOR.has(nome)) {
      const valor = args[++i];
      if (valor === undefined)
        throw new Error(`A opção --${nome} exige um valor.`);
      opcoes.set(nome, valor);
    } else {
      opcoes.set(nome, true);
    }
  }
  return { posicionais, opcoes };
}

function mensagemConexao(erro: unknown): Error {
  const mensagem = erro instanceof Error ? erro.message : String(erro);
  const codigo = (erro as { code?: string }).code ?? '';
  if (/ECONNREFUSED|ENOTFOUND|ETIMEDOUT|ECONNRESET/.test(codigo + mensagem)) {
    return new Error(
      `Não foi possível conectar ao banco (${mensagem}). No plano gratuito da Aiven o serviço pode estar desligado por inatividade; ligue-o no console e tente de novo.`,
    );
  }
  return erro instanceof Error ? erro : new Error(mensagem);
}

export interface DependenciasCli {
  provedor?: Provedor;
  schema?: string;
  escrever?: (texto: string) => void;
}

export async function executarConhecimento(
  args: string[],
  deps: DependenciasCli = {},
): Promise<number> {
  const escrever = deps.escrever ?? ((t: string) => console.log(t));
  const { posicionais, opcoes } = lerArgumentos(args);
  const [comando, ...resto] = posicionais;
  const json = opcoes.has('json');
  const saida = (dados: unknown, texto: string): void =>
    escrever(json ? JSON.stringify(dados, null, 2) : texto);

  if (!comando || opcoes.has('help')) {
    escrever(AJUDA);
    return comando ? 0 : 1;
  }

  const criarRepo = (): RepositorioConhecimento =>
    new RepositorioConhecimento(
      deps.provedor ?? provedorPostgres(),
      deps.schema ?? loadConfig().databaseSchema,
    );

  try {
    switch (comando) {
      case 'migrar': {
        const r = await criarRepo().migrar();
        saida(r, formatarMigracao(r));
        return 0;
      }
      case 'carregar': {
        if (resto.length === 0) throw new Error('Informe ao menos um caminho.');
        let repo: RepositorioConhecimento | undefined;
        try {
          repo = criarRepo();
          await repo.buscarHashFonte('');
        } catch (erro) {
          if (!opcoes.has('dry-run')) throw mensagemConexao(erro);
          repo = undefined;
        }
        const executor =
          typeof opcoes.get('executor') === 'string'
            ? (opcoes.get('executor') as string)
            : os.userInfo().username;
        const r = await carregar(repo, resto, {
          dryRun: opcoes.has('dry-run'),
          forcar: opcoes.has('forcar'),
          executor,
        });
        saida(r, formatarCarga(r));
        return r.arquivos.some((a) => a.situacao === 'rejeitado') ? 1 : 0;
      }
      case 'pendencias': {
        const filtro = opcoes.get('estado');
        const r = await criarRepo().listarPendencias(
          typeof filtro === 'string' ? filtro : undefined,
        );
        saida(r, formatarPendencias(r));
        return 0;
      }
      case 'listar-cargas': {
        const limite = Number(opcoes.get('limite') ?? 10);
        const r = await criarRepo().listarCargas(
          Number.isInteger(limite) && limite > 0 ? limite : 10,
        );
        saida(r, formatarCargas(r));
        return 0;
      }
      case 'verificar': {
        const r = await criarRepo().verificar();
        saida(r, formatarVerificacao(r));
        return 0;
      }
      default:
        escrever(`Comando desconhecido: ${comando}\n\n${AJUDA}`);
        return 1;
    }
  } catch (erro) {
    throw mensagemConexao(erro);
  }
}
