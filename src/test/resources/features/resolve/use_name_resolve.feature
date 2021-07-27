Feature: Resolve use name to its declaration
  Scenario: Refer to rule section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      threads: 12

    use rule NAME as NAME_other with:
      input:
        "data_file.txt"
    """
    When I put the caret at NAME as
    Then reference should resolve to "NAME:" in "foo.smk"

  Scenario: Refer to other use section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    use rule foo as NAME with:
      output: "dir/log.log"

    use rule NAME as NAME_other with:
      input:
        "data_file.txt"
    """
    When I put the caret at NAME as
    Then reference should resolve to "NAME with" in "foo.smk"

  Scenario: Refer to MODULE which imports a rule
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    module MODULE:
      snakefile:
        "../path/to/otherworkflow/Snakefile"
      configfile:
        "path/to/custom_configfile.yaml"

    use rule NAME from MODULE as other with:
      input:
        "data_file.txt"
    """
    When I put the caret at NAME
    Then reference should resolve to "MODULE:" in "foo.smk"