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

    use rule <name> from M as NAME_other with:
      input:
        "data_file.txt"
    """
    When I put the caret at <name>
    Then reference should resolve to "<name>" in "boo.smk"
    Examples:
      | name        | rule_like  |
      | z           | rule       |
      | z           | checkpoint |
      | updated_zoo | rule       |

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

    use rule <name> from M as NAME_other with:
      input:
        "data_file.txt"
    """
    When I put the caret at <name>
    Then reference should resolve to "<resolve_to>" in "zoo.smk"
    Examples:
      | name          | resolve_to    | rule_like  |
      | zoo_rule      | zoo_rule      | rule       |
      | zoo_rule      | zoo_rule      | checkpoint |
      | rule_from_zoo | rule_from_zoo | rule       |

  Scenario Outline: Refer to rule, declared in appropriate file. Same file case
    Given a snakemake project
    And a file "boo.smk" with text
    """
    <rule_like> zoo_rule: threads: 1
    """
    Given I open a file "foo.smk" with text
    """
    module M:
      snakefile: "boo.smk"

    use rule zoo_rule from M with:
      threads: 2

    use rule zoo_rule as zoo_rule_other with:
      input:
        "data_file.txt"
    """
    When I put the caret at zoo_rule as
    Then reference should resolve to "zoo_rule" in context "zoo_rule from" in file "foo.smk"
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Do not resolve reference of imported rule to local rule with the same name
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    module M:
      snakefile: "boo.smk"

    <rule_like> <name>:
      log: "{sample}"
      benchmark: "{sample1}"

    use rule <name> from M with:
      input:
        "data_file.txt"
    """
    When I put the caret at <name> from
    Then reference should not resolve
    Examples:
      | name   | rule_like  |
      | rule_a | rule       |
      | rule_a | checkpoint |

  Scenario Outline: Refer to another use identifier which declared new rules with wildcard
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    use rule <original> with:
      output: "dir/log.log"

    use rule other_o as NAME_other with:
      input:
        "data_file.txt"
    """
    When I put the caret at other_o as NAME
    Then reference should resolve to "<resolve_to>" in "foo.smk"
    Examples:
      | original                  | resolve_to      |
      | a,o,c from M as other_*   | other_*         |
      | a,o,c from M as o th er_* | o th er_*       |
      | o from M as *ther_*       | *ther_*         |
      | other_a,other_o from M as | other_a,other_o |

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

  Scenario: Module name resolving results shouldn't contain use sections for which the module name matched name pattern
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

  Scenario: Resolve to rule imported via 'include'
    Given a snakemake project
    Given I open a file "boo.smk" with text
    """
    rule A:
      threads: 1
    """
    Given I open a file "foo.smk" with text
    """
    include: "boo.smk"

    use rule A as B with:
      threads: 2
    """
    When I put the caret at A
    Then reference should resolve to "A" in "boo.smk"