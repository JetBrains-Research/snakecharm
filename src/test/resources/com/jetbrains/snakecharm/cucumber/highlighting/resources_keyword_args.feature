Feature: Inspection checking whether all resources section arguments are keyword arguments
  Scenario Outline: All arguments in 'resources' are keyword arguments
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input: "input.txt"
        output: "output.txt"
        resources: threads=4, mem_mb=100
        shell: "command"
    """
    And Resources Keyword Arguments inspection is enabled
    Then I expect no inspection error
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |


  Scenario Outline: Positional argument in resources section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input: "input.txt"
        output: "output.txt"
        resources: 4, mem_mb=100
        shell: "command"
    """
    And Resources Keyword Arguments inspection is enabled
    Then I expect inspection error on <4> with message
    """
    Resources have to be named (e.g. 'threads=4').
    """
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |