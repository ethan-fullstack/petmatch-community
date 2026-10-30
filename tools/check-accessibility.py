#!/usr/bin/env python3
"""Static regression check of field-error associations, not a WCAG audit."""
import argparse, pathlib, re, subprocess, sys
parser=argparse.ArgumentParser();parser.add_argument('--revision');args=parser.parse_args()
paths=['auth/register.html','pets/form.html','support-requests/form.html','support-applications/form.html']
missing=[];count=0
for path in paths:
    name='src/main/resources/templates/'+path
    text=subprocess.check_output(['git','show',args.revision+':'+name],text=True) if args.revision else pathlib.Path(name).read_text()
    for field in re.findall(r'th:errors="\*\{(\w+)\}"',text):
        count+=1
        if f"'{field}-error'" not in text or f'id="{field}-error"' not in text:
            missing.append(path+':'+field)
print(f'Campos con errores revisados: {count}; sin asociación accesible: {len(missing)}')
for item in missing: print(item)
sys.exit(bool(missing))
