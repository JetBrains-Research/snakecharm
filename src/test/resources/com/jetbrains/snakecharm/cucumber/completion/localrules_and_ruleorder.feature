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
    And completion list shouldn't contain:
      | rule1       |

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
    And completion list shouldn't contain:
      | rule3       |