"""把有道词库 JSON 转成 App 的纯文本词表格式：word|词性|中文释义。

用法：
    python tools/ingest_wordlist.py <输入.json> <输出.txt> [来源说明]

输入支持两种形态：整个文件是一个 JSON 数组，或每行一个 JSON 对象（NDJSON）。
"""

import io
import json
import re
import sys

POS_LETTERS = {
    "n": "n", "noun": "n", "pl": "n",
    "v": "v", "vt": "v", "vi": "v", "verb": "v",
    "a": "a", "adj": "a", "adjective": "a",
    "d": "d", "adv": "d", "adverb": "d",
}
WORD_SHAPE = re.compile(r"^[A-Za-z]+(?:[-'][A-Za-z]+)*$")


def records(raw: str):
    text = raw.strip()
    if text.startswith("["):
        yield from json.loads(text)
        return
    for line in text.splitlines():
        line = line.strip()
        if line:
            yield json.loads(line)


def head_word(rec: dict) -> str:
    return str(rec.get("headWord") or rec.get("word", {}) or "").strip()


def trans_list(rec: dict) -> list:
    content = rec.get("content") or {}
    word = content.get("word") or {}
    inner = word.get("content") or {}
    return inner.get("trans") or []


def pos_letters(trans: list) -> str:
    letters = set()
    for entry in trans:
        mapped = POS_LETTERS.get(str(entry.get("pos", "")).strip().lower().rstrip("."))
        if mapped:
            letters.add(mapped)
    return " ".join(sorted(letters))


def gloss(trans: list) -> str:
    parts = []
    for entry in trans:
        pos = str(entry.get("pos", "")).strip()
        cn = str(entry.get("tranCn", "")).strip().replace("\n", " ")
        cn = re.sub(r"\s+", " ", cn)
        if not cn:
            continue
        parts.append(f"{pos} {cn}".strip())
    return "；".join(parts[:2])


def main() -> int:
    if len(sys.argv) < 3:
        print(__doc__)
        return 2
    src, dst = sys.argv[1], sys.argv[2]
    source_note = sys.argv[3] if len(sys.argv) > 3 else src

    seen = {}
    skipped = 0
    for rec in records(io.open(src, encoding="utf-8").read()):
        word = head_word(rec).lower()
        if not word or not WORD_SHAPE.match(word) or word in seen:
            skipped += 1
            continue
        trans = trans_list(rec)
        seen[word] = (pos_letters(trans), gloss(trans))

    with io.open(dst, "w", encoding="utf-8", newline="\n") as out:
        out.write(f"# 来源：{source_note}\n")
        out.write("# 格式：word|词性|中文释义。词性含 n/v/a/d 时分别控制生成复数/动词变形/比较级。\n")
        out.write(f"# 词条数：{len(seen)}（跳过 {skipped} 条：词组、重复或非法字符）\n")
        for word in sorted(seen):
            pos, cn = seen[word]
            out.write(f"{word}|{pos}|{cn}\n" if cn or pos else f"{word}\n")

    print(f"{src} -> {dst}: {len(seen)} 条，跳过 {skipped} 条")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
