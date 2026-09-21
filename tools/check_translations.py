"""Check complete UI translations, positional format tokens and array ordering lengths."""
from pathlib import Path
from xml.etree import ElementTree as E
import re
root=Path(__file__).resolve().parents[1]/'app/src/main/res'
def resources(folder):
    result={}
    for path in (root/folder).glob('*.xml'):
        for node in E.parse(path).getroot():
            if node.tag in ('string','string-array') and node.get('translatable')!='false':
                result[node.get('name')]=node
    return result
base=resources('values')
for folder in ('values-en','values-pl'):
    translated=resources(folder)
    assert base.keys()==translated.keys(),(folder,base.keys()-translated.keys(),translated.keys()-base.keys())
    for key,node in base.items():
        other=translated[key]
        if node.tag=='string-array':assert len(node)==len(other),(folder,key)
        else:
            tokens=lambda x:sorted(re.findall(r'%\d+\$[+\d.]*[sdf]',x or ''))
            assert tokens(node.text)==tokens(other.text),(folder,key)
    print(folder,len(translated),'resources OK')
