#!/usr/bin/env python3
"""Export five editable sources via Pandoc + Chromium, then validate PDF text."""
import hashlib, json, pathlib, subprocess, tempfile
from playwright.sync_api import sync_playwright
root=pathlib.Path(__file__).resolve().parents[1]
quality=root/'docs/08-aplicacion-cronograma/calidad';out=quality/'pdf';out.mkdir(exist_ok=True)
css='''@page {size:A4; margin:17mm;} body{font:11pt Arial,sans-serif;line-height:1.45;color:#162230;} h1{font-size:21pt} h2{font-size:15pt} h1,h2,h3{break-after:avoid} table{border-collapse:collapse;width:100%;font-size:9pt;overflow-wrap:anywhere} th,td{border:1px solid #9aa7b5;padding:6px;vertical-align:top} th{background:#e8eff5} pre,code{font-size:9pt;white-space:pre-wrap;overflow-wrap:anywhere} a{color:#165291;overflow-wrap:anywhere}'''
manifest=[]
with tempfile.TemporaryDirectory(prefix='petmatch-pdfs-') as tmp, sync_playwright() as playwright:
    browser=playwright.chromium.launch(executable_path='/usr/bin/chromium',headless=True)
    for source in sorted(quality.glob('[0-9][0-9]-*.md')):
        html=subprocess.check_output(['pandoc','--from=gfm','--to=html5',str(source)],text=True)
        page=pathlib.Path(tmp)/(source.stem+'.html');page.write_text('<!doctype html><html lang="es"><meta charset="UTF-8"><style>'+css+'</style><body>'+html+'</body></html>')
        pdf=out/(source.stem+'.pdf')
        tab=browser.new_page()
        tab.set_content(page.read_text(),wait_until='load')
        tab.pdf(path=str(pdf),format='A4',print_background=True,prefer_css_page_size=True)
        tab.close()
        text=subprocess.check_output(['pdftotext',str(pdf),'-'],text=True)
        title=source.read_text().splitlines()[0].lstrip('# ')
        # The complete product title must survive export, allowing layout whitespace.
        normalize=lambda s:' '.join(s.split())
        if normalize(title) not in normalize(text): raise RuntimeError('PDF title not preserved: '+str(pdf))
        info=subprocess.check_output(['pdfinfo',str(pdf)],text=True)
        pages=next(line.split(':',1)[1].strip() for line in info.splitlines() if line.startswith('Pages:'))
        manifest.append({'source':source.name,'source_sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'pdf':pdf.name,'pdf_sha256':hashlib.sha256(pdf.read_bytes()).hexdigest(),'pages':int(pages),'title_text_verified':True})
    browser.close()
(out/'manifest.json').write_text(json.dumps(manifest,indent=2,ensure_ascii=False)+'\n')
print(json.dumps(manifest,indent=2,ensure_ascii=False))
