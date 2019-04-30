DATASETS = []

rule all:
  input: directory(ex<caret>pand("{dataset}/dir", dataset=DATASETS))