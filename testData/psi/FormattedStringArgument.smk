rule NAME:
    shell:
           "multiline"
           f"{30 + 42}"
           "string"
               f"arg{25}"

df = []

onstart:
    print(f"Total files: {len(df)}")

rule a:
    output: "out"