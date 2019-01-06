rule compose_merge:
    run:
        with open(output.txt, 'w') as out:
            foo(*input, sep="\n", file=out)
        value = 2 + 3
        foo("foo", str(value))
        foo("foo"
              "dooo",
              "sss")
