rule NAME:
    input:
         "inputfile.txt"
    output:
          "outputfile.txt"
    shadow: <warning descr="Shadow must either be 'shallow', 'full', 'minimal', or True (equivalent to 'full').">"parameter"</warning>