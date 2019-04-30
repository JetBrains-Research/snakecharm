DATASETS = []

rule NAME:
    input: "path/to/input"
    output: "path/to/output"
    run:
        datasets = exp<caret>and("{dataset}/file.A.txt", dataset=DATASETS)