DATASETS = []

rule all:
  input: exp<ref>and("{dataset}/file.A.txt", dataset=DATASETS)