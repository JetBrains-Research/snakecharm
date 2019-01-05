@workflow.input(
    "{dataset}/inputfile"
    "fff"
)

rule samtools_sort:
    input:
        "foo"
        "boo"

rule samtools_sort:
    input:
        "foo" \
        "boo"