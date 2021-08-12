Feature: Resolve use and module name to its declaration
  Scenario Outline: Refer to rule section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      threads: 12

    use rule NAME as NAME_other with:
      input:
        "data_file.txt"
    """
    When I put the caret at NAME as
    Then reference should resolve to "NAME:" in "foo.smk"
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

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

  Scenario Outline: Refer to rules, declared in other modules
    Given a snakemake project
    And a file "boo.smk" with text
    """
    <rule_like> z:
      input: "data_file.csv"

    use rule z as updated_zoo with: threads: 3
    """
    Given I open a file "foo.smk" with text
    """
    module M:
      snakefile: "boo.smk"

    use rule * from M as other_*

    use rule <name> as NAME_other with:
      input:
        "data_file.txt"
    """
    When I put the caret at <name>
    Then reference should resolve to "<resolve_to>" in "boo.smk"
    Examples:
      | name              | resolve_to  | rule_like  |
      | other_z           | z           | rule       |
      | other_z           | z           | checkpoint |
      | other_updated_zoo | updated_zoo | rule       |

  Scenario Outline: Refer to rules, declared in other .smk file included into module
    Given a snakemake project
    And a file "boo.smk" with text
    """
    include: "zoo.smk"
    """
    And a file "zoo.smk" with text
    """
    <rule_like> zoo_rule: threads: 1

    use rule zoo_rule as rule_from_zoo with: threads: 2
    """
    Given I open a file "foo.smk" with text
    """
    module M:
      snakefile: "boo.smk"

    use rule * from M as other_*

    use rule <name> as NAME_other with:
      input:
        "data_file.txt"
    """
    When I put the caret at <name>
    Then reference should resolve to "<resolve_to>" in "zoo.smk"
    Examples:
      | name                | resolve_to    | rule_like  |
      | other_zoo_rule      | zoo_rule      | rule       |
      | other_zoo_rule      | zoo_rule      | checkpoint |
      | other_rule_from_zoo | rule_from_zoo | rule       |


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

  Scenario: Module name refer to module declaration
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

  Scenario: Module name refer to module declaration, confused scenario
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    module MODULE:
      snakefile:
        "../path/to/otherworkflow/Snakefile"
      configfile:
        "path/to/custom_configfile.yaml"

    use rule * from MODULE as *
    """
    When I put the caret at MODULE as
    Then reference should resolve to "MODULE:" in "foo.smk"