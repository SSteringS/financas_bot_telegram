#!/usr/bin/env python3
"""Metricas dos status reports do projeto.

Le o frontmatter YAML de cada status report e agrega um punhado de metricas
que MUDAM COMPORTAMENTO -- nao vanity metrics. A escolha e deliberada:

  1. Cobertura do schema   -- quantos reports seguem o schema canonico (drift)
  2. Trabalho em aberto     -- estado: bloqueado/parcial = nao esta pronto
  3. Atrito                 -- desvios + pendencias de humano + gates que falharam
  4. Testes                 -- soma de testes_total/testes_novos (contexto)

Por que isto existe: o status report e um *output contract* (ADR 0007). Como o
frontmatter e YAML parseavel, da pra montar um painel do projeto sem esforco.
Este script e essa leitura agregada.

Por default varre `docs/status` (legado) + `docs/sprints/<NN>/status` (ADR 0010).
Pode-se sobrepor com `--dir <path>` (repetivel).

Uso:
    python3 docs/scripts/metricas_status.py [--dir <path> ...] [--json]

Sem dependencias externas obrigatorias: usa PyYAML se disponivel, senao cai
num parser minimo embutido (suficiente pro schema plano + bloco `gates:`).
Robusto a CRLF (Windows) e a reports antigos sem frontmatter.
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

try:
    import yaml  # type: ignore
    _HAS_YAML = True
except Exception:  # pragma: no cover - fallback quando PyYAML nao esta instalado
    _HAS_YAML = False


def _strip_frontmatter(text: str) -> str | None:
    """Devolve o bloco de frontmatter (sem as cercas ---) ou None se nao houver."""
    lines = text.replace("\r\n", "\n").replace("\r", "\n").split("\n")
    i = 0
    while i < len(lines) and lines[i].strip() == "":
        i += 1
    if i >= len(lines) or lines[i].strip() != "---":
        return None
    i += 1
    buf = []
    while i < len(lines) and lines[i].strip() != "---":
        buf.append(lines[i])
        i += 1
    if i >= len(lines):
        return None
    return "\n".join(buf)


def _coerce(v):
    if v == "":
        return None
    low = v.lower()
    if low in ("null", "none"):
        return None
    if low == "na":
        return "na"
    try:
        return int(v)
    except ValueError:
        return v


def _parse_minimo(fm: str) -> dict:
    """Parser minimo de YAML plano + um nivel de aninhamento (bloco `gates:`)."""
    out: dict = {}
    cur_nested_key = None
    for raw in fm.split("\n"):
        if not raw.strip() or raw.strip().startswith("#"):
            continue
        indent = len(raw) - len(raw.lstrip(" "))
        line = raw.strip()
        if line.startswith("- "):
            continue
        if ":" not in line:
            continue
        key, _, val = line.partition(":")
        key = key.strip()
        val = val.split("#", 1)[0].strip().strip('"').strip("'")
        if indent == 0:
            if val == "":
                cur_nested_key = key
                out[key] = {}
            else:
                out[key] = _coerce(val)
                cur_nested_key = None
        else:
            if cur_nested_key and isinstance(out.get(cur_nested_key), dict):
                out[cur_nested_key][key] = _coerce(val)
    return out


def parse_frontmatter(text: str) -> dict | None:
    fm = _strip_frontmatter(text)
    if fm is None:
        return None
    if _HAS_YAML:
        try:
            data = yaml.safe_load(fm)
            return data if isinstance(data, dict) else None
        except Exception:
            return _parse_minimo(fm)
    return _parse_minimo(fm)


def _normalizar_estado(v) -> str:
    """Normaliza valor de estado, tolerando aliases/acentos/caixa entre epocas."""
    if v is None:
        return "?"
    s = str(v).strip().lower()
    trocas = {"á": "a", "é": "e", "í": "i", "ó": "o",
              "ú": "u", "ã": "a", "õ": "o", "ê": "e"}
    for k, val in trocas.items():
        s = s.replace(k, val)
    mapa = {
        "concluido": "concluido", "concluida": "concluido", "done": "concluido",
        "parcial": "parcial", "partial": "parcial",
        "bloqueado": "bloqueado", "blocked": "bloqueado",
    }
    return mapa.get(s, s or "?")


def _as_int(v) -> int | None:
    if isinstance(v, bool):
        return None
    if isinstance(v, int):
        return v
    if isinstance(v, str):
        try:
            return int(v)
        except ValueError:
            return None
    return None


def coletar(status_dirs) -> dict:
    """Coleta metricas. Aceita Path unico (compat) ou lista de Paths."""
    if isinstance(status_dirs, Path):
        status_dirs = [status_dirs]
    # coleta + dedupe por filename: se mesmo nome aparece em multiplas pastas
    # (ex: docs/status/ legado vs docs/sprints/01-mvp/status/), prefere a versao
    # em sprint/ (sob docs/sprints/) pra refletir ADR 0010.
    por_nome = {}
    for d in status_dirs:
        for p in d.glob("*.md"):
            if p.name.startswith("_"):
                continue
            existente = por_nome.get(p.name)
            if existente is None:
                por_nome[p.name] = p
            else:
                em_sprint = lambda x: "sprints" in x.parts
                if em_sprint(p) and not em_sprint(existente):
                    por_nome[p.name] = p
    arquivos = sorted(por_nome.values(), key=lambda p: p.name)

    com_fm: list = []
    sem_fm: list = []
    for p in arquivos:
        data = parse_frontmatter(p.read_text(encoding="utf-8", errors="replace"))
        if data is None:
            sem_fm.append(p.name)
        else:
            com_fm.append((p.name, data))

    estados: dict = {}
    gate_fails: list = []
    desvios_total = 0
    pend_humano_total = 0
    testes_total = 0
    testes_novos = 0
    desvios_por_area: dict = {}
    abertos: list = []
    canonicos = 0
    nao_canonicos: list = []

    gate_keys = ("build", "lint", "testes", "branch_convencao", "territorio")

    for nome, d in com_fm:
        if "estado" in d:
            canonicos += 1
        else:
            nao_canonicos.append(nome)
        estado = _normalizar_estado(d.get("estado", d.get("status")))
        estados[estado] = estados.get(estado, 0) + 1
        if estado in ("parcial", "bloqueado"):
            abertos.append(nome + " [" + estado + "]")

        gates = d.get("gates") or {}
        if isinstance(gates, dict):
            if any(str(gates.get(k)).lower() == "fail" for k in gate_keys):
                gate_fails.append(nome)
            tt = _as_int(gates.get("testes_total"))
            tn = _as_int(gates.get("testes_novos"))
            if tt:
                testes_total += tt
            if tn:
                testes_novos += tn

        dv = _as_int(d.get("desvios")) or 0
        ph = _as_int(d.get("pendencias_humano")) or 0
        desvios_total += dv
        pend_humano_total += ph
        if dv:
            area = str(d.get("responsavel", "?"))
            desvios_por_area[area] = desvios_por_area.get(area, 0) + dv

    return {
        "total_reports": len(arquivos),
        "com_frontmatter": len(com_fm),
        "sem_frontmatter": sem_fm,
        "schema_canonico": canonicos,
        "schema_nao_canonico": nao_canonicos,
        "estados": estados,
        "abertos": abertos,
        "gate_fails": gate_fails,
        "desvios_total": desvios_total,
        "desvios_por_area": desvios_por_area,
        "pendencias_humano_total": pend_humano_total,
        "testes_total": testes_total,
        "testes_novos": testes_novos,
    }


def imprimir(m: dict) -> None:
    total = m["total_reports"]
    cobertos = m["com_frontmatter"]
    pct = (100 * cobertos / total) if total else 0
    print("=" * 56)
    print("  Metricas dos status reports")
    print("=" * 56)

    print("\nCobertura do schema : {}/{} com frontmatter ({:.0f}%)".format(
        cobertos, total, pct))
    print("  canonico (chave estado)  : {}".format(m["schema_canonico"]))
    nao_can = m["schema_nao_canonico"]
    print("  frontmatter nao-canonico : {}".format(len(nao_can)))
    for nome in nao_can:
        print("    - " + nome)
    sem = m["sem_frontmatter"]
    if sem:
        print("  sem frontmatter (legado) : {}".format(len(sem)))
        for nome in sem:
            print("    - " + nome)

    print("\nEstado das tasks:")
    for estado, n in sorted(m["estados"].items()):
        print("  {:<12} {}".format(estado, n))

    print("\nTrabalho em aberto (parcial/bloqueado):")
    if m["abertos"]:
        for a in m["abertos"]:
            print("  - " + a)
    else:
        print("  nenhum")

    print("\nAtrito:")
    gf = m["gate_fails"]
    print("  gate-fails        : {}  {}".format(len(gf), gf if gf else ""))
    dpa = m["desvios_por_area"]
    print("  desvios (total)   : {}  {}".format(
        m["desvios_total"], ("por area: " + str(dpa)) if dpa else ""))
    print("  pendencias humano : {}".format(m["pendencias_humano_total"]))

    print("\nTestes (contexto):")
    print("  soma testes_total : {}".format(m["testes_total"]))
    print("  soma testes_novos : {}".format(m["testes_novos"]))
    print()


def _default_status_dirs() -> list:
    """Acha todas as pastas `status/` sob `docs/`: docs/status + docs/sprints/<NN>/status."""
    docs_root = Path(__file__).resolve().parent.parent  # docs/
    dirs = []
    legacy = docs_root / "status"
    if legacy.is_dir():
        dirs.append(legacy)
    sprints_root = docs_root / "sprints"
    if sprints_root.is_dir():
        for sprint_dir in sorted(sprints_root.iterdir()):
            if sprint_dir.is_dir():
                sd = sprint_dir / "status"
                if sd.is_dir():
                    dirs.append(sd)
    return dirs


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(description="Metricas dos status reports.")
    ap.add_argument(
        "--dir",
        action="append",
        help="pasta dos status reports (repetivel); default = docs/status + docs/sprints/*/status",
    )
    ap.add_argument("--json", action="store_true", help="saida em JSON")
    args = ap.parse_args(argv)

    if args.dir:
        status_dirs = [Path(d) for d in args.dir]
    else:
        status_dirs = _default_status_dirs()

    if not status_dirs:
        print("erro: nenhuma pasta de status encontrada", file=sys.stderr)
        return 2
    for d in status_dirs:
        if not d.is_dir():
            print("erro: pasta nao encontrada: {}".format(d), file=sys.stderr)
            return 2

    m = coletar(status_dirs)
    if args.json:
        print(json.dumps(m, ensure_ascii=False, indent=2))
    else:
        imprimir(m)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
