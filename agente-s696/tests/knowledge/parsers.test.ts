import { readFileSync } from 'node:fs';
import path from 'node:path';
import { parseDdl } from '../../src/knowledge/parsers/ddl.js';
import { extrairDocumento } from '../../src/knowledge/parsers/documento.js';
import { parseMermaidEstados } from '../../src/knowledge/parsers/mermaid.js';
import { dividirEmTrechos, lerTextoUtf8 } from '../../src/knowledge/texto.js';

const docs = path.resolve(__dirname, '../../project-docs');
const aprendizado = readFileSync(path.join(docs, 'aprendizado.md'), 'utf8');
const cenario = readFileSync(
  path.join(docs, 'cvs-exemplo/analise-diretoria-csv.md'),
  'utf8',
);

describe('parseMermaidEstados', () => {
  it('lê estados, rótulos, transições e marca de início', () => {
    const diagrama = parseMermaidEstados(
      [
        'stateDiagram-v2',
        '[*] --> A : Criar',
        'A : Estado A',
        'A --> B : Avançar',
        'B --> [*]',
      ].join('\n'),
    );
    expect(diagrama?.estados.get('A')).toBe('Estado A');
    expect(diagrama?.estados.get('B')).toBe('B');
    expect(diagrama?.transicoes).toHaveLength(3);
  });

  it('ignora blocos que não são diagramas de estado', () => {
    expect(parseMermaidEstados('graph TD\nA-->B')).toBeUndefined();
  });
});

describe('parseDdl', () => {
  it('lê colunas, chave primária e chaves estrangeiras do cenário', () => {
    const blocos = [...cenario.matchAll(/```sql\r?\n([\s\S]*?)```/g)].map(
      (m) => m[1],
    );
    const tabelas = blocos.flatMap((bloco) => parseDdl(bloco).tabelas);
    expect(tabelas.map((t) => t.nome)).toEqual([
      'T696POAC',
      'T696ACES',
      'T696ETAC',
      'T696VIAC',
      'T696DOAC',
      'T696ENAC',
      'T696APID',
    ]);

    const portfolio = tabelas[0];
    expect(portfolio.colunas).toHaveLength(10);
    expect(portfolio.chavePrimaria).toEqual(['SQ_PTF_ACO']);
    expect(
      portfolio.chavesEstrangeiras.map((f) => f.tabelaReferenciada),
    ).toEqual(['T696PLEM', 'T696VARI']);
    expect(portfolio.colunas[0]).toMatchObject({
      nome: 'SQ_PTF_ACO',
      tipo: 'int',
      anulavel: false,
      identidade: true,
    });
    expect(portfolio.colunas.find((c) => c.nome === 'DE_HST')).toMatchObject({
      tipo: 'nvarchar(MAX)',
      anulavel: true,
    });

    const acoes = tabelas[1];
    expect(acoes.colunas.find((c) => c.nome === 'TP_ACO_ETT')).toMatchObject({
      tipo: 'int',
      padrao: '3',
      anulavel: false,
    });
    expect(acoes.colunas.find((c) => c.nome === 'VR_PSO')?.tipo).toBe(
      'decimal(5,2)',
    );
  });

  it('avisa quando um ALTER TABLE não tem o CREATE TABLE', () => {
    const { avisos } = parseDdl(
      'ALTER TABLE X ADD CONSTRAINT FK_X FOREIGN KEY (A) REFERENCES Y(B);',
    );
    expect(avisos[0]).toContain('não tem CREATE TABLE');
  });
});

describe('máquina de estados (amostra aprendizado.md, com lacunas)', () => {
  const documento = extrairDocumento('aprendizado.md', aprendizado);
  const estados = documento.itens.filter((item) => item.tipo === 'estado');

  it('carrega os estados do portfolio e marca como pendentes os sem código', () => {
    const lista = estados.filter((e) =>
      e.chave.startsWith(
        'estado:um-portfolio-pode-estar-nos-seguintes-estados:',
      ),
    );
    expect(lista.map((e) => e.titulo)).toEqual([
      'Em planejamento',
      'Em execução',
      'Em Replanejamento',
      'Em encerramento',
      'Encerrado',
    ]);
    const replanejamento = lista.find((e) => e.titulo === 'Em Replanejamento');
    expect(replanejamento?.status).toBe('pendente');
    expect(replanejamento?.pendencias.map((p) => p.codigo)).toEqual(
      expect.arrayContaining(['termos_comando', 'tabela_coluna', 'codigo']),
    );
    expect(lista[0].estruturado.codigo).toBe('1');
  });

  it('carrega os estados e as transições do diagrama', () => {
    const diagrama = estados.filter((e) =>
      e.chave.startsWith('estado:quando-em-replanejamentoele-pode-estar:'),
    );
    expect(diagrama).toHaveLength(10);
    expect(diagrama.every((e) => e.status === 'pendente')).toBe(true);

    const transicoes = documento.relacoes.filter((r) => r.tipo === 'transicao');
    expect(transicoes).toHaveLength(16);
    expect(
      transicoes.some(
        (r) =>
          r.origem.endsWith(':AnaliseDiretoria') &&
          r.destino.endsWith(':Edicao') &&
          r.rotulo === 'Retornar proposta para edição',
      ),
    ).toBe(true);
    const edicao = diagrama.find((e) => e.chave.endsWith(':Edicao'));
    expect(edicao?.estruturado.inicial).toBe('Criação da Proposta de Ação');
  });

  it('não inventa transição de rejeição a partir de AnaliseDiretoria', () => {
    const saidas = documento.relacoes.filter(
      (r) => r.tipo === 'transicao' && r.origem.endsWith(':AnaliseDiretoria'),
    );
    expect(saidas.map((r) => r.rotulo).join('|')).not.toMatch(/rejeit/i);
  });
});

