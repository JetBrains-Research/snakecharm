Feature: Rule section redeclaration inspection
  Scenario Outline: No section redeclarations
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input: "input.txt"
        output: "output.txt"
        params: a="value"
        shell: "command {params.a}"
    """
    And Section Redeclaration inspection is enabled
    Then I expect no inspection error
    And I expect no inspection warning
    When I check highlighting errors
  Examples:
    | rule_like  |
    | checkpoint |
    | rule       |

  Scenario Outline: Single section redeclaration
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input: "input.txt"
        output: "output.txt"
        params: a="value", b="b_value"
        params: c="c_value", a="value"
        shell: "command {params.a}"
    """
    And Section Redeclaration inspection is enabled
    Then I expect inspection warning on <params: c="c_value", a="value"> with message
    """
    Declaration of section 'params' above overrides this declaration.
    """
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Multiple section redeclarations
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input: "input.txt"
        output: "output.txt"
        params: a="value", b="b_value"
        params: c="c_value", a="value"
        resources: threads=4
        threads: 4
        params: b="value"
        shell: "command {params.a}"
    """
    And Section Redeclaration inspection is enabled
    Then I expect inspection warning on <params: c="c_value", a="value"> with message
    """
    Declaration of section 'params' above overrides this declaration.
    """
    And I expect inspection warning on <params: b="value"> with message
    """
    Declaration of section 'params' above overrides this declaration.
    """
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario: Subworkflow section redeclaration
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    subworkflow NAME:
      snakefile: "foo.smk"
      snakefile: "boo.smk"
    """
    And Section Redeclaration inspection is enabled
    Then I expect inspection warning on <snakefile: "boo.smk"> with message
    """
    Declaration of section 'snakefile' above overrides this declaration.
    """
    When I check highlighting errors

  Scenario Outline: Section redeclaration element removal fix test
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> name1:
      input: "input1"
      input: "input2"
      output: "output.txt"
    """
    And Section Redeclaration inspection is enabled
    Then I check highlighting errors
    And I invoke quick fix Remove section and see text:
    """
    <rule_like> name1:
      input: "input1"
      output: "output.txt"
    """
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Section redeclaration rename fix test
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> name1:
      input: "input1"
      input: "input2"
      output: "output.txt"
    """
    And Section Redeclaration inspection is enabled
    Then I check highlighting errors
    And I invoke quick fix Rename element and see text:
    """
    <rule_like> name1:
      input: "input1"
      SNAKEMAKE_IDENTIFIER: "input2"
      output: "output.txt"
    """
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |
