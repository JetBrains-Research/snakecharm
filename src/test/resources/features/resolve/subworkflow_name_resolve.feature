Feature: Resolve subworkflow name to its corresponding declaration
  Scenario: Resolve for a particular rule name when 'rules' is used inside a rule section
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
        otherworkflow()
    """
    When I put the caret at otherworkflow()
    Then reference should resolve to "otherworkflow" in "foo.smk"