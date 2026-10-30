#!/usr/bin/env python3
"""Optional local browser smoke. Requires Playwright Python + Chromium and an app on 8081.
Only uses the explicitly isolated petmatch_browser_test schema in a local Docker container.
It creates synthetic accounts; no real credentials or production data are accepted.
"""
import datetime, json, pathlib, secrets, subprocess, uuid, urllib.request
from playwright.sync_api import sync_playwright, expect
BASE='http://127.0.0.1:8081'
OUT=pathlib.Path('target/browser-smoke');OUT.mkdir(parents=True,exist_ok=True)
suffix=uuid.uuid4().hex[:10];password=secrets.token_urlsafe(18)
emails={r:f'{r}-{suffix}@example.test' for r in ['owner','helper','admin']}
# Guard operator bootstrap: the app process must be separately configured for this DB.
subprocess.run(['docker','exec','petmatch-mysql','mysql','-uroot','petmatch_browser_test','-Nse','SELECT 1'],check=True,capture_output=True)
results=[]
with sync_playwright() as p:
    browser=p.chromium.launch(executable_path='/usr/bin/chromium',headless=True)
    contexts={r:browser.new_context(viewport={'width':1280,'height':900}) for r in emails}
    # Fetch the public CDN asset through the environment's verified HTTPS proxy.
    # Chromium lacks that proxy CA; do not ignore certificate errors.
    with urllib.request.urlopen('https://cdn.jsdelivr.net/npm/@tailwindcss/browser@4',timeout=30) as response:
        tailwind=response.read()
    for context in contexts.values():
        context.route('https://cdn.jsdelivr.net/npm/@tailwindcss/browser@4', lambda route: route.fulfill(status=200,content_type='text/javascript',body=tailwind))
    pages={r:c.new_page() for r,c in contexts.items()}
    for role,page in pages.items():
        page.goto(BASE+'/register')
        page.get_by_label('Nombre',exact=True).fill(role.title())
        page.get_by_label('Correo',exact=True).fill(emails[role])
        page.get_by_label('Contraseña',exact=True).fill(password)
        page.get_by_label('Confirmar contraseña',exact=True).fill(password)
        page.get_by_role('button',name='Crear cuenta',exact=True).click()
        page.wait_for_url('**/login?registered')
    subprocess.run(['docker','exec','petmatch-mysql','mysql','-uroot','petmatch_browser_test','-e',f"UPDATE users SET role='ADMIN' WHERE email='{emails['admin']}' AND active=1"],check=True,capture_output=True)
    for role,page in pages.items():
        page.goto(BASE+'/login');page.get_by_label('Correo',exact=True).fill(emails[role]);page.get_by_label('Contraseña',exact=True).fill(password);page.get_by_role('button',name='Ingresar',exact=True).click();page.wait_for_url(BASE+'/')
    owner=pages['owner'];helper=pages['helper'];admin=pages['admin']
    owner.goto(BASE+'/pets/new')
    owner.get_by_label('Nombre',exact=True).fill('Luna');owner.get_by_label('Especie',exact=True).fill('Perro');owner.get_by_label('Edad',exact=True).fill('2');owner.get_by_role('button',name='Registrar mascota',exact=True).click();owner.wait_for_url('**/pets/*');petid=owner.url.rsplit('/',1)[1]
    owner.goto(BASE+'/support-requests/new');title='Paseo de prueba '+suffix
    owner.get_by_label('Título',exact=True).fill(title);owner.get_by_label('Descripción',exact=True).fill('Recorrido automatizado con datos ficticios.')
    owner.get_by_label('Tipo de apoyo',exact=True).select_option('WALK');owner.get_by_label('Mascota',exact=True).select_option(petid)
    service_date=datetime.datetime.now()+datetime.timedelta(days=2)
    owner.get_by_label('Fecha y hora del apoyo',exact=True).fill(service_date.strftime('%Y-%m-%dT%H:%M'))
    owner.get_by_role('button',name='Publicar solicitud',exact=True).click();owner.wait_for_url('**/support-requests/*');requestid=owner.url.rsplit('/',1)[1]
    expect(owner.get_by_role('heading',name=title,exact=True)).to_be_visible();results.append({'flow':'registro-mascota-solicitud','status':'PASS'})
    helper.goto(BASE+f'/support-applications/request/{requestid}/new');helper.locator('#message').fill('Puedo ayudar.');helper.locator('form button[type=submit]').last.click()
    owner.goto(BASE+f'/support-applications/request/{requestid}');owner.get_by_role('button',name='Aceptar',exact=True).click()
    owner.goto(BASE+f'/support-requests/{requestid}');expect(owner.get_by_text('IN_PROGRESS',exact=True).first).to_be_visible();results.append({'flow':'postulacion-aceptacion','status':'PASS'})
    def member_row():
        admin.goto(BASE+'/admin/users')
        for _ in range(100):
            row=admin.locator('tbody tr').filter(has_text=emails['helper'])
            if row.count(): return row
            next_page=admin.get_by_role('link',name='Siguiente',exact=True)
            if not next_page.count(): raise AssertionError('Synthetic helper missing from user list')
            next_page.click()
        raise AssertionError('Too many user pages for smoke fixture')
    row=member_row();row.locator('select').select_option('ADMIN');row.get_by_role('button').click();expect(admin.get_by_role('status')).to_contain_text('Rol actualizado')
    row=member_row();row.locator('select').select_option('USER');row.get_by_role('button').click()
    admin.goto(BASE+'/admin/reports');admin.get_by_label('Estado',exact=True).select_option('IN_PROGRESS');admin.get_by_label('Tipo de apoyo',exact=True).select_option('WALK');admin.get_by_label('Desde',exact=True).fill(service_date.date().isoformat());admin.get_by_label('Hasta',exact=True).fill(service_date.date().isoformat());admin.get_by_role('button',name='Aplicar filtros',exact=True).click();expect(admin.get_by_role('cell',name=title,exact=True)).to_be_visible()
    admin.get_by_label('Agrupar resultados por',exact=True).select_option('SUPPORT_TYPE');admin.get_by_role('button',name='Aplicar filtros',exact=True).click();expect(admin.locator('ul[aria-label="Resumen agrupado"]')).to_contain_text('WALK')
    # Theme persistence and real downloads are verified in the browser context.
    theme_toggle = admin.get_by_role('button', name='Cambiar a modo claro')
    expect(theme_toggle).to_be_visible()
    theme_toggle.click()
    expect(admin.locator('html')).to_have_attribute('data-theme', 'light')
    admin.reload()
    expect(admin.locator('html')).to_have_attribute('data-theme', 'light')
    admin.get_by_role('button', name='Cambiar a modo oscuro').click()
    expect(admin.locator('html')).to_have_attribute('data-theme', 'dark')
    with admin.expect_download() as pdf_download:
        admin.get_by_role('link', name='Descargar PDF').click()
    assert pdf_download.value.suggested_filename.endswith('.pdf')
    with admin.expect_download() as excel_download:
        admin.get_by_role('link', name='Descargar Excel').click()
    assert excel_download.value.suggested_filename.endswith('.xlsx')
    assert admin.locator('.pm-nav__link[aria-current="page"]').count() == 1
    results.append({'flow': 'theme-persistence-and-file-downloads', 'status': 'PASS'})
    # Keyboard skip target and narrow viewport are checked mechanically, not by a human.
    admin.goto(admin.url);admin.keyboard.press('Tab');expect(admin.locator('.skip-link')).to_be_focused();admin.keyboard.press('Enter');assert admin.evaluate('document.activeElement.id')=='main-content'
    assert admin.locator('body').evaluate('(e)=>getComputedStyle(e).backgroundColor') != 'rgba(0, 0, 0, 0)'
    admin.set_viewport_size({'width':390,'height':844});admin.screenshot(path=str(OUT/'reportes-movil.png'),full_page=True)
    results.append({'flow':'admin-roles-reportes-strategy','status':'PASS'})
    browser.close()
(OUT/'browser-smoke.json').write_text(json.dumps({'date':datetime.datetime.now(datetime.timezone.utc).isoformat(),'kind':'automated browser, not human validation','database':'petmatch_browser_test','results':results},indent=2)+'\n')
print(json.dumps(results))
