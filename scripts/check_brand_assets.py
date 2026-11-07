#!/usr/bin/env python3
"""Verify shipped Android Prism Tabs resources against approved central exports.

Uses only Python stdlib and the pinned checked-in brand provenance manifest.
No network, Gradle, image rendering or device access.
"""
import hashlib
import json
from pathlib import Path
import struct
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
BRANDING = ROOT / "branding"
DRAWABLE = ROOT / "app/src/main/res/drawable-nodpi"
MANIFEST = json.loads((BRANDING / "asset-index.json").read_text(encoding="utf-8"))
assert MANIFEST["product"] == "browser-mobile"
assert MANIFEST["source_git_blob_sha"] == "c82371aa6fadd45eb26610bf4907e64122d15b12"


def blob_id(data):
    return hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest()


for name, expected in (
    ("prism-tabs-v5.svg", "c82371aa6fadd45eb26610bf4907e64122d15b12"),
    ("prism-tabs-v5-symbol.svg", "d5f96e5dfac25588f4c4a95ac3277adc33d3cafa"),
    ("prism-tabs-v5-monochrome.svg", "56fd59404c83ac8c26416c3a824f8511cf2191d2"),
):
    path = BRANDING / name
    data = path.read_bytes()
    assert blob_id(data) == expected, f"Source fingerprint mismatch: {name}"
    assert ET.fromstring(data).attrib["viewBox"] == "0 0 512 512"

for filename, source in (
    ("ic_browser_symbol.png", "icon-512.png"),
    ("ic_launcher_foreground.png", "android-foreground-512.png"),
    ("ic_launcher_monochrome.png", "android-monochrome-512.png"),
):
    data = (DRAWABLE / filename).read_bytes()
    spec = MANIFEST["files"][source]
    assert data[:8] == bytes.fromhex("89504e470d0a1a0a"), filename
    assert struct.unpack(">II", data[16:24]) == (512, 512), filename
    assert hashlib.sha256(data).hexdigest() == spec["sha256"], filename
    assert len(data) == spec["bytes"], filename

adaptive = (ROOT / "app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml").read_text()
assert "@drawable/ic_launcher_foreground" in adaptive
assert "@drawable/ic_launcher_monochrome" in adaptive
assert "@color/icon_bg" in adaptive
print("PASS approved Prism Tabs v5: 3 source SVGs, 3 pinned PNGs, Android adaptive resource links")
