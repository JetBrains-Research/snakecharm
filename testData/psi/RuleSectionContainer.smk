rule foo:
    container: "docker://joseespinosa/docker-r-ggplot2"
    script:
        "foo.R"