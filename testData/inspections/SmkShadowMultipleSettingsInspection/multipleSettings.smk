rule NAME:
    input:
         "inputfile.txt"
    output:
          "outputfile.txt"
    shadow <error descr="Only one argument after shadow section is permitted.">:"full", "shallow"</error>