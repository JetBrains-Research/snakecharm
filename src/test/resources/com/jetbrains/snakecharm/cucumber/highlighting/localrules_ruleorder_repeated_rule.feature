Feature: localrules/ruleorder repeated rule inspection
  Scenario: No repeated rules in localrules
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
    """
    And Repeated Rule in Localrules or Ruleorder inspection is enabled
    Then I expect no inspection warning
    When I check highlighting errors

  Scenario: No repeated rules in ruleorder
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    ruleorder: rule2 > rule3 > rule1

    rule rule1:
        input: "input.txt"
        output: "output.txt"

    rule rule2:
        output: touch("file.txt")

    rule rule3:
        output: touch("output.txt")
    """
    And Repeated Rule in Localrules or Ruleorder inspection is enabled
    Then I expect no inspection warning
    When I check highlighting errors


  Scenario: Repeated rules in localrules
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

    localrules:
        rule1,
        rule2,
        rule3,
        rule2, # repeated
        rule1, # repeated
        rule3  # repeated
    """
    And Repeated Rule in Localrules or Ruleorder inspection is enabled
    Then I expect inspection weak warning on <rule2> in <rule2, # repeated> with message
    """
    This rule has already been added to this section.
    """
    And I expect inspection weak warning on <rule1> in <rule1, # repeated> with message
    """
    This rule has already been added to this section.
    """
    And I expect inspection weak warning on <rule3> in <rule3  # repeated> with message
    """
    This rule has already been added to this section.
    """
    When I check highlighting errors
