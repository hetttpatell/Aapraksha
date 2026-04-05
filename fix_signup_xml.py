import sys

file_path = "c:\\Users\\Jitu\\AndroidStudioProjects\\Aapraksha\\app\\src\\main\\res\\layout\\activity_signup.xml"

with open(file_path, "r", encoding="utf-8") as f:
    lines = f.readlines()

# Python list is 0-indexed.
# Lines to delete: 257 to 451 (which is index 256 to 450)
del lines[256:451]

with open(file_path, "w", encoding="utf-8") as f:
    f.writelines(lines)

print("Fixed activity_signup.xml")
