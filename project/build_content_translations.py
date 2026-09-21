"""Build bundled translations from editable, reviewed text; no network or API key."""
import json
from pathlib import Path
root=Path(__file__).resolve().parents[1]
assets=root/'app/src/main/assets'
read=lambda path:json.loads(path.read_text(encoding='utf-8'))
recipes=read(assets/'recipes.json');products=read(assets/'products.json');lessons=read(assets/'lessons.json')
titles={line.split('|')[0]:line.split('|')[1:] for line in (root/'project/content_titles.txt').read_text(encoding='utf-8').splitlines() if line}
steps=list(dict.fromkeys(s for r in recipes for s in r['steps']))
step_text={row['source']:row for row in read(root/'project/content_steps.json')}
assert set(step_text)==set(steps), 'Update translations when cooking steps change'
lesson_text=read(root/'project/content_lessons.json')
assert len(titles)==220 and len(step_text)==len(steps)==47
result={'en':{},'pl':{}}
for language,index in [('en',0),('pl',1)]:
    for item in recipes+products:result[language][item['title']]=titles[item['id']][index]
    for step in steps:result[language][step]=step_text[step][language]
    for lesson in lessons:
        for key in ('title','author','body'):
            if key in lesson:result[language][lesson[key]]=lesson_text[lesson['id']][language][key]
    # Legacy recommendation names are snapshots in the diary too.
    import xml.etree.ElementTree as E
    base=E.parse(root/'app/src/main/res/values/strings.xml').getroot().find("string-array[@name='foods']")
    translated=E.parse(root/f'app/src/main/res/values-{language}/strings.xml').getroot().find("string-array[@name='foods']")
    for ru,target in zip(base,translated):result[language].setdefault(ru.text,target.text)
    path=assets/'translations'/f'{language}.json';path.parent.mkdir(exist_ok=True)
    path.write_text(json.dumps(result[language],ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(language,len(result[language]),'translated content strings')
