Feature: Rule SmkSectionRedeclarationInspection inspection
  Scenario Outline: No SmkSectionRedeclarationInspections
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input: "input.txt"
        output: "output.txt"
        params: a="value"
        shell: "command {params.a}"
    """
    And SmkSectionRedeclarationInspection inspection is enabled
    And I expect no inspection weak warnings
    When I check highlighting weak warnings
  Examples:
    | rule_like  |
    | checkpoint |
    | rule       |

  Scenario Outline: Single SmkSectionRedeclarationInspection
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
    And SmkSectionRedeclarationInspection inspection is enabled
    Then I expect inspection weak warning on <params: c="c_value", a="value"> with message
    """
    Declaration of section 'params' above overrides this declaration.
    """
    When I check highlighting weak warnings
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Multiple SmkSectionRedeclarationInspections
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
    And SmkSectionRedeclarationInspection inspection is enabled
    Then I expect inspection weak warning on <params: c="c_value", a="value"> with message
    """
    Declaration of section 'params' above overrides this declaration.
    """
    And I expect inspection weak warning on <params: b="value"> with message
    """
    Declaration of section 'params' above overrides this declaration.
    """
    When I check highlighting weak warnings
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Subworkflow and Module SmkSectionRedeclarationInspection
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <header> NAME:
      snakefile: "foo.smk"
      snakefile: "boo.smk"
    """
    And SmkSectionRedeclarationInspection inspection is enabled
    Then I expect inspection weak warning on <snakefile: "boo.smk"> with message
    """
    Declaration of section 'snakefile' above overrides this declaration.
    """
    When I check highlighting weak warnings
    Examples:
      | header      |
      | subworkflow |
      | module      |

  Scenario: SmkSectionRedeclarationInspection for 'use' section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    use rule a from module as b with:
      input: "some_file"
      input: "other_file"
    """
    And SmkSectionRedeclarationInspection inspection is enabled
    Then I expect inspection weak warning on <input: "other_file"> with message
    """
    Declaration of section 'input' above overrides this declaration.
    """
    When I check highlighting weak warnings
    
  Scenario Outline: SmkSectionRedeclarationInspection element removal fix test
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> name1:
      input: "input1"
      input: "input2"
      output: "output.txt"
    """
    And SmkSectionRedeclarationInspection inspection is enabled
    Then I expect inspection weak warning on <input: "input2"> with message
    """
    Declaration of section 'input' above overrides this declaration.
    """
    Then I check highlighting weak warnings
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

  Scenario Outline: SmkSectionRedeclarationInspection rename fix test
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> name1:
      input: "input1"
      input: "input2"
      output: "output.txt"
    """
    And SmkSectionRedeclarationInspection inspection is enabled
    Then I expect inspection weak warning on <input: "input2"> with message
    """
    Declaration of section 'input' above overrides this declaration.
    """
    When I check highlighting weak warnings
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