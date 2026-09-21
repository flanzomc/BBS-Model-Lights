"""Launch the remapped JAR through the normal Fabric client (no Loom dev runtime)."""
import concurrent.futures
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import urllib.request
import zipfile

root = Path('production-run').resolve()
root.mkdir(exist_ok=True)

def download(url, path, digest=None):
    path = Path(path)
    if path.exists() and (digest is None or hashlib.sha1(path.read_bytes()).hexdigest() == digest):
        return path
    path.parent.mkdir(parents=True, exist_ok=True)
    for attempt in range(3):
        try:
            with urllib.request.urlopen(url, timeout=90) as response:
                data = response.read()
            if digest and hashlib.sha1(data).hexdigest() != digest:
                raise RuntimeError('Checksum mismatch: ' + url)
            path.write_bytes(data)
            return path
        except Exception:
            if attempt == 2:
                raise

def get_json(url):
    with urllib.request.urlopen(url, timeout=60) as response:
        return json.load(response)

manifest = get_json('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json')
version = get_json(next(v['url'] for v in manifest['versions'] if v['id'] == '1.20.4'))
profile = get_json('https://meta.fabricmc.net/v2/versions/loader/1.20.4/0.16.14/profile/json')
client = version['downloads']['client']
classpath = [str(download(client['url'], root / 'client.jar', client['sha1']))]
natives = root / 'natives'
natives.mkdir(exist_ok=True)

def allowed(entry):
    rules = entry.get('rules')
    if not rules:
        return True
    result = False
    for rule in rules:
        if rule.get('os', {}).get('name', 'linux') != 'linux':
            continue
        if rule.get('features'):
            continue
        result = rule['action'] == 'allow'
    return result

for lib in version['libraries'] + profile['libraries']:
    if not allowed(lib):
        continue
    artifact = lib.get('downloads', {}).get('artifact')
    if artifact:
        classpath.append(str(download(artifact['url'], root / 'libraries' / artifact['path'], artifact.get('sha1'))))
    elif 'downloads' not in lib:
        group, name, release = lib['name'].split(':')[:3]
        path = f'{group.replace(".", "/")}/{name}/{release}/{name}-{release}.jar'
        classpath.append(str(download(lib.get('url', 'https://libraries.minecraft.net/') + path, root / 'libraries' / path)))
    classifier = lib.get('natives', {}).get('linux')
    if classifier:
        artifact = lib['downloads']['classifiers'][classifier.replace('${arch}', '64')]
        native = download(artifact['url'], root / 'libraries' / artifact['path'], artifact.get('sha1'))
        with zipfile.ZipFile(native) as archive:
            for name in archive.namelist():
                if name.endswith('.so'):
                    (natives / Path(name).name).write_bytes(archive.read(name))

index_info = version['assetIndex']
index_file = download(index_info['url'], root / 'assets/indexes' / (index_info['id'] + '.json'), index_info['sha1'])
objects = json.loads(index_file.read_text())['objects']
def asset(obj):
    digest = obj['hash']
    download('https://resources.download.minecraft.net/' + digest[:2] + '/' + digest,
             root / 'assets/objects' / digest[:2] / digest, digest)
with concurrent.futures.ThreadPoolExecutor(max_workers=12) as pool:
    list(pool.map(asset, objects.values()))

mods = root / 'mods'
mods.mkdir(exist_ok=True)
fs_version = sys.argv[1]
fs_id = {'2.5.2': 'qGbGyKHH', '2.6.1': 'Jg7pltPc'}[fs_version]
download(f'https://cdn.modrinth.com/data/ZeRO0IDA/versions/{fs_id}/bbs-{fs_version}-1.20.4.jar', mods / 'bbs.jar')
api_versions = get_json('https://api.modrinth.com/v2/project/fabric-api/version?game_versions=%5B%221.20.4%22%5D&loaders=%5B%22fabric%22%5D')
api = next(v for v in api_versions if v['version_number'] == '0.97.3+1.20.4')
api_file = next(f for f in api['files'] if f['primary'])
download(api_file['url'], mods / 'fabric-api.jar', api_file['hashes']['sha1'])
jar = Path('build/libs/bbs-model-lights-0.1.0-alpha.1.jar')
shutil.copy2(jar, mods / jar.name)
(root / 'options.txt').write_text('onboardAccessibility:false\nrenderDistance:4\nsimulationDistance:4\nmaxFps:30\nsoundCategory_master:0.0\n')
args = ['java', '-Xmx2G', '-Dbml.smoke=true', '-Dfabric.development=false',
        '-Djava.library.path=' + str(natives), '-cp', os.pathsep.join(classpath), profile['mainClass'],
        '--username', 'BMLTest', '--version', '1.20.4', '--gameDir', str(root),
        '--assetsDir', str(root / 'assets'), '--assetIndex', index_info['id'],
        '--uuid', '00000000000000000000000000000001', '--accessToken', '0',
        '--userType', 'legacy', '--versionType', 'release', '--width', '960', '--height', '540']
print('Launching production Fabric client with FS', fs_version, flush=True)
result = subprocess.run(args, cwd=root, timeout=300)
if result.returncode or not (root / 'bml-smoke-passed.txt').exists():
    raise SystemExit('Production smoke test did not pass')
print((root / 'bml-smoke-passed.txt').read_text())
