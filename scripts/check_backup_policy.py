#!/usr/bin/env python3
"""Offline GoreeShield policy check: Android browser data never enters backups.

This checks source declarations; packaged APK resource checks run separately.
No network access, profile inspection, WebView data or user content is read.
"""
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1] / "app" / "src" / "main"
ANDROID = "{http://schemas.android.com/apk/res/android}"
REQUIRED = {
    ("root", "."),
    ("device_root", "."),
    ("file", "."),
    ("database", "."),
    ("sharedpref", "."),
    ("external", "."),
}


def require(value: bool, reason: str) -> None:
    if not value:
        raise SystemExit(f"Browser backup policy failed: {reason}")


manifest = ET.parse(ROOT / "AndroidManifest.xml").getroot()
perms = [node.get(ANDROID + "name") for node in manifest.findall("uses-permission")]
require(perms == ["android.permission.INTERNET"], "unexpected Android permission set")
app = manifest.find("application")
require(app is not None, "missing application element")
require(app.get(ANDROID + "allowBackup") == "false", "allowBackup must be false")
require(
    app.get(ANDROID + "fullBackupContent") == "@xml/backup_rules",
    "explicit Android 8-11 legacy backup rules required",
)
require(
    app.get(ANDROID + "dataExtractionRules") == "@xml/data_extraction_rules",
    "Android 12+ transfer and cloud rules required",
)
require(
    app.get(ANDROID + "usesCleartextTraffic") == "false",
    "cleartext traffic must remain disabled",
)


def excluded(node: ET.Element, label: str) -> None:
    require(
        all(child.tag == "exclude" for child in node),
        f"{label}: only explicit exclusion elements are permitted",
    )
    actual = {(child.get("domain"), child.get("path")) for child in node}
    require(REQUIRED <= actual, f"{label}: required browser data domains are not excluded")
    require(
        len(actual) == len(node),
        f"{label}: duplicate or malformed exclusions",
    )


legacy = ET.parse(ROOT / "res/xml/backup_rules.xml").getroot()
require(legacy.tag == "full-backup-content", "invalid legacy backup rules root")
excluded(legacy, "legacy backup")
modern = ET.parse(ROOT / "res/xml/data_extraction_rules.xml").getroot()
require(modern.tag == "data-extraction-rules", "invalid extraction rules root")
require(
    [section.tag for section in modern] == ["cloud-backup", "device-transfer"],
    "both Android 12+ cloud-backup and device-transfer rules required",
)
for section in modern:
    excluded(section, section.tag)
print("PASS: GoreeShield browser legacy/cloud/device-transfer exclusions and manifest")
