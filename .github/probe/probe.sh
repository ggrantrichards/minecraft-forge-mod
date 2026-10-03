#!/usr/bin/env bash
set +e
exec > >(tee /tmp/probe.txt) 2>&1
for page in space-music-out-there cc0-space-music heavenly-loop ambient-relaxing-loop space-music-2; do
  echo "===== $page"
  curl -sL "https://opengameart.org/content/$page" -o p.html
  grep -o -E 'https?://creativecommons\.org/[a-z/0-9.-]+|license-icon[^>]*>|GPL [0-9.]+|OGA-BY [0-9.]+|CC0|CC-BY[-A-Z]* [0-9.]+' p.html | sort | uniq -c
  python3 - <<'PY'
import re, html
t = open("p.html", encoding="utf-8", errors="replace").read()
for key in ("Attribution Instructions", "Attribution notes", "Copyright/Attribution Notice", "field-name-field-copyright-notice", "Author:", "Submitted by"):
    i = t.find(key)
    if i >= 0:
        print(key, "->", " ".join(html.unescape(re.sub(r"<[^>]+>", " ", t[i:i+600])).split())[:300])
i = t.find("License(s)")
if i >= 0:
    print("LICENSE BLOCK ->", " ".join(html.unescape(re.sub(r"<[^>]+>", " ", t[i:i+900])).split())[:300])
PY
done
exit 0
