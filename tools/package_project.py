"""Package the built APK and portable sources, excluding local settings and caches."""
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile
import hashlib
import shutil

root = Path(__file__).resolve().parents[1]
output = root / "delivery"
output.mkdir(exist_ok=True)
apk = root / "app/build/outputs/apk/debug/app-debug.apk"
if not apk.is_file():
    raise SystemExit("Build assembleDebug first")
shutil.copy2(apk, output / "NutriFit-2.2-debug.apk")
files = [root / name for name in (
    "README.md", ".gitignore", "build.gradle", "settings.gradle",
    "gradle.properties", "gradlew", "gradlew.bat", "app/build.gradle"
)]
for directory in ("app/src", "gradle/wrapper", "docs", "tools"):
    files.extend(p for p in (root / directory).rglob("*")
                 if p.is_file() and "__pycache__" not in p.parts
                 and p.suffix not in (".pyc", ".tmp"))
archive = output / "NutriFit-AndroidStudio.zip"
with ZipFile(archive, "w", ZIP_DEFLATED) as zipped:
    for path in sorted(set(files)):
        zipped.write(path, "NutriFit/" + path.relative_to(root).as_posix())
with ZipFile(archive) as zipped:
    assert zipped.testzip() is None
    assert "NutriFit/local.properties" not in zipped.namelist()
    assert not any("/build/" in name or "/.gradle/" in name for name in zipped.namelist())
checksums = []
for path in (output / "NutriFit-2.2-debug.apk", archive):
    checksums.append(hashlib.sha256(path.read_bytes()).hexdigest() + "  " + path.name)
    print(path.name, path.stat().st_size, "bytes")
(output / "SHA256SUMS.txt").write_text("\n".join(checksums) + "\n", encoding="utf-8")
