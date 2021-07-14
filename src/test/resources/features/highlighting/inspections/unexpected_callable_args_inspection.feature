Feature: Inspection for unexpected callable arguments in rulelike sections

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
      | rule_like  | section       |
      | rule       | benchmark     |
      | rule       | cache         |
      | rule       | output        |
      | rule       | container     |
      | rule       | containerized |
      | rule       | cwl           |
      | rule       | log           |
      | checkpoint | version       |
      | checkpoint | message       |
      | checkpoint | notebook      |
      | checkpoint | priority      |
      | checkpoint | script        |
      | checkpoint | shadow        |
      | checkpoint | shell         |
      | checkpoint | wrapper       |

  Scenario Outline: No warn on expected callable arguments
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    def foo(wildcards):
        return "text"

    <rule_like> NAME:
        <section>: foo
    """
    And SmkSectionUnexpectedCallableArgsInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | rule_like  | section   |
      | rule       | input     |
      | rule       | params    |
      | rule       | threads   |
      | checkpoint | resources |
      | checkpoint | group     |

  Scenario Outline: No warn on other PyReferenceExpression arguments
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
      | rule_like  | section   | argument |
      | rule       | message   | MSG      |
      | rule       | output    | foo()    |
      | rule       | priority  | bar(1)   |
      | checkpoint | log       | MSG      |
      | checkpoint | benchmark | foo()    |
      | checkpoint | cache     | bar(0)   |
