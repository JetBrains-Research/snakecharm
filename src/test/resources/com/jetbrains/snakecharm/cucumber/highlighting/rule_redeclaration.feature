Feature: Rule redeclaration inspection
  Scenario: A single rule redeclaration
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
        input: "input.txt"
        output: "output.txt"
        resources: threads=4, mem_mb=100
        shell: "command"

    rule ANOTHER_NAME:
        output: touch("file.txt")

    rule NAME: #overrides
        output: touch("output.txt")
    """
    And Rule Redeclaration inspection is enabled
    Then I expect inspection error on <NAME> in <rule NAME: #overrides> with message
    """
    This rule name is already used by another rule.
    """
    When I check highlighting errors


  Scenario: Multiple rule redeclarations
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
        input: "input.txt"
        output: "output.txt"
        resources: threads=4, mem_mb=100
        shell: "command"

    rule ANOTHER_NAME:
        output: touch("file.txt")

    rule NAME: #overrides1
        output: touch("output.txt")

    rule ANOTHER_NAME: #overrides
        output: touch("file.txt")

    rule NAME: #overrides2
        output: touch("output.txt")
    """
    And Rule Redeclaration inspection is enabled
    Then I expect inspection error on <NAME> in <rule NAME: #overrides1> with message
    """
    This rule name is already used by another rule.
    """
    And I expect inspection error on <ANOTHER_NAME> in <rule ANOTHER_NAME: #overrides> with message
    """
    This rule name is already used by another rule.
    """
    And I expect inspection error on <NAME> in <rule NAME: #overrides2> with message
    """
    This rule name is already used by another rule.
    """
    When I check highlighting errors

  Scenario: Checkpoint redeclaration
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    checkpoint NAME:
        input: "input.txt"
        output: "output.txt"
        resources: threads=4, mem_mb=100
        shell: "command"

    rule ANOTHER_NAME:
        output: touch("file.txt")

    rule NAME: #overrides
        output: touch("output.txt")
    """
    And Rule Redeclaration inspection is enabled
    Then I expect inspection error on <NAME> in <rule NAME: #overrides> with message
    """
    This rule name is already used by another rule.
    """
    When I check highlighting errors