Feature: Inspection for unexpected callable arguments in rulelike sections

  Scenario Outline: Report Unexpected callable arguments in mock YAML API
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "3.0.0"
        override:
        - name: "foobooo"
          type: "rule-like"
          lambda_args:
            - "wildcards"

      - version: "2.0.0"
        introduced:
        - name: "foobooo"
          type: "rule-like"
    """
    And I set snakemake language version to "<lang_version>"
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
    Section '<section>' does not support callable arguments in Snakemake '<lang_version>'.
    """
    When I check highlighting errors
    Examples:
      | lang_version | rule_like  | section |
      | 2.0.0        | rule       | foobooo |
      | 2.0.0        | checkpoint | foobooo |

  Scenario Outline: Report Unexpected callable arguments in latest language level
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
    Section '<section>' does not support callable arguments in Snakemake 'CURR_SMK_LANG_VERS'.
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

  Scenario Outline: No warn on expected callable arguments in mock YAML API
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "3.0.0"
        override:
        - name: "foobooo"
          type: "rule-like"
          lambda_args:
            - "wildcards"

      - version: "2.0.0"
        introduced:
        - name: "foobooo"
          type: "rule-like"
    """
    And I set snakemake language version to "<lang_version>"
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
      | lang_version | rule_like  | section |
      | 3.0.0        | rule       | foobooo |
      | 3.0.0        | checkpoint | foobooo |

  Scenario Outline: No warn on expected callable arguments in latest language level
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
      | rule       | conda     |
      | rule       | resources |
      | checkpoint | resources |
      | checkpoint | group     |

  Scenario Outline: No warn on other PyReferenceExpression arguments in mock YAML API
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "3.0.0"
        override:
        - name: "foobooo"
          type: "rule-like"
          lambda_args:
            - "wildcards"

      - version: "2.0.0"
        introduced:
        - name: "foobooo"
          type: "rule-like"
    """
    And I set snakemake language version to "<lang_version>"
    Given I open a file "foo.smk" with text
    """
    def foo():
        return "text"
    def bar(arg):
        return str(arg)
    MSG="msg"

    <rule_like> NAME:
        foobooo: <argument>
    """
    And SmkSectionUnexpectedCallableArgsInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | lang_version | rule_like  | argument |
      | 2.0.0        | rule       | MSG      |
      | 2.0.0        | rule       | foo()    |
      | 2.0.0        | rule       | bar(1)   |
      | 2.0.0        | checkpoint | MSG      |
      | 2.0.0        | checkpoint | foo()    |
      | 2.0.0        | checkpoint | bar(0)   |

  Scenario Outline: No warn on other PyReferenceExpression arguments in latest language level
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
