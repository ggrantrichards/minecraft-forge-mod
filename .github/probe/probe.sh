#!/usr/bin/env bash
set +e
exec > >(tee /tmp/probe.txt) 2>&1
mkdir -p /tmp/music && cd /tmp/music
for page in space-music-out-there outer-space-loop heavenly-loop ambient-relaxing-loop cc0-space-music space-music-2 loopable-dungeon-ambience; do
  echo "===== $page"
  curl -sL "https://opengameart.org/content/$page" -o "$page.html"
  python3 - "$page.html" <<'PY'
import re, sys, html
t = open(sys.argv[1], encoding="utf-8", errors="replace").read()
title = re.search(r"<title>(.*?)</title>", t, re.S)
print("TITLE:", html.unescape(title.group(1)).strip() if title else "?")
a = re.search(r'field-name-author-submitter.*?<a[^>]*>(.*?)</a>', t, re.S)
print("SUBMITTER:", html.unescape(a.group(1)).strip() if a else "?")
for m in re.findall(r'field-name-field-art-licenses.*?</div>\s*</div>\s*</div>', t, re.S)[:1]:
    print("LICENSES:", sorted(set(re.findall(r'title="([^"]+)"', m))))
body = re.search(r'field-name-body.*?<div class="field-item[^"]*">(.*?)</div>', t, re.S)
if body:
    txt = re.sub(r"<[^>]+>", " ", body.group(1))
    print("BODY:", " ".join(html.unescape(txt).split())[:700])
for f in sorted(set(re.findall(r'https://opengameart\.org/sites/default/files/[^"\']+\.(?:ogg|mp3|wav|flac)', t))):
    print("FILE:", f)
PY
done
mkdir -p files
for f in $(grep -h -o 'https://opengameart\.org/sites/default/files/[^"'"'"']*\.ogg' *.html | sort -u | head -12); do
  curl -sL "$f" -o "files/$(basename "$f")"
done
ls -la files
cd "$GITHUB_WORKSPACE"
exit 0
