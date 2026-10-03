"""Publishes the mod to Modrinth (run by .github/workflows/release.yml).

    MODRINTH_TOKEN=... python3 tools/release/modrinth.py <jar> [--type beta|alpha|release]

The first run creates the project (as a draft, with its icon, gallery and description) and submits it
for Modrinth's review; every run uploads the jar as a new version, unless that version number is
already there. Standard library only.
"""
import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.parse
import urllib.request
import uuid
from pathlib import Path

API = os.environ.get("MODRINTH_API", "https://api.modrinth.com/v2")   # overridable for testing
ROOT = Path(__file__).resolve().parents[2]
SLUG = "the-astral-end"
TITLE = "The Astral End"
SUMMARY = ("The End, expanded: seven new biomes, new creatures, a boss, a living sky with auroras and "
           "sunstorms, live comets, and ruins to explore.")
GAME_VERSIONS = ["26.3"]
LOADERS = ["forge"]
USER_AGENT = "ggrantrichards/minecraft-forge-mod (release workflow)"


def request(method, path, token, body=None, content_type=None, ok=(200, 201, 204)):
    headers = {"User-Agent": USER_AGENT}
    if token:
        headers["Authorization"] = token
    if content_type:
        headers["Content-Type"] = content_type
    req = urllib.request.Request(API + path, data=body, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            raw = resp.read()
            return resp.status, (json.loads(raw) if raw else None)
    except urllib.error.HTTPError as e:
        raw = e.read().decode(errors="replace")
        if e.code in ok:
            return e.code, None
        return e.code, raw


def multipart(fields, files):
    """fields: {name: json-able}; files: {name: (filename, bytes, mime)}."""
    boundary = uuid.uuid4().hex
    out = bytearray()
    for name, value in fields.items():
        out += f"--{boundary}\r\nContent-Disposition: form-data; name=\"{name}\"\r\n\r\n".encode()
        out += (json.dumps(value) if not isinstance(value, str) else value).encode() + b"\r\n"
    for name, (filename, data, mime) in files.items():
        out += (f"--{boundary}\r\nContent-Disposition: form-data; name=\"{name}\"; filename=\"{filename}\"\r\n"
                f"Content-Type: {mime}\r\n\r\n").encode()
        out += data + b"\r\n"
    out += f"--{boundary}--\r\n".encode()
    return bytes(out), f"multipart/form-data; boundary={boundary}"


def die(message):
    print(f"::error::{message}")
    sys.exit(1)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("jar")
    parser.add_argument("--type", default="beta", choices=["alpha", "beta", "release"])
    args = parser.parse_args()
    # Forgive the usual paste mistakes: quotes, a "Bearer " prefix, stray whitespace or line breaks.
    token = "".join(os.environ.get("MODRINTH_TOKEN", "").split()).strip("\"'")
    if token.lower().startswith("bearer"):
        token = token[6:]
    if not token:
        die("No MODRINTH_TOKEN secret. Create a personal access token on modrinth.com (Settings > "
            "Personal access tokens) and add it to the repository as an Actions secret named MODRINTH_TOKEN.")
    jar = Path(args.jar)
    version_number = re.search(r"-(\d+\.\d+\.\d+[^/]*?)\.jar$", jar.name)
    if not version_number:
        die(f"Can't read a version number from {jar.name}")
    version_number = version_number.group(1)

    # Modrinth has to know the game version and loader.
    status, tags = request("GET", "/tag/game_version", token)
    known = {t["version"] for t in tags} if status == 200 else set()
    missing = [v for v in GAME_VERSIONS if v not in known]
    if missing:
        die(f"Modrinth doesn't list Minecraft {', '.join(missing)} yet, so a version can't target it.")

    if not token.startswith("mrp_"):
        print("::warning::The token doesn't start with 'mrp_', so it may not be a Modrinth personal access token.")
    # Who the token belongs to: only for the log. Modrinth answers "Invalid Authentication Credentials"
    # both for a bad token and for one missing a scope, so a refusal here isn't fatal; the requests that
    # need other scopes report their own errors.
    status, me = request("GET", "/user", token)
    if status == 200:
        print(f"Publishing as {me['username']}")
    else:
        print(f"::warning::Couldn't read the token's user ({status}); it may lack the Read user data scope. Carrying on.")

    body_text = (ROOT / "docs/modrinth/description.md").read_text()
    gallery = json.loads((ROOT / "docs/modrinth/gallery.json").read_text())

    status, project = request("GET", f"/project/{SLUG}", token)
    if status == 401:
        status, project = request("GET", f"/project/{SLUG}", None)   # the token may lack Read projects
    created = False
    if status == 404:
        data = {
            "slug": SLUG, "title": TITLE, "description": SUMMARY, "body": "Coming soon.",
            "categories": ["worldgen", "adventure", "mobs"], "additional_categories": ["decoration", "transportation"],
            "client_side": "required", "server_side": "required", "license_id": "MIT",
            "project_type": "mod", "initial_versions": [], "is_draft": True,
            "issues_url": None, "source_url": None, "wiki_url": None, "discord_url": None, "donation_urls": [],
        }
        icon = (ROOT / "docs/modrinth/icon.png").read_bytes()
        payload, ctype = multipart({"data": data}, {"icon": ("icon.png", icon, "image/png")})
        status, project = request("POST", "/project", token, payload, ctype)
        if status not in (200, 201):
            die(f"Couldn't create the project ({status}): {project}. If this is an authentication error, the "
                f"token needs the Create projects scope (and Write projects, Create versions).")
        created = True
        print(f"Created the project {project['slug']} ({project['id']})")
    elif status != 200:
        die(f"Couldn't look up the project ({status}): {project}")
    else:
        print(f"Project {project['slug']} ({project['id']}) already exists")
    pid = project["id"]

    # The gallery, then the description with its pictures pointing at the gallery. Pictures are known
    # by title: one whose description changed is uploaded again, and one no longer listed is removed.
    have = {g.get("title"): g for g in (project.get("gallery") or [])}
    wanted = {entry["title"]: entry for entry in gallery}
    for title, old in list(have.items()):
        if title not in wanted or (old.get("description") or "") != wanted[title]["description"]:
            status, err = request("DELETE", f"/project/{pid}/gallery?{urllib.parse.urlencode({'url': old['url']})}", token)
            if status not in (200, 204):
                die(f"Couldn't remove the old picture {title} ({status}): {err}")
            print(f"Gallery: removed {title}")
            del have[title]
    for order, entry in enumerate(gallery):
        if entry["title"] in have:
            continue
        ext = Path(entry["file"]).suffix.lstrip(".").lower()
        query = urllib.parse.urlencode({"ext": ext, "featured": str(entry.get("featured", False)).lower(),
                                        "title": entry["title"], "description": entry["description"], "ordering": order})
        status, err = request("POST", f"/project/{pid}/gallery?{query}", token,
                              (ROOT / entry["file"]).read_bytes(), f"image/{'jpeg' if ext == 'jpg' else ext}")
        if status not in (200, 201, 204):
            die(f"Couldn't upload the picture {entry['title']} ({status}): {err}")
        print(f"Gallery: {entry['title']}")
    status, project = request("GET", f"/project/{pid}", token)
    urls = {g["title"]: g["url"] for g in project.get("gallery") or []}

    def picture(m):
        title = m.group(1)
        return f"![{title}]({urls[title]})" if title in urls else ""
    body = re.sub(r"\{\{(.+?)\}\}", picture, body_text)
    status, err = request("PATCH", f"/project/{pid}", token, json.dumps({"body": body, "description": SUMMARY}).encode(),
                          "application/json")
    if status not in (200, 204):
        die(f"Couldn't update the description ({status}): {err}")

    # The version.
    status, versions = request("GET", f"/project/{pid}/version", token)
    if status == 200 and any(v["version_number"] == version_number for v in versions):
        print(f"Version {version_number} is already on Modrinth; nothing to upload.")
    else:
        changelog_file = ROOT / "docs/modrinth/changelog" / f"{version_number}.md"
        changelog = changelog_file.read_text() if changelog_file.exists() else f"The Astral End {version_number}."
        data = {
            "name": f"The Astral End {version_number}", "version_number": version_number, "changelog": changelog,
            "dependencies": [], "game_versions": GAME_VERSIONS, "version_type": args.type, "loaders": LOADERS,
            "featured": True, "project_id": pid, "file_parts": ["jar"], "primary_file": "jar",
        }
        payload, ctype = multipart({"data": data}, {"jar": (jar.name, jar.read_bytes(), "application/java-archive")})
        status, version = request("POST", "/version", token, payload, ctype)
        if status not in (200, 201):
            die(f"Couldn't upload the version ({status}): {version}")
        print(f"Uploaded {version['name']} ({version['id']})")

    # A new project waits in draft until it's sent to Modrinth's moderators.
    if project.get("status") == "draft":
        status, err = request("PATCH", f"/project/{pid}", token, json.dumps({"status": "processing"}).encode(),
                              "application/json")
        if status in (200, 204):
            print("Submitted the project for Modrinth's review. It goes public once a moderator approves it.")
        else:
            print(f"::warning::Couldn't submit for review automatically ({status}): {err}. "
                  f"Open https://modrinth.com/mod/{SLUG} and press 'Submit for review'.")
    print(f"https://modrinth.com/mod/{SLUG}")


if __name__ == "__main__":
    main()
