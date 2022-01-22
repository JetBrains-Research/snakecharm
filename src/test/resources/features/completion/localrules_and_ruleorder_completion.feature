Feature: Completion for rule names in localrules and ruleorder sections

  Scenario: Complete in localrules section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule rule1:
      input: "input.txt"

    rule rule2:
      output: touch("output.txt"mamm
    rule rule3:
      output: touch("_output.txt")

    use rule rule3 as rule4 with:
      output: touch("_output_rule4.txt")

    localrules: rule1,
    """
    When I put the caret after rule1,
    And I invoke autocompletion popup
    Then completion list should only contain:
      | rule2 |
      | rule3 |
      | rule4 |

  Scenario: Complete in ruleorder section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule rule1:
      input: "input.txt"

    rule rule2:
      output: touch("output.txt")

    rule rule3:
      output: touch("_output.txt")

    use rule rule3 as rule4 with:
      output: touch("_output_rule4.txt")

    ruleorder: rule3 >
    """
    When I put the caret after rule3 >
    And I invoke autocompletion popup
    Then completion list should only contain:
      | rule1 |
      | rule2 |
      | rule4 |

  Scenario Outline: Complete in localrules/ruleorder section from included files
    Given a snakemake project
    Given a file "boo.smk" with text
    """
    rule rule4:
        input: "path/to/input"

    use rule rule4 as rule6 with:
      output: touch("_output_rule6.txt"
    """
    Given a file "soo.smk" with text
    """
    include: "boo.smk"
    rule rule5:
        shell: "echo hello"
    """
    Given a file "goo.smk" with text
    """
    include: "boo.smk"
    rule rule6:
        shell: "echo hello"
    """
    Given I open a file "foo.smk" with text
    """
    rule rule1:
      input: "input.txt"

    rule rule2:
      output: touch("output.txt")

    rule rule3:
      output: touch("_output.txt")

    <section>: rule3 <separator>

    include: "soo.smk"
    """
    When I put the caret after rule3 <separator>
    And I invoke autocompletion popup
    Then completion list should only contain:
      | rule1 |
      | rule2 |
      | rule4 |
      | rule5 |
      | rule6 |
    Examples:
      | section    | separator |
      | localrules | ,         |
      | ruleorder  | >         |

  Scenario Outline: second completion popup invocation produces all indexed rules
    Given a snakemake project
    Given a file "boo.smk" with text
    """
    rule boo1: input: "file.txt"

    rule boo2: input: "file1.txt"

    use rule boo2 as boo3 with:
      input: "file2.txt"
    """
    Given a file "soo.smk" with text
    """
    rule soo: input: "soo.txt"
    """
    Given I open a file "foo.smk" with text
    """
    include: "soo.smk"

    rule foo1:
      input: "file.txt"

    rule foo2:
      input: "file.txt"

    <section>: foo1 <separator>
    """
    When I put the caret after foo1 <separator>
    And I invoke autocompletion popup 2 times
    Then completion list should only contain:
      | foo2 |
      | soo  |
      | boo1 |
      | boo2 |
      | boo3 |
    Examples:
      | section    | separator |
      | localrules | ,         |
      | ruleorder  | >         |

