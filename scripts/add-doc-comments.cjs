const fs = require('fs');
const path = require('path');

const ROOT = process.cwd();
const BACKEND_TARGETS = [
  path.join(ROOT, 'backend', 'src', 'main', 'java'),
  path.join(ROOT, 'backend', 'src', 'test', 'java'),
];
const FRONTEND_TARGETS = [
  path.join(ROOT, 'frontend', 'src'),
];
const SKIP_DIRS = new Set(['node_modules', '.git', 'dist', 'build', 'coverage', 'artifacts']);
const VALID_EXTENSIONS = new Set(['.java', '.ts', '.tsx']);

const JAVA_NON_METHOD_PREFIXES = [
  'return ',
  'new ',
  'throw ',
  'if ',
  'for ',
  'while ',
  'switch ',
  'catch ',
  'else ',
  'do ',
  'try ',
  'case ',
];

const TS_NON_METHOD_NAMES = new Set([
  'if',
  'for',
  'while',
  'switch',
  'catch',
  'else',
  'do',
  'try',
  'return',
  'const',
  'let',
  'var',
  'new',
]);

function walkFiles(dir, output) {
  if (!fs.existsSync(dir)) {
    return;
  }

  const entries = fs.readdirSync(dir, { withFileTypes: true });
  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      if (!SKIP_DIRS.has(entry.name)) {
        walkFiles(fullPath, output);
      }
      continue;
    }
    if (VALID_EXTENSIONS.has(path.extname(entry.name))) {
      output.push(fullPath);
    }
  }
}

function splitTopLevel(text, delimiterChar = ',') {
  const parts = [];
  let current = '';
  let angleDepth = 0;
  let parenDepth = 0;
  let braceDepth = 0;
  let bracketDepth = 0;
  let quote = null;
  let escaped = false;

  for (let i = 0; i < text.length; i += 1) {
    const char = text[i];

    if (quote !== null) {
      current += char;
      if (escaped) {
        escaped = false;
        continue;
      }
      if (char === '\\') {
        escaped = true;
        continue;
      }
      if (char === quote) {
        quote = null;
      }
      continue;
    }

    if (char === '"' || char === '\'' || char === '`') {
      quote = char;
      current += char;
      continue;
    }

    if (char === '<') {
      angleDepth += 1;
      current += char;
      continue;
    }
    if (char === '>') {
      angleDepth = Math.max(0, angleDepth - 1);
      current += char;
      continue;
    }
    if (char === '(') {
      parenDepth += 1;
      current += char;
      continue;
    }
    if (char === ')') {
      parenDepth = Math.max(0, parenDepth - 1);
      current += char;
      continue;
    }
    if (char === '{') {
      braceDepth += 1;
      current += char;
      continue;
    }
    if (char === '}') {
      braceDepth = Math.max(0, braceDepth - 1);
      current += char;
      continue;
    }
    if (char === '[') {
      bracketDepth += 1;
      current += char;
      continue;
    }
    if (char === ']') {
      bracketDepth = Math.max(0, bracketDepth - 1);
      current += char;
      continue;
    }

    if (
      char === delimiterChar &&
      angleDepth === 0 &&
      parenDepth === 0 &&
      braceDepth === 0 &&
      bracketDepth === 0
    ) {
      parts.push(current.trim());
      current = '';
      continue;
    }

    current += char;
  }

  if (current.trim() !== '') {
    parts.push(current.trim());
  }

  return parts;
}

