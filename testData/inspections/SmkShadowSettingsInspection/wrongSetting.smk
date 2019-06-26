rule NAME:
    input:
         "inputfile.txt"
    output:
          "outputfile.txt"
    shadow: <warning descr="Only 'full', 'shallow' and 'minimal' shadow settings are allowed.">"parameter"</warning>