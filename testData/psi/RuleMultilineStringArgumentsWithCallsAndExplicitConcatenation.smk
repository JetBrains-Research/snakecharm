rule NAME:
    shell:
           "mkdir -p" +
           "multiline "
                     "string"
                          .join(['a', 'b', 'c'])
           + "string".join([]) + "text"
           "newline"