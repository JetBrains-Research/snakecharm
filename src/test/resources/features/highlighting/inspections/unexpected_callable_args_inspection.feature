Feature: Inspection for unexpected callable arguments in section

  Scenario Outline: Unexpected callable arguments
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    def bar():
        return "text"

    <rule_like> NAME:
        <section>: bar
    """
    And SmkSectionUnexpectedCallableArgsInspection inspection is enabled
    Then I expect inspection error on <bar> in <<section>: bar> with message
    """
    Section '<section>' does not support callable arguments
    """
    When I check highlighting errors
    Examples:
      | rule_like   | section    |
      | subworkflow | configfile |
      | rule        | benchmark  |
      | rule        | cache      |
      | rule        | output     |
      | rule        | container  |
      | rule        | cwl        |
      | rule        | log        |
      | checkpoint  | message    |
      | checkpoint  | notebook   |
      | checkpoint  | priority   |
      | checkpoint  | script     |
      | checkpoint  | shadow     |
      | checkpoint  | shell      |
      | checkpoint  | wrapper    |

  Scenario Outline: No warn on expected callable arguments
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    def foo():
        return "text"
    def bar(arg):
        return str(arg)
    MSG="msg"

    <rule_like> NAME:
        <section>: <argument>
    """
    And SmkSectionUnexpectedCallableArgsInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | rule_like   | section   | argument |
      | rule        | input     | foo      |
      | rule        | threads   | bar(1)   |
      | rule        | params    | MSG      |
      | checkpoint  | resources | foo      |
      | checkpoint  | version   | bar(1.0) |
      | subworkflow | snakefile | MSG      |
      | subworkflow | workdir   | foo      |
