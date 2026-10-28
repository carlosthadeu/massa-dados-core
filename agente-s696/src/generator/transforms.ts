import { CsvDataset, CsvRecord, FieldMap, TransformSpec } from '../types.js';

function isValidDate(year: number, month: number, day: number): boolean {
  const date = new Date(Date.UTC(year, month - 1, day));
  return (
    date.getUTCFullYear() === year &&
    date.getUTCMonth() === month - 1 &&
    date.getUTCDate() === day
  );
}

function transformTimestamp(value: string): string {
  const match =
    /^(\d{4})-(\d{2})-(\d{2})(?:[ T](\d{2}):(\d{2}):(\d{2})(?:\.(\d{1,6}))?)?$/.exec(
      value,
    );
  if (!match) {
    throw new Error(`Invalid timestamp value: "${value}".`);
  }
  const [
    ,
    yearText,
    monthText,
    dayText,
    hourText = '00',
    minuteText = '00',
    secondText = '00',
    fraction = '',
  ] = match;
  const year = Number(yearText);
  const month = Number(monthText);
  const day = Number(dayText);
  const hour = Number(hourText);
  const minute = Number(minuteText);
  const second = Number(secondText);
  if (
    !isValidDate(year, month, day) ||
    hour > 23 ||
    minute > 59 ||
    second > 59
  ) {
    throw new Error(`Invalid timestamp value: "${value}".`);
  }
  const milliseconds = fraction.padEnd(3, '0').slice(0, 3);
  return `${yearText}-${monthText}-${dayText} ${hourText}:${minuteText}:${secondText}.${milliseconds}`;
}

export function applyTransform(
  value: string | null,
  spec: TransformSpec,
): string | null {
  if (spec.type === 'default') {
    if (value !== null && value !== '') {
      return value;
    }
    return spec.value === null ? null : String(spec.value);
  }
  if (value === null || value === '') {
    return null;
  }
  switch (spec.type) {
    case 'integer': {
      if (!/^-?\d+$/.test(value) || !Number.isSafeInteger(Number(value))) {
        throw new Error(`Invalid integer value: "${value}".`);
      }
      return String(Number(value));
    }
    case 'number': {
      const normalized = value.replace(',', '.');
      if (
        !/^-?\d+(?:\.\d+)?$/.test(normalized) ||
        !Number.isFinite(Number(normalized))
      ) {
        throw new Error(`Invalid numeric value: "${value}".`);
      }
      return normalized;
    }
    case 'timestamp':
      return transformTimestamp(value);
    case 'date':
      if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) {
        throw new Error(
          `Expected ${spec.format} date but received "${value}".`,
        );
      }
      if (
        !isValidDate(
          Number(value.slice(0, 4)),
          Number(value.slice(5, 7)),
          Number(value.slice(8, 10)),
        )
      ) {
        throw new Error(`Invalid date value: "${value}".`);
      }
      return value;
    case 'valueMap':
      return spec.values[value] ?? spec.fallback ?? value;
    case 'string':
      return value;
  }
}

export function transformRecord(
  dataset: CsvDataset,
  record: CsvRecord,
  fieldMaps: FieldMap[],
): CsvRecord {
  const transformed = { ...record };
  for (const fieldMap of fieldMaps) {
    if (fieldMap.targetTable === dataset.targetTable) {
      transformed[fieldMap.targetColumn] = applyTransform(
        transformed[fieldMap.source] ?? null,
        fieldMap.transform,
      );
    }
  }
  return transformed;
}
