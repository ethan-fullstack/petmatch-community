#!/usr/bin/env python3
"""Read real Maven outputs; never substitute a zero for a missing report."""
import datetime, json, pathlib, subprocess, xml.etree.ElementTree as E
root=pathlib.Path(__file__).resolve().parents[1]
out=root/'docs/08-aplicacion-cronograma/evidencias';out.mkdir(parents=True,exist_ok=True)
reports=sorted((root/'target/surefire-reports').glob('TEST-*.xml'))
if not reports: raise SystemExit('Missing Surefire XML')
suites=[]
for path in reports:
    r=E.parse(path).getroot();suites.append({'suite':r.get('name'),**{k:int(r.get(k,'0')) for k in ['tests','failures','errors','skipped']}})
r=E.parse(root/'target/site/jacoco/jacoco.xml').getroot()
def lines(node):
    c=next(c for c in node.findall('counter') if c.get('type')=='LINE');covered=int(c.get('covered'));missed=int(c.get('missed'))
    return {'covered':covered,'missed':missed,'percent':round(100*covered/(covered+missed),2) if covered+missed else None}
packages={p.get('name'):lines(p) for p in r.findall('package')}
names=['AdminUserService','AdminUserController','AdminReportController','SupportRequestReportRepository','SupportRequestReportService','StatusReportAggregationStrategy','SupportTypeReportAggregationStrategy','SupportRequestReportFilter']
classes={c.get('name'):lines(c) for p in r.findall('package') for c in p.findall('class') if c.get('name').rsplit('/',1)[-1] in names}
result={'generated_utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'base_commit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip(),'working_tree':'implementation identified additionally by source-sha256.txt; docs may be updated later','database':'petmatch_test (MySQL 8.4.11)','suites':suites,'totals':{k:sum(s[k] for s in suites) for k in ['tests','failures','errors','skipped']},'line_coverage':lines(r),'packages':packages,'new_modules':classes}
(out/'metrics.json').write_text(json.dumps(result,indent=2,ensure_ascii=False)+'\n')
# Compact XML summaries and source fingerprints make this snapshot reviewable independently of ignored target/.
import shutil,hashlib
shutil.copyfile(root/'target/site/jacoco/jacoco.xml',out/'jacoco.xml')
for path in reports: shutil.copyfile(path,out/path.name)
files=sorted([p for p in (root/'src').rglob('*') if p.is_file()]+[root/'pom.xml'])
(out/'source-sha256.txt').write_text(''.join(hashlib.sha256(p.read_bytes()).hexdigest()+'  '+str(p.relative_to(root))+'\n' for p in files))
print(json.dumps({'totals':result['totals'],'coverage':result['line_coverage'],'new_modules':classes},indent=2))
