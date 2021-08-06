Feature: Resolve use and module name to its declaration
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
    Then reference should resolve to "NAME" in "foo.smk"

  Scenario: Refer to other file
    Given a snakemake project
    And a file "foo.smk" with text
    """
    module M:
      snakefile: "boo.smk"

    use rule * from M as other_*

    use rule other_z as NAME_other with:
      input:
        "data_file.txt"
    """
    And a file "boo.smk" with text
    """
    rule z:
      input: "data_file.csv"
    """
    Given I open a file "foo.smk" with text
    """
    module M:
      snakefile: "boo.smk"

    use rule * from M as other_* with:
      output: "dir/log.log"

    use rule other_z as NAME_other with:
      input:
        "data_file.txt"
    """
    When I put the caret at other_z
    Then reference should resolve to "z" in "boo.smk"

  Scenario Outline: Refer to other use section which declared new rules with wildcard
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    use rule <original> with:
      output: "dir/log.log"

    use rule other_b as NAME_other with:
      input:
        "data_file.txt"
    """
    When I put the caret at other_b as NAME
    Then reference should resolve to "<resolve_to>" in "foo.smk"
    Examples:
      | original                  | resolve_to |
      | a,b,c from M as other_*   | b          |
      | other_a,other_b from M as | other_b    |

  Scenario Outline: Refer to MODULE which imports a rule
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    module MODULE:
      snakefile:
        "https://usefullsmkfiles.com/file.smk "
      configfile:
        "path/to/custom_configfile.yaml"

    use rule NAME from MODULE as <name> with:
      input:
        "data_file.txt"
    """
    When I put the caret at NAME
    Then reference should resolve to "MODULE" in "foo.smk"
    Examples:
      | name  |
      | other |
      |       |

  Scenario: Module name refere to module declaration
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
    When I put the caret at MODULE as
    Then reference should resolve to "MODULE:" in "foo.smk"