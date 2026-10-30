#!/usr/bin/env python3
"""Validate relative file links in the new documentation and its entry links."""
import pathlib,re,sys,urllib.parse
root=pathlib.Path(__file__).resolve().parents[1]
files=list((root/'docs/08-aplicacion-cronograma').rglob('*.md'))+[root/'examples/java-jvm-memory/README.md']
missing=[]
for path in files:
    for link in re.findall(r'\]\(([^)]+)\)',path.read_text()):
        url=link.split('#',1)[0]
        if not url or '://' in url or url.startswith('mailto:'):continue
        target=path.parent/urllib.parse.unquote(url)
        if not target.exists():missing.append(f'{path.relative_to(root)} -> {url}')
print(f'{len(files)} documentos revisados; {len(missing)} enlaces locales rotos')
print('\n'.join(missing));sys.exit(bool(missing))
