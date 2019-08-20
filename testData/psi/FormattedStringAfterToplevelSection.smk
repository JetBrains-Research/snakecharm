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
    f"text {print(30)}" # this is not a correct piece of code; this is a formatted string test

rule c:
    output: "out"