df = []

onstart:
    print(f"Total files: {len(df)}")

rule a:
    output: "out"

include:
    f"{os.path.join(tt1, tt2)}"

rule b:
    output: "out"

rule:
    f"text {print(30)}" # this is a docstring, snakemake accepts such syntax

rule c:
    output: "out"

rule foo: f"text: { {latitude: a, longitude: b}  }"
    input: expand(f"sorted_reads/{os.path.join(dir1, dir2, file2)}", sample=config["samples"])
    shell: "echo hello"

rule foo: input: "fjkd"