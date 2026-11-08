#!/usr/bin/env python3
"""Execute une collection Postman v2.1 sans newman.

Gere : variables de collection/environnement, en-tetes, corps JSON,
extraction de jetons via les scripts de test (pm.collectionVariables.set),
et verification du code HTTP attendu (to.have.status(...)).
"""
import json
import re
import sys
import urllib.error
import urllib.request

COLLECTION = sys.argv[1]
ENVIRONMENT = sys.argv[2]

coll = json.load(open(COLLECTION))
env = json.load(open(ENVIRONMENT))

variables = {v["key"]: v["value"] for v in coll.get("variable", [])}
variables.update({v["key"]: v["value"] for v in env.get("values", []) if v.get("enabled", True)})


def substituer(texte):
    if texte is None:
        return ""
    return re.sub(r"\{\{(\w+)\}\}", lambda m: str(variables.get(m.group(1), m.group(0))), texte)


def executer(item, dossier, resultats):
    requete = item["request"]
    url = substituer(requete["url"]["raw"])
    méthode = requete["method"]
    entetes = {substituer(h["key"]): substituer(h["value"]) for h in requete.get("header", [])}
    corps = None
    if "body" in requete and requete["body"].get("raw"):
        corps = substituer(requete["body"]["raw"]).encode()
        entetes.setdefault("Content-Type", "application/json")

    req = urllib.request.Request(url, data=corps, headers=entetes, method=méthode)
    try:
        with urllib.request.urlopen(req, timeout=15) as reponse:
            code, contenu, entetes_reponse = reponse.status, reponse.read(), dict(reponse.headers)
    except urllib.error.HTTPError as erreur:
        code, contenu, entetes_reponse = erreur.code, erreur.read(), dict(erreur.headers)

    attendus = []
    for evenement in item.get("event", []):
        if evenement.get("listen") != "test":
            continue
        script = "\n".join(evenement["script"]["exec"])
        attendus += [int(n) for n in re.findall(r"to\.have\.status\((\d{3})\)", script)]
        for cle in re.findall(r"collectionVariables\.set\('(\w+)'", script):
            try:
                variables[cle] = json.loads(contenu)["token"]
            except Exception:
                pass

    ok = all(code == attendu for attendu in attendus) if attendus else True
    resultats.append((ok, dossier, item["name"], méthode, code, attendus))
    return contenu, entetes_reponse


resultats = []


def parcourir(items, dossier=""):
    for item in items:
        if "item" in item:
            parcourir(item["item"], item["name"])
        else:
            executer(item, dossier, resultats)


parcourir(coll["item"])

echecs = 0
dossier_courant = None
for ok, dossier, nom, méthode, code, attendus in resultats:
    if dossier != dossier_courant:
        print(f"\n[{dossier}]")
        dossier_courant = dossier
    marque = "OK  " if ok else "ECHEC"
    attendu = "/".join(str(a) for a in attendus) or "-"
    print(f"  {marque} {méthode:6} {nom:38} http={code} attendu={attendu}")
    if not ok:
        echecs += 1

print(f"\n{len(resultats)} requetes, {echecs} echec(s)")
sys.exit(1 if echecs else 0)