describe('máquina de estados com tabela de estados', () => {
  const texto = [
    '# Proposta',
    '',
    '```mermaid',
    'stateDiagram-v2',
    '    AnaliseDiretoria : Proposta em Análise - Diretoria Executiva',
    '    Edicao : Proposta em Edição',
    '    AnaliseDiretoria --> Edicao : Retornar proposta para edição',
    '```',
    '',
    '| Estado (id no diagrama) | Rótulo de negócio | Termos de comando | Resumo | Onde aparece nos dados |',
    '|---|---|---|---|---|',
    '| AnaliseDiretoria | Proposta em Análise - Diretoria Executiva | análise da diretoria, em análise pela diretoria | Aguarda decisão da Diretoria | T696ACES.ST_ACO_ETT = 10 |',
    '| Fantasma | Estado fantasma | fantasma | x | T696ACES.ST_ACO_ETT = 99 |',
  ].join('\n');
  const documento = extrairDocumento('estados.md', texto);

  it('completa o estado com termos, resumo e identificação nos dados', () => {
    const estado = documento.itens.find((i) =>
      i.chave.endsWith(':AnaliseDiretoria'),
    );
    expect(estado?.status).toBe('ativo');
    expect(estado?.estruturado.onde).toEqual([
      { tabela: 'T696ACES', coluna: 'ST_ACO_ETT', valor: '10' },
    ]);
    expect(estado?.descricao).toContain('análise da diretoria');
    const termos = documento.sinonimos
      .filter((s) => s.equivalente === estado?.chave)
      .map((s) => s.termo);
    expect(termos).toEqual(
      expect.arrayContaining([
        'Proposta em Análise - Diretoria Executiva',
        'análise da diretoria',
        'em análise pela diretoria',
      ]),
    );
  });

  it('aponta divergências entre a tabela e o diagrama', () => {
    expect(documento.divergencias).toEqual(
      expect.arrayContaining([
        expect.stringContaining('"Estado fantasma" consta na tabela'),
        expect.stringContaining('"Proposta em Edição" consta no diagrama'),
      ]),
    );
  });

  it('o estado do diagrama sem linha na tabela fica pendente', () => {
    const edicao = documento.itens.find((i) => i.chave.endsWith(':Edicao'));
    expect(edicao?.status).toBe('pendente');
  });
});

describe('documento de cenário', () => {
  const documento = extrairDocumento('analise-diretoria-csv.md', cenario);

  it('carrega o esquema das sete tabelas e as chaves estrangeiras', () => {
    const esquemas = documento.itens.filter((i) => i.tipo === 'esquema');
    expect(esquemas).toHaveLength(7);
    expect(esquemas[0]).toMatchObject({
      chave: 'esquema:T696POAC',
      titulo: 'T696POAC - Portfolio',
      status: 'ativo',
    });
    const fks = documento.relacoes.filter((r) => r.tipo === 'fk');
    expect(fks.length).toBe(14);
    expect(fks.some((r) => r.destino === 'esquema:T696PLEM')).toBe(true);
  });

  it('avisa que os CSVs só serão tratados na Fase 3', () => {
    expect(documento.avisos.join(' ')).toContain('Fase 3');
  });
});

describe('utilitários de texto', () => {
  it('rejeita arquivo que não é UTF-8', () => {
    expect(() => lerTextoUtf8(Buffer.from([0x41, 0xe7, 0xe3, 0x6f]))).toThrow(
      'UTF-8',
    );
    expect(lerTextoUtf8(Buffer.from('Ação', 'utf8'))).toBe('Ação');
  });

  it('divide descrições longas respeitando o limite', () => {
    const texto = Array.from(
      { length: 40 },
      (_, i) => `- COLUNA_${i} varchar(100)`,
    ).join('\n');
    const trechos = dividirEmTrechos(texto, 200);
    expect(trechos.length).toBeGreaterThan(1);
    expect(trechos.every((t) => t.length <= 200)).toBe(true);
    expect(trechos.join('\n')).toBe(texto);
  });
});
