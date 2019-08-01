Feature: Completion for rule names in localrules and ruleorder sections

  Scenario: Complete in localrules section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule rule1:
      input: "input.txt"

    rule rule2:
      output: touch("output.txt")

    rule rule3:
      output: touch("_output.txt")

    localrules: rule1,
    """
    When I put the caret after rule1,
    And I invoke autocompletion popup
    Then completion list should contain:
      | rule2       |
      | rule3       |

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

    ruleorder: rule3 >
    """
    When I put the caret after rule3 >
    And I invoke autocompletion popup
    Then completion list should contain:
      | rule1       |
      | rule2       |

  Scenario Outline: Complete in localrules/ruleorder section from included files
    Given a snakemake project
    Given a file "boo.smk" with text
    """
    rule rule4:
        input: "path/to/input"
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
    Then completion list should contain:
      | rule1 |
      | rule2 |
      | rule4 |
      | rule5 |
    And completion list shouldn't contain:
      | rule6 |
    Examples:
      | section    | separator |
      | localrules | ,         |
      | ruleorder  | >         |