function removeTopLevelDefault(param) {
  let angleDepth = 0;
  let parenDepth = 0;
  let braceDepth = 0;
  let bracketDepth = 0;
  let quote = null;
  let escaped = false;

  for (let i = 0; i < param.length; i += 1) {
    const char = param[i];

    if (quote !== null) {
      if (escaped) {
        escaped = false;
        continue;
      }
      if (char === '\\') {
        escaped = true;
        continue;
      }
      if (char === quote) {
        quote = null;
      }
      continue;
    }

    if (char === '"' || char === '\'' || char === '`') {
      quote = char;
      continue;
    }
    if (char === '<') {
      angleDepth += 1;
      continue;
    }
    if (char === '>') {
      angleDepth = Math.max(0, angleDepth - 1);
      continue;
    }
    if (char === '(') {
      parenDepth += 1;
      continue;
    }
    if (char === ')') {
      parenDepth = Math.max(0, parenDepth - 1);
      continue;
    }
    if (char === '{') {
      braceDepth += 1;
      continue;
    }
    if (char === '}') {
      braceDepth = Math.max(0, braceDepth - 1);
      continue;
    }
    if (char === '[') {
      bracketDepth += 1;
      continue;
    }
    if (char === ']') {
      bracketDepth = Math.max(0, bracketDepth - 1);
      continue;
    }

    if (char === '=' && angleDepth === 0 && parenDepth === 0 && braceDepth === 0 && bracketDepth === 0) {
      return param.slice(0, i).trim();
    }
  }

  return param.trim();
}

function humanizeName(name) {
  return name
    .replace(/[_$]+/g, ' ')
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .replace(/\s+/g, ' ')
    .trim()
    .toLowerCase();
}

function stripLeadingWord(words, word) {
  if (words === word) {
    return '';
  }
  const prefix = `${word} `;
  if (words.startsWith(prefix)) {
    return words.slice(prefix.length).trim();
  }
  return words;
}

function describeParam(name) {
  const lowered = name.toLowerCase();
  if (lowered === 'entity' || lowered === 'source' || lowered === 'model') {
    return 'The source object to transform.';
  }
  if (lowered === 'http' || lowered.includes('http') || lowered.includes('servletrequest')) {
    return 'The current HTTP request used to populate response metadata.';
  }
  if (lowered === 'payload') {
    return 'The message payload for this operation.';
  }
  if (lowered === 'event') {
    return 'The domain event payload consumed by this handler.';
  }
  if (lowered === 'registry') {
    return 'The configuration registry receiving component definitions.';
  }
  if (lowered === 'id' || lowered.endsWith('id')) {
    return `The ${humanizeName(name)} used to locate the target record.`;
  }
  if (lowered.includes('request') || lowered === 'req') {
    return `The request payload for this operation.`;
  }
  if (lowered.includes('response') || lowered === 'res') {
    return `The response object used by this operation.`;
  }
  if (lowered.includes('filter')) {
    return `The filter criteria applied to this operation.`;
  }
  if (lowered.includes('page')) {
    return `The pagination configuration.`;
  }
  if (lowered.includes('query') || lowered.includes('search')) {
    return `The search query text.`;
  }
  if (lowered.includes('options') || lowered === 'opts' || lowered === 'options') {
    return `Additional options that customize this operation.`;
  }
  if (lowered.includes('user')) {
    return `The user context for this operation.`;
  }
  return `The ${humanizeName(name) || 'input'} value.`;
}

function describeReturn(name, returnType, lang) {
  const loweredName = (name || '').toLowerCase();
  const loweredType = (returnType || '').toLowerCase();

  if (loweredName.startsWith('is') || loweredName.startsWith('has') || loweredName.startsWith('can')) {
    return 'True when the condition is satisfied; otherwise false.';
  }

  if (loweredType.includes('page<')) {
    return 'A paginated result set.';
  }
  if (loweredType.includes('list<') || loweredType.startsWith('list') || loweredType.endsWith('[]')) {
    return 'A list of matching items.';
  }
  if (loweredType.includes('apiresponse<')) {
    return 'A standard API response envelope containing operation data and request metadata.';
  }
  if (loweredType.includes('dto')) {
    return 'The mapped DTO representation.';
  }
  if (loweredType.includes('promise<void>') || loweredType === 'void') {
    return lang === 'ts' ? 'No value.' : 'No value is returned.';
  }
  if (loweredType.startsWith('promise<')) {
    return 'A promise that resolves with the operation result.';
  }
  if (loweredType.includes('jsx.element') || loweredType.includes('reactelement')) {
    return 'The rendered component tree.';
  }

  const words = humanizeName(name);
  if (words) {
    return `The result of ${words}.`;
  }
  return 'The operation result.';
}

