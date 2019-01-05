rule compose_merge:
    run:
        with open(output.txt, 'w') as out:
            print(*input, sep="\n", file=out)
        value = 2 + 3
        print("foo", str(value))
        print("foo"
              "dooo",
              "sss")
