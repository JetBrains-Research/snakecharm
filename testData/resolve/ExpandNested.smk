DATASETS = []

rule all:
  input: directory(expand("{dataset}/dir", dataset=DATASETS))
#                    <ref>