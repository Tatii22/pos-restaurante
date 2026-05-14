import os
import re
from pathlib import Path

ROOT = Path('src')
JAVA_DIRS = [ROOT / 'main' / 'java', ROOT / 'test' / 'java']
removed_total = 0
files_changed = 0

for base in JAVA_DIRS:
    if not base.exists():
        continue
    for path in base.rglob('*.java'):
        text = path.read_text(encoding='utf-8')
        lines = text.splitlines()
        # collect import lines and their indices
        import_indices = []
        for i, line in enumerate(lines):
            stripped = line.strip()
            if stripped.startswith('import ') and not stripped.startswith('import static'):
                import_indices.append(i)
        if not import_indices:
            continue
        # determine search region (after last import)
        last_import_idx = import_indices[-1]
        search_text = "\n".join(lines[last_import_idx+1:])
        to_remove = []
        for idx in import_indices:
            line = lines[idx].strip()
            # skip wildcard imports
            if line.endswith('.*;'):
                continue
            # extract simple name
            m = re.search(r'import\s+([\w\.]+)\.([A-Za-z_][A-Za-z0-9_]*)\s*;', line)
            if not m:
                continue
            simple = m.group(2)
            # search for word boundary occurrences in search_text
            if re.search(r'\b' + re.escape(simple) + r'\b', search_text) is None:
                to_remove.append(idx)
        if not to_remove:
            continue
        # remove lines (create new lines list)
        new_lines = [l for i, l in enumerate(lines) if i not in to_remove]
        path.write_text("\n".join(new_lines) + "\n", encoding='utf-8')
        removed_total += len(to_remove)
        files_changed += 1

print(f"Files changed: {files_changed}")
print(f"Imports removed: {removed_total}")
