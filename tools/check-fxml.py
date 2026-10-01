#!/usr/bin/env python3
"""Checks every FXML: fx:id → controller field, onAction → method, %key → all three bundles."""
import os
import re
import sys
import xml.etree.ElementTree as ET

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FXML_DIR = os.path.join(ROOT, "src/main/resources/fxml")
JAVA_ROOT = os.path.join(ROOT, "src/main/java")
BUNDLES = [os.path.join(ROOT, "src/main/resources", f)
           for f in ("messages.properties", "messages_el.properties", "messages_en.properties")]
FX = "{http://javafx.com/fxml/1}"

# Pre-existing before sub-project 2β (unbound fx:ids are valid JavaFX; the key falls back to messages.properties).
# Problems introduced later are never added here.
KNOWN = {
    "main-navigation.fxml: fx:id 'logoPlaceholder' has no field in net.gizmolab.library.librarymanagementsystemdesktop.controller.MainNavigationController",
    "main-navigation.fxml: fx:id 'logoView' has no field in net.gizmolab.library.librarymanagementsystemdesktop.controller.MainNavigationController",
    "main-navigation.fxml: fx:id 'languageSelectorContainer' has no field in net.gizmolab.library.librarymanagementsystemdesktop.controller.MainNavigationController",
    "main-navigation.fxml: fx:id 'logoutButton' has no field in net.gizmolab.library.librarymanagementsystemdesktop.controller.MainNavigationController",
}

sources = {}
for dirpath, _, files in os.walk(JAVA_ROOT):
    for f in files:
        if f.endswith(".java"):
            sources[f[:-5]] = open(os.path.join(dirpath, f), encoding="utf-8").read()


def class_chain(simple_name):
    chain, name = [], simple_name
    while name in sources:
        chain.append(sources[name])
        m = re.search(r"class\s+%s\b[^{]*?\bextends\s+(\w+)" % name, sources[name])
        name = m.group(1) if m else None
    return "\n".join(chain)


keys = [set(re.findall(r"^([\w.]+)\s*=", open(b, encoding="utf-8").read(), re.M)) for b in BUNDLES]
problems = []
for fx in sorted(os.listdir(FXML_DIR)):
    if not fx.endswith(".fxml"):
        continue
    path = os.path.join(FXML_DIR, fx)
    text = open(path, encoding="utf-8").read()
    root = ET.parse(path).getroot()
    controller = root.get(FX + "controller")
    code = class_chain(controller.rsplit(".", 1)[1]) if controller else ""
    for el in root.iter():
        fid = el.get(FX + "id")
        if controller and fid and not re.search(r"\b%s(Controller)?\s*;" % re.escape(fid), code):
            problems.append(f"{fx}: fx:id '{fid}' has no field in {controller}")
        handler = el.get("onAction")
        if controller and handler and handler.startswith("#") and not re.search(r"void\s+%s\s*\(" % handler[1:], code):
            problems.append(f"{fx}: onAction '{handler}' has no method in {controller}")
    for key in set(re.findall(r'"%([\w.]+)"', text)):
        for bundle, present in zip(BUNDLES, keys):
            if key not in present:
                problems.append(f"{fx}: key '{key}' missing in {os.path.basename(bundle)}")

problems = [p for p in problems if p not in KNOWN]
print("\n".join(problems) if problems else "FXML OK")
sys.exit(1 if problems else 0)
