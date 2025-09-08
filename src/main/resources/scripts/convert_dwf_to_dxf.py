import sys
import FreeCAD
import ImportGui

# 1️⃣ 커맨드라인 인자로 파일 경로 받기
input_file = sys.argv[1]   # DWF 파일 경로
output_file = sys.argv[2]  # DXF 파일 경로

# 2️⃣ 새 문서 생성
doc = FreeCAD.newDocument()

# 3️⃣ DWF 파일 불러오기
ImportGui.insert(input_file, doc.Name)

# 4️⃣ DXF로 내보내기
ImportGui.export(doc.Objects, output_file)
