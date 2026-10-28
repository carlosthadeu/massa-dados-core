import { PreviewOptions } from '../types.js';

export interface ParsedInstruction {
  options: PreviewOptions;
  clarifications: Array<
    | { kind: 'actionsCount' }
    | { kind: 'stepsPerAction' }
    | { kind: 'actionsPerUnit'; count?: number }
  >;
}

function extractNumber(patterns: RegExp[], input: string): number | undefined {
  for (const pattern of patterns) {
    const match = input.match(pattern);
    if (match?.[1]) {
      return Number(match[1]);
    }
  }
  return undefined;
}

export function parseInstruction(input: string): ParsedInstruction {
  const normalized = input.trim().replace(/\s+/g, ' ');
  if (!normalized) {
    throw new Error('Provide an instruction after --instr.');
  }

  const actionsCount = extractNumber(
    [/\b(\d+)\s+ações?\b/i, /\b(\d+)\s+acoes?\b/i],
    normalized,
  );
  const stepsPerAction = extractNumber(
    [
      /\b(\d+)\s+etapas?\s+(?:por|em cada|para cada)\s+ação\b/i,
      /\b(\d+)\s+etapas?\s+(?:por|em cada|para cada)\s+acao\b/i,
      /\b(?:cada|por)\s+ação\s+(\d+)\s+etapas?\b/i,
      /\b(?:cada|por)\s+acao\s+(\d+)\s+etapas?\b/i,
      /\b(\d+)\s+etapas?\b/i,
    ],
    normalized,
  );
  const perUnitActionCount =
    /\b(?:\d+\s+)?ações?\s+(?:por|em cada)\s+unidade\b|\b(?:\d+\s+)?acoes?\s+(?:por|em cada)\s+unidade\b/i.test(
      normalized,
    );
  const vagueAmount = /\b(?:algumas?|várias?|varias?|poucas?|muitas?)\b/i.test(
    normalized,
  );
  const mentionsActions = /\bações?\b|\bacoes?\b/i.test(normalized);
  const mentionsStages = /\betapas?\b/i.test(normalized);
  const portfolioName =
    normalized.match(
      /\b(?:chamado|chamada|com nome|nomeado|nomeada)\s+["'“]([^"'”]+)["'”]/i,
    )?.[1] ??
    normalized
      .match(
        /\b(?:chamado|chamada|com nome|nomeado|nomeada)\s+([\p{L}\p{N}][\p{L}\p{N} _-]*?)(?=\s+(?:com|para|e|que)\b|$)/iu,
      )?.[1]
      ?.trim();

  const clarifications: ParsedInstruction['clarifications'] = [];
  if (perUnitActionCount) {
    clarifications.push({ kind: 'actionsPerUnit', count: actionsCount });
  } else if (vagueAmount && mentionsActions && actionsCount === undefined) {
    clarifications.push({ kind: 'actionsCount' });
  }
  if (vagueAmount && mentionsStages && stepsPerAction === undefined) {
    clarifications.push({ kind: 'stepsPerAction' });
  }

  return {
    options: {
      ...(perUnitActionCount || actionsCount === undefined
        ? {}
        : { actionsCount }),
      ...(stepsPerAction === undefined ? {} : { stepsPerAction }),
      ...(portfolioName ? { portfolioName } : {}),
      ...(/\b(?:próximo ano|proximo ano|ano seguinte|ano subsequente|ano que vem|next[_\s-]?year)\b/i.test(
        normalized,
      )
        ? { ensureNextYearStep: true }
        : {}),
      ...(/\b(?:atrasad[ao]s?|em atraso|delayed)\b/i.test(normalized)
        ? { ensureDelayedStep: true }
        : {}),
    },
    clarifications,
  };
}
