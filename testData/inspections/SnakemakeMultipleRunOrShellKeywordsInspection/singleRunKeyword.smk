rule NAME:
    input:
        "inputfile.txt"
    output:
        "outputfile.txt"
    run:
        print('string')