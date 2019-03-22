DATASETS = []

rule all:
  input: expand("{dataset}/file.A.txt", dataset=DATASETS)
#         <ref>