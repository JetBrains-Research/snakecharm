DATASETS = []

rule all:
  input: e<caret>xpand("{dataset}/file.A.txt", dataset=DATASETS)