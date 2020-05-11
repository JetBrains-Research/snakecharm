files = {"file1.txt": ["file1_a.pdf", "file1_b.pdf"],
         "file2.txt": ["file2_a.pdf"]}

rule all:
    input: files.values()

for input_files, output_files in files.items():
    rule:
        input: input_files
        output: output_files
        shell: "python scripy.py "
               "--input {input} "
               "--output {output} "
               "--many "
               "--other "
               "--flags "