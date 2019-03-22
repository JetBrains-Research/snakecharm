DATASETS = []

rule all:
  input: expand<ref>("{dataset}/file.A.txt", dataset=DATASETS)