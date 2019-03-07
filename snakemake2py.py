import argparse
import snakemake.parser as smp

def _cli():
    ########################################################################
    # First install snakemake module
    parser = argparse.ArgumentParser(
        description="Converts *.smk file to *.py using snakemake parser",
        formatter_class=argparse.ArgumentDefaultsHelpFormatter
    )
    parser.add_argument("input", metavar="INPUT",
                        help="Snakemake file (or *.smk file)")

    parser.add_argument("output", metavar="OUTPUT",
                        help="Result python file")

    args = parser.parse_args()
    input_path = args.input
    output_path = args.output

    ########################################################################

    compilation, _linemap, rc = smp.parse(input_path)
    with open(output_path, mode="w") as out:
        print(compilation, file=out)
    print("Done, rules:", str(rc))
    print("Saved to :", output_path)

if __name__ == "__main__":
    _cli()
