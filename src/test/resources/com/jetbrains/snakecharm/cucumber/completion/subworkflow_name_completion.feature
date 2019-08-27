Feature: Completion for subworkflow name

  Scenario: Complete subworkflow name
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    subworkflow otherworkflow:
    workdir:
        "../path/to/otherworkflow"
    snakefile:
        "../path/to/otherworkflow/Snakefile"
    configfile:
        "path/to/custom_configfile.yaml"

    rule a:
      input:
          othe#here
    """
    When I put the caret at #here
    Then I invoke autocompletion popup and see a text:
    """
    subworkflow otherworkflow:
    workdir:
        "../path/to/otherworkflow"
    snakefile:
        "../path/to/otherworkflow/Snakefile"
    configfile:
        "path/to/custom_configfile.yaml"

    rule a:
      input:
          otherworkflow#here
    """