import sys
import os
import re

def extract_release_notes(input_path, output_path, target_version=None):
    if not os.path.exists(input_path):
        print(f"Error: input file {input_path} not found.")
        sys.exit(1)

    with open(input_path, 'r', encoding='utf-8') as f:
        content = f.read()

    notes = ""
    if target_version:
        v_clean = target_version.lstrip('v').strip()
        pattern = rf'(###\s+.*?v?{re.escape(v_clean)}[\s\S]*?)(?=\n---\n|\n###\s+|\Z)'
        match = re.search(pattern, content)
        if match:
            notes = match.group(1).strip()

    if not notes:
        # Fallback: extract the very first '### ' section
        match = re.search(r'(###\s+[\s\S]*?)(?=\n---\n|\n###\s+|\Z)', content)
        if match:
            notes = match.group(1).strip()
        else:
            notes = content.strip()

    footer = "\n\n---\n> 📖 更多历史版本更新记录请参阅项目完整 [RELEASE_NOTES.md](https://github.com/vimbackground/woofish/blob/master/RELEASE_NOTES.md)。"
    final_notes = notes + footer

    with open(output_path, 'w', encoding='utf-8') as f:
        f.write(final_notes + "\n")

    print(f"Successfully extracted {len(notes)} chars of release notes to {output_path}")

if __name__ == '__main__':
    input_file = sys.argv[1] if len(sys.argv) > 1 else 'RELEASE_NOTES.md'
    output_file = sys.argv[2] if len(sys.argv) > 2 else 'CURRENT_RELEASE_NOTES.md'
    version = sys.argv[3] if len(sys.argv) > 3 else None
    extract_release_notes(input_file, output_file, version)