function buildSummary(info, fileBaseName, ext) {
  if (info.constructor) {
    return `Creates a new ${fileBaseName} instance.`;
  }

  const name = info.name || '';
  if (ext === '.tsx' && /^[A-Z]/.test(name)) {
    return `Renders the ${name} component.`;
  }

  const words = humanizeName(name);
  if (!words) {
    return 'Executes this operation.';
  }

  const lowered = name.toLowerCase();
  const returnType = (info.returnType || '').replace(/\s+/g, '');
  if (lowered === 'setup') {
    return 'Initializes test fixtures.';
  }
  if (lowered.includes('should')) {
    return `Verifies that ${words}.`;
  }
  if (lowered.startsWith('to') && returnType.toLowerCase().includes('dto')) {
    return `Maps source data to ${returnType}.`;
  }
  if (lowered.startsWith('get')) {
    return `Retrieves ${stripLeadingWord(words, 'get') || 'data'}.`;
  }
  if (lowered.startsWith('find')) {
    return `Finds ${stripLeadingWord(words, 'find') || 'matching data'}.`;
  }
  if (lowered.startsWith('list')) {
    return `Lists ${stripLeadingWord(words, 'list') || 'matching data'}.`;
  }
  if (lowered.startsWith('fetch')) {
    return `Fetches ${stripLeadingWord(words, 'fetch') || 'data'}.`;
  }
  if (lowered.startsWith('my')) {
    return `Retrieves ${words}.`;
  }
  if (lowered.startsWith('create')) {
    return `Creates ${stripLeadingWord(words, 'create') || 'resource'}.`;
  }
  if (lowered.startsWith('build')) {
    return `Builds ${stripLeadingWord(words, 'build') || 'resource'}.`;
  }
  if (lowered.startsWith('generate')) {
    return `Generates ${stripLeadingWord(words, 'generate') || 'output'}.`;
  }
  if (lowered.startsWith('add')) {
    return `Adds ${stripLeadingWord(words, 'add') || 'item'}.`;
  }
  if (lowered.startsWith('update')) {
    return `Updates ${stripLeadingWord(words, 'update') || 'resource'}.`;
  }
  if (lowered.startsWith('set')) {
    return `Sets ${stripLeadingWord(words, 'set') || 'value'}.`;
  }
  if (lowered.startsWith('apply')) {
    return `Applies ${stripLeadingWord(words, 'apply') || 'changes'}.`;
  }
  if (lowered.startsWith('send')) {
    return `Sends ${stripLeadingWord(words, 'send') || 'message'}.`;
  }
  if (lowered.startsWith('push')) {
    return `Pushes ${stripLeadingWord(words, 'push') || 'message'}.`;
  }
  if (lowered.startsWith('broadcast')) {
    return `Broadcasts ${stripLeadingWord(words, 'broadcast') || 'message'}.`;
  }
  if (lowered.startsWith('delete')) {
    return `Deletes ${stripLeadingWord(words, 'delete') || 'resource'}.`;
  }
  if (lowered.startsWith('remove')) {
    return `Removes ${stripLeadingWord(words, 'remove') || 'item'}.`;
  }
  if (lowered.startsWith('clear')) {
    return `Clears ${stripLeadingWord(words, 'clear') || 'state'}.`;
  }
  if (lowered.startsWith('validate') || lowered.startsWith('check') || lowered.startsWith('verify')) {
    return `Validates ${words}.`;
  }
  if (lowered.startsWith('map') || lowered.startsWith('transform') || lowered.startsWith('convert')) {
    return `Transforms data for ${stripLeadingWord(words, 'map') || words}.`;
  }
  if (lowered.startsWith('on')) {
    return `Handles ${stripLeadingWord(words, 'on') || 'event'}.`;
  }
  if (lowered.startsWith('is') || lowered.startsWith('has') || lowered.startsWith('can')) {
    return `Determines whether ${words}.`;
  }
  return `Executes ${words}.`;
}

