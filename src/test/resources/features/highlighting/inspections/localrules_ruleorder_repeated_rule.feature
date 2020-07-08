Feature: localrules/ruleorder repeated rule inspection
  Scenario: no false positives for same rule in localrules and ruleorder
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule rule1:
        input: "input.txt"
        output: "output.txt"

    rule rule2:
        output: touch("file.txt")

    rule rule3:
        output: touch("output.txt")

    localrules: rule1, rule2, rule3
    ruleorder: rule1 > rule2 > rule3
    """
    And Repeated Rule in Localrules or Ruleorder inspection is enabled
    Then I expect no inspection warning
    When I check highlighting warnings

  Scenario Outline: No repeated rules in localrules/ruleorder
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule rule1:
        input: "input.txt"
        output: "output.txt"

    rule rule2:
        output: touch("file.txt")

    rule rule3:
        output: touch("output.txt")
        
    <section>: rule1<separator> rule2<separator> rule3
    """
    And Repeated Rule in Localrules or Ruleorder inspection is enabled
    Then I expect no inspection warning
    When I check highlighting warnings
    Examples:
    | section    | separator |
    | localrules | ,         |
    | ruleorder  | >         |

  Scenario Outline: Repeated rules in localrules/ruleorder
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule rule1:
        input: "input.txt"
        output: "output.txt"

    checkpoint rule2:
        output: touch("file.txt")

    rule rule3:
        output: touch("output.txt")

    <section_name>:
        rule1
        <separator> rule2
        <separator> rule3
        <separator> rule2 # repeated
        <separator> rule1 # repeated
        <separator> rule3 # repeated
    """
    And Repeated Rule in Localrules or Ruleorder inspection is enabled
    Then I expect inspection warning on <rule2> in <rule2 # repeated> with message
    """
    This rule has already been added to this section.
    """
    And I expect inspection warning on <rule1> in <rule1 # repeated> with message
    """
    This rule has already been added to this section.
    """
    And I expect inspection warning on <rule3> in <rule3 # repeated> with message
    """
    This rule has already been added to this section.
    """
    When I check highlighting warnings
    Examples:
    | section_name | separator |
    | localrules   | ,         |
    | ruleorder    | >         |
