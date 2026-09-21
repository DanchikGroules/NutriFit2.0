"""Small adb helper for the isolated .qa build; never clears application data."""
import os,re,subprocess,sys,time,xml.etree.ElementTree as ET
from pathlib import Path
sys.stdout.reconfigure(encoding='utf-8')
adb=str(Path(os.environ.get('ANDROID_HOME',Path.home()/'AppData/Local/Android/Sdk'))/'platform-tools/adb.exe')
def call(*args):return subprocess.check_output([adb,"-s","emulator-5554",*args]).decode('utf-8',errors='replace')
def tree():
    for _ in range(4):
        result=call('shell','uiautomator','dump','/sdcard/nutrifit-window.xml')
        if 'dumped to' in result:
            return ET.fromstring(call('shell','cat','/sdcard/nutrifit-window.xml'))
        time.sleep(.5)
    raise RuntimeError('Could not obtain a fresh UI hierarchy')
def node(key):
    for n in tree().iter('node'):
        if n.get('resource-id','').endswith(':id/'+key) or n.get('text')==key:return n
    raise RuntimeError('Visible element not found: '+key)
def tap(key):
    n=node(key);x1,y1,x2,y2=map(int,re.findall(r'\d+',n.get('bounds')))
    call('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
if __name__=='__main__':
    action=sys.argv[1]
    if action=='dump':
        for n in tree().iter('node'):
            if n.get('text') or n.get('clickable')=='true':print(n.get('resource-id'),'[password]' if n.get('password')=='true' else n.get('text'),n.get('bounds'))
    elif action=='tap':tap(sys.argv[2])
    elif action=='type':
        tap(sys.argv[2]);call('shell','input','text',sys.argv[3])
    elif action=='replace':
        tap(sys.argv[2]);call('shell','input','keyevent','KEYCODE_MOVE_END')
        call('shell','input','keyevent',*(['KEYCODE_DEL']*50));call('shell','input','text',sys.argv[3])
    elif action=='scroll':call('shell','input','swipe','650','2200','650','700','350')
    elif action=='shot':
        Path(sys.argv[2]).write_bytes(subprocess.check_output([adb,'-s','emulator-5554','exec-out','screencap','-p']))