function parseJavaParams(paramsText) {
  const trimmed = paramsText.trim();
  if (!trimmed) {
    return [];
  }

  const rawParams = splitTopLevel(trimmed, ',');
  return rawParams
    .map((raw, index) => {
      const noAnnotation = raw.replace(/@\w+(?:\([^()]*\))?\s*/g, ' ');
      const noFinal = noAnnotation.replace(/\bfinal\b/g, ' ').trim();
      const match = noFinal.match(/([A-Za-z_$][\w$]*)\s*(?:\[\])?\s*$/);
      if (match) {
        return match[1];
      }
      return `param${index + 1}`;
    })
    .filter(Boolean);
}

function parseTsParams(paramsText) {
  const trimmed = paramsText.trim();
  if (!trimmed) {
    return [];
  }

  const rawParams = splitTopLevel(trimmed, ',');
  return rawParams
    .map((raw, index) => {
      const withoutDefault = removeTopLevelDefault(raw);
      if (!withoutDefault) {
        return null;
      }

      const cleaned = withoutDefault
        .replace(/\b(public|private|protected|readonly)\b/g, ' ')
        .trim();
      if (!cleaned) {
        return null;
      }

      if (cleaned.startsWith('{') || cleaned.startsWith('[')) {
        return `param${index + 1}`;
      }

      const noRest = cleaned.startsWith('...') ? cleaned.slice(3).trim() : cleaned;
      const match = noRest.match(/^([A-Za-z_$][\w$]*)/);
      if (match) {
        return match[1];
      }
      return `param${index + 1}`;
    })
    .filter(Boolean);
}

function parseThrows(throwsText) {
  const trimmed = (throwsText || '').trim();
  if (!trimmed) {
    return [];
  }

  return splitTopLevel(trimmed, ',')
    .map((item) => item.trim().replace(/^.*\./, ''))
    .filter(Boolean);
}

function parseJavaSignature(candidate, className) {
  const value = candidate.trim();
  if (
    value === '' ||
    value.startsWith('}') ||
    value.startsWith(')') ||
    value.startsWith('//') ||
    value.startsWith('*') ||
    value.includes(' class ') ||
    value.includes(' interface ') ||
    value.includes(' enum ') ||
    value.includes(' record ')
  ) {
    return null;
  }

  const lowered = value.toLowerCase();
  if (JAVA_NON_METHOD_PREFIXES.some((prefix) => lowered.startsWith(prefix))) {
    return null;
  }

  // Reject method-call chains like verify(...).save(...), which are statements, not declarations.
  if (/\)\s*\./.test(value)) {
    return null;
  }

  if (/^\}?\s*catch\s*\(/.test(value)) {
    return null;
  }

  if (value.includes('->')) {
    return null;
  }

  const parenIndex = value.indexOf('(');
  if (parenIndex < 0) {
    return null;
  }
  if (value.slice(0, parenIndex).includes('=')) {
    return null;
  }

  const cleaned = value.replace(/[;{]\s*$/, '').trim();

  const constructorMatch = cleaned.match(
    /^(?:(?:public|protected|private|static|final|abstract|default|synchronized|native|strictfp)\s+)*([A-Za-z_$][\w$]*)\s*\((.*)\)\s*(?:throws\s+(.+))?$/,
  );
  if (constructorMatch && constructorMatch[1] === className) {
    return {
      lang: 'java',
      name: constructorMatch[1],
      params: parseJavaParams(constructorMatch[2] || ''),
      returnType: null,
      throwsList: parseThrows(constructorMatch[3] || ''),
      constructor: true,
    };
  }

  const methodMatch = cleaned.match(
    /^(?:(?:public|protected|private|static|final|abstract|default|synchronized|native|strictfp)\s+)*(.+?)\s+([A-Za-z_$][\w$]*)\s*\((.*)\)\s*(?:throws\s+(.+))?$/,
  );
  if (!methodMatch) {
    return null;
  }

  const returnType = methodMatch[1].trim();
  if (!returnType || returnType.endsWith(")")) {
    return null;
  }

  return {
    lang: 'java',
    name: methodMatch[2],
    params: parseJavaParams(methodMatch[3] || ''),
    returnType,
    throwsList: parseThrows(methodMatch[4] || ''),
    constructor: false,
  };
}

