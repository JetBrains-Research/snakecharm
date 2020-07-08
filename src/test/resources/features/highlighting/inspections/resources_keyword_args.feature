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
    And SmkResourcesKeywordArgsInspection inspection is enabled
    Then I expect no inspection errors
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
    And SmkResourcesKeywordArgsInspection inspection is enabled
    Then I expect inspection error on <4> with message
    """
    Resources have to be named (e.g. 'threads=4').
    """
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Name an unnamed (positional) resources section argument
      Given a snakemake project
      Given I open a file "foo.smk" with text
        """
        <rule_like> NAME:
          resources: 4
        """
      And SmkResourcesKeywordArgsInspection inspection is enabled
      Then I expect inspection error on <4> with message
        """
        Resources have to be named (e.g. 'threads=4').
        """
      When I check highlighting errors
      And I invoke quick fix Name argument and see text:
      """
      <rule_like> NAME:
        resources: arg=4
      """
      Examples:
        | rule_like  |
        | rule       |
        | checkpoint |