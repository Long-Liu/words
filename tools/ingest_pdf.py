"""从"序号 / 单词 / 音标 / 词义"四列表格的背单词 PDF 里抽词表。

用法：
    python tools/ingest_pdf.py <输入.pdf> <输出.txt> [来源说明]

按行读文本流，用"序号行 + 紧随其后的英文行"或"序号 单词 同行"作为一条词条的
锚点，锚点之后到下一个锚点之前的内容算释义，音标行丢弃。
"""

import io
import re
import sys

import pymupdf

WORD_LINE = re.compile(r"^[A-Za-z][A-Za-z'’.-]*$")
INDEX_LINE = re.compile(r"^\d{1,4}$")
COMBINED_LINE = re.compile(r"^(\d{1,4})\s+([A-Za-z][A-Za-z'’.-]+)\s*(.*)$")
PHONETIC_LINE = re.compile(r"^[/\[]|^[ˈˌæŋəʃʒɪʌɜ ]+$")
POS_TOKEN = re.compile(
    r"(?<![A-Za-z])(adv|adj|vt|vi|v|n|prep|conj|pron|num|int|abbr|aux)\.(?![A-Za-z])", re.I
)
POS_LETTERS = {"n": "n", "adj": "a", "adv": "d", "v": "v", "vt": "v", "vi": "v"}


def page_lines(doc):
    for page in doc:
        for line in page.get_text().splitlines():
            yield line.strip()


def parse(lines):
    entries = []
    pending = None
    expect_word = False

    def close():
        nonlocal pending
        if pending:
            entries.append(pending)
            pending = None

    for line in lines:
        if not line:
            continue
        if INDEX_LINE.match(line):
            close()
            expect_word = True
            continue
        combined = COMBINED_LINE.match(line)
        if combined:
            close()
            pending = {"word": combined.group(2).lower(), "gloss": []}
            if combined.group(3):
                pending["gloss"].append(combined.group(3))
            expect_word = False
            continue
        if expect_word and WORD_LINE.match(line):
            pending = {"word": line.lower().rstrip("."), "gloss": []}
            expect_word = False
            continue
        if pending is not None and not PHONETIC_LINE.match(line):
            pending["gloss"].append(line)
    close()
    return entries


def clean_gloss(parts):
    return re.sub(r"\s+", "", " ".join(parts)).strip("；;，, ")


def main():
    if len(sys.argv) < 3:
        print(__doc__)
        return 2
    src, dst = sys.argv[1], sys.argv[2]
    note = sys.argv[3] if len(sys.argv) > 3 else src

    doc = pymupdf.open(src)
    seen = {}
    for entry in parse(page_lines(doc)):
        word = entry["word"]
        if not word or word in seen:
            continue
        gloss = clean_gloss(entry["gloss"])
        letters = set()
        for match in POS_TOKEN.finditer(gloss):
            mapped = POS_LETTERS.get(match.group(1).lower())
            if mapped:
                letters.add(mapped)
        seen[word] = (" ".join(sorted(letters)), gloss)
    doc.close()

    with io.open(dst, "w", encoding="utf-8", newline="\n") as out:
        out.write(f"# 来源：{note}\n")
        out.write("# 格式：word|词性|中文释义。词性含 n/v/a/d 时分别控制生成复数/动词变形/比较级。\n")
        out.write(f"# 词条数：{len(seen)}\n")
        for word in sorted(seen):
            pos, gloss = seen[word]
            out.write(f"{word}|{pos}|{gloss}\n")

    print(f"{src} -> {dst}: {len(seen)} 条")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
