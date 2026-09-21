"""Format XML resources without changing Android resource text content."""
from pathlib import Path
from xml.dom import minidom
from xml.sax.saxutils import quoteattr

def render(node, depth=0):
    pad='    '*depth
    if node.nodeType==node.COMMENT_NODE:
        return pad+'<!--'+node.data+'-->'
    attrs=list(node.attributes.items())
    opening=pad+'<'+node.tagName
    if len(attrs)>2:
        opening+='\n'+'\n'.join(pad+'    '+key+'='+quoteattr(value) for key,value in attrs)
    else:
        opening+=''.join(' '+key+'='+quoteattr(value) for key,value in attrs)
    elements=[c for c in node.childNodes if c.nodeType in (c.ELEMENT_NODE,c.COMMENT_NODE)]
    if not elements:
        text=''.join(c.toxml() for c in node.childNodes)
        return opening+('>'+text+'</'+node.tagName+'>' if text else ' />')
    return opening+'>\n'+'\n'.join(render(c,depth+1) for c in elements)+'\n'+pad+'</'+node.tagName+'>'

root=Path(__file__).resolve().parents[1]/'app/src/main/res'
for path in root.rglob('*.xml'):
    doc=minidom.parse(str(path))
    path.write_text('<?xml version="1.0" encoding="utf-8"?>\n'+render(doc.documentElement)+'\n',encoding='utf-8')
