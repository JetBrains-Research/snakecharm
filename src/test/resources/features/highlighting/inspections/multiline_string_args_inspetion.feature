Feature: Inspection for multiline arguments in same section
  Scenario Outline: Multiline string argument in execution section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        <section>: "a" "b", "c"
    """
    And SmkSectionMultilineStringArgsInspection inspection is enabled
    Then I expect inspection warning on <"a" "b"> with message
    """
    Multiline string argument in '<section>' will be considered as concatenation.
    """
    When I check highlighting warnings
    Examples:
      | section    | rule_like  |
      | input      | rule       |
      | output     | rule       |
      | log        | rule       |
      | input      | checkpoint |
      | output     | checkpoint |
      | log        | checkpoint |