function parseTsSignature(candidate) {
  const value = candidate.trim();
  if (
    value === '' ||
    value.startsWith('}') ||
    value.startsWith(')') ||
    value.startsWith('//') ||
    value.startsWith('*') ||
    value.startsWith('type ') ||
    value.startsWith('interface ')
  ) {
    return null;
  }

  if (/^\}?\s*catch\s*\(/.test(value)) {
    return null;
  }

  const normalized = value.replace(/\{\s*$/, '').trim();

  const functionMatch = normalized.match(
    /^(?:export\s+)?(?:default\s+)?(?:async\s+)?function\s+([A-Za-z_$][\w$]*)(?:<[^>]*>)?\s*\((.*)\)\s*(?::\s*(.+))?$/,
  );
  if (functionMatch) {
    return {
      lang: 'ts',
      name: functionMatch[1],
      params: parseTsParams(functionMatch[2] || ''),
      returnType: (functionMatch[3] || '').trim() || null,
      constructor: false,
    };
  }

  const arrowMatch = normalized.match(
    /^(?:export\s+)?(?:const|let|var)\s+([A-Za-z_$][\w$]*)\s*=\s*(?:async\s+)?(?:<[^>]*>\s*)?(?:\((.*)\)|([A-Za-z_$][\w$]*))\s*(?::\s*(.+))?\s*=>\s*(?:\()?$/,
  );
  if (arrowMatch) {
    const singleParam = arrowMatch[3] ? arrowMatch[3].trim() : '';
    const paramText = singleParam || (arrowMatch[2] || '');
    return {
      lang: 'ts',
      name: arrowMatch[1],
      params: parseTsParams(paramText),
      returnType: (arrowMatch[4] || '').trim() || null,
      constructor: false,
    };
  }

  if (!/\{\s*$/.test(value)) {
    return null;
  }

  const methodMatch = normalized.match(
    /^(?:public\s+|private\s+|protected\s+)?(?:static\s+)?(?:async\s+)?([A-Za-z_$][\w$]*)(?:<[^>]*>)?\s*\((.*)\)\s*(?::\s*(.+))?$/,
  );
  if (!methodMatch) {
    return null;
  }
  if (TS_NON_METHOD_NAMES.has(methodMatch[1])) {
    return null;
  }

  return {
    lang: 'ts',
    name: methodMatch[1],
    params: parseTsParams(methodMatch[2] || ''),
    returnType: (methodMatch[3] || '').trim() || null,
    constructor: methodMatch[1] === 'constructor',
  };
}

function buildCandidateFrom(lines, startIndex) {
  let endIndex = startIndex;
  let candidate = lines[startIndex].trim();

  const startsLikeInvocation =
    !/^(?:export\s+)?(?:const|let|var|function|async|public|private|protected|static)\b/.test(candidate) &&
    /^[A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)*\s*\(/.test(candidate) &&
    !candidate.includes(':') &&
    !candidate.includes('=');

  if (startsLikeInvocation) {
    return { candidate, endIndex };
  }

  if (!candidate.includes('(') || /[;{]\s*$/.test(candidate) || candidate.includes('=>')) {
    return { candidate, endIndex };
  }

  while (endIndex + 1 < lines.length) {
    endIndex += 1;
    candidate = `${candidate} ${lines[endIndex].trim()}`.trim();
    if (lines[endIndex].trim().startsWith('}')) {
      break;
    }
    if (/[;{]\s*$/.test(lines[endIndex].trim())) {
      break;
    }
  }

  return { candidate, endIndex };
}

function getDocBlockRangeAbove(lines, index) {
  let end = index - 1;
  while (end >= 0 && lines[end].trim() === '') {
    end -= 1;
  }
  if (end < 0 || !lines[end].trim().endsWith('*/')) {
    return null;
  }

  let start = end;
  while (start >= 0) {
    const value = lines[start].trim();
    if (value.startsWith('/**')) {
      return { start, end };
    }
    if (value.startsWith('/*') && !value.startsWith('/**')) {
      return null;
    }
    start -= 1;
  }
  return null;
}

function getDocBlockRangeAt(lines, index) {
  if (index < 0 || index >= lines.length) {
    return null;
  }
  if (!lines[index].trim().startsWith('/**')) {
    return null;
  }

  let end = index;
  while (end < lines.length && !lines[end].trim().endsWith('*/')) {
    end += 1;
  }
  if (end >= lines.length) {
    return null;
  }
  return { start: index, end };
}

function nextNonEmptyLineIndex(lines, index) {
  let current = index;
  while (current < lines.length && lines[current].trim() === '') {
    current += 1;
  }
  return current < lines.length ? current : -1;
}

function isLikelyStatementLine(line) {
  const value = line.trim();
  if (value === '') {
    return false;
  }

  if (/^\}?\s*catch\s*\(/.test(value)) {
    return true;
  }

  if (/^(?:if|for|while|switch|do|try|else|return|throw)\b/.test(value)) {
    return true;
  }

  if (/^(?:const|let|var)\b/.test(value)) {
    return true;
  }

  return /^[A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)*\s*\(/.test(value);
}

function isAutoGeneratedDocBlock(docLines) {
  const text = docLines.join('\n');
  return (
    /(?:Executes|Retrieves|Creates|Updates|Removes|Validates|Transforms|Determines)\s/.test(text) ||
    /@param\s+(?:param\d+|true|null)\b/.test(text) ||
    /The\s.+\svalue\./.test(text) ||
    /The result of\s/.test(text) ||
    /A promise that resolves with the operation result\./.test(text)
  );
}

function buildDocLines(info, indent, fileBaseName, ext) {
  const lines = [];
  lines.push(`${indent}/**`);
  lines.push(`${indent} * ${buildSummary(info, fileBaseName, ext)}`);

  const paramLines = info.params.map((param) =>
    info.lang === 'java'
      ? `${indent} * @param ${param} ${describeParam(param)}`
      : `${indent} * @param ${param} ${describeParam(param)}`,
  );

  const tagLines = [];
  if (paramLines.length > 0) {
    tagLines.push(...paramLines);
  }

  if (info.lang === 'java') {
    const returnType = (info.returnType || '').trim();
    if (!info.constructor && returnType && returnType.toLowerCase() !== 'void') {
      tagLines.push(`${indent} * @return ${describeReturn(info.name, returnType, 'java')}`);
    }
    for (const throwsType of info.throwsList || []) {
      tagLines.push(`${indent} * @throws ${throwsType} If the operation cannot be completed.`);
    }
  } else {
    if (!info.constructor) {
      const returnType = (info.returnType || '').trim() || 'unknown';
      tagLines.push(`${indent} * @returns ${describeReturn(info.name, returnType, 'ts')}`);
    }
  }

  if (tagLines.length > 0) {
    lines.push(`${indent} *`);
    lines.push(...tagLines);
  }

  lines.push(`${indent} */`);
  return lines;
}

function linesEqual(left, right) {
  if (left.length !== right.length) {
    return false;
  }
  for (let i = 0; i < left.length; i += 1) {
    if (left[i] !== right[i]) {
      return false;
    }
  }
  return true;
}

function processFile(filePath) {
  const source = fs.readFileSync(filePath, 'utf8');
  const eol = source.includes('\r\n') ? '\r\n' : '\n';
  const lines = source.split(/\r?\n/);
  const ext = path.extname(filePath);
  const baseName = path.basename(filePath, ext);

  let changed = false;
  let insertedCount = 0;
  let replacedCount = 0;
  let removedCount = 0;

  for (let index = 0; index < lines.length; index += 1) {
    const docRange = getDocBlockRangeAt(lines, index);
    if (!docRange) {
      continue;
    }

    const nextIndex = nextNonEmptyLineIndex(lines, docRange.end + 1);
    if (nextIndex < 0) {
      index = docRange.end;
      continue;
    }

    const { candidate } = buildCandidateFrom(lines, nextIndex);
    const info = ext === '.java' ? parseJavaSignature(candidate, baseName) : parseTsSignature(candidate);
    const docLines = lines.slice(docRange.start, docRange.end + 1);

    if (!info && isAutoGeneratedDocBlock(docLines) && isLikelyStatementLine(lines[nextIndex])) {
      lines.splice(docRange.start, docRange.end - docRange.start + 1);
      changed = true;
      removedCount += 1;
      index = Math.max(-1, docRange.start - 1);
      continue;
    }

    index = docRange.end;
  }

  for (let index = 0; index < lines.length; index += 1) {
    const { candidate, endIndex } = buildCandidateFrom(lines, index);
    const info = ext === '.java' ? parseJavaSignature(candidate, baseName) : parseTsSignature(candidate);
    if (!info) {
      index = Math.max(index, endIndex);
      continue;
    }

    let insertAt = index;
    while (insertAt > 0 && lines[insertAt - 1].trim().startsWith('@')) {
      insertAt -= 1;
    }

    const indent = (lines[insertAt].match(/^\s*/) || [''])[0];
    const newDocLines = buildDocLines(info, indent, baseName, ext);
    const existingRange = getDocBlockRangeAbove(lines, insertAt);

    if (!existingRange) {
      lines.splice(insertAt, 0, ...newDocLines);
      changed = true;
      insertedCount += 1;
      if (insertAt <= index) {
        index += newDocLines.length;
      }
      continue;
    }

    const currentDocLines = lines.slice(existingRange.start, existingRange.end + 1);
    if (linesEqual(currentDocLines, newDocLines)) {
      continue;
    }

    lines.splice(existingRange.start, existingRange.end - existingRange.start + 1, ...newDocLines);
    changed = true;
    replacedCount += 1;
    const delta = newDocLines.length - (existingRange.end - existingRange.start + 1);
    if (existingRange.start <= index) {
      index += delta;
    }
  }

  if (!changed) {
    return { changed: false, insertedCount: 0, replacedCount: 0, removedCount: 0 };
  }

  fs.writeFileSync(filePath, lines.join(eol), 'utf8');
  return { changed: true, insertedCount, replacedCount, removedCount };
}

function main() {
  const verbose = process.argv.includes('--verbose');
  const backendOnly = process.argv.includes('--backend-only');
  const frontendOnly = process.argv.includes('--frontend-only');
  let targets = [];
  if (backendOnly && frontendOnly) {
    targets = [...BACKEND_TARGETS, ...FRONTEND_TARGETS];
  } else if (backendOnly) {
    targets = BACKEND_TARGETS;
  } else if (frontendOnly) {
    targets = FRONTEND_TARGETS;
  } else {
    targets = [...BACKEND_TARGETS, ...FRONTEND_TARGETS];
  }
  const files = [];
  for (const target of targets) {
    walkFiles(target, files);
  }

  let touchedFiles = 0;
  let inserted = 0;
  let replaced = 0;
  let removed = 0;

  for (const filePath of files) {
    const result = processFile(filePath);
    if (result.changed) {
      touchedFiles += 1;
      inserted += result.insertedCount;
      replaced += result.replacedCount;
      removed += result.removedCount;
      if (verbose) {
        console.log(` - ${filePath}`);
      }
    }
  }

  console.log(`Updated ${touchedFiles} files: inserted ${inserted} docs, replaced ${replaced} docs, removed ${removed} stale docs.`);
}

main();
