Feature: Inspection warns about confusing wildcard names.

  Scenario Outline: Confusing sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
        """
        <rule_like>:
            output: "{<section>}" #1
            log: "{<section>}" #2
            input: "{<section>}" #3
            params: "{<section>}" #4
        """
    And SmkSLWildcardNameIsConfusingInspection inspection is enabled
    Then I expect inspection warning on <<section>> in <"{<section>}" #1> with message
        """
        Confusing wildcard name: '<section>'. It looks like section name.
        """
    Then I expect inspection warning on <<section>> in <"{<section>}" #2> with message
        """
        Confusing wildcard name: '<section>'. It looks like section name.
        """
    Then I expect inspection warning on <<section>> in <"{<section>}" #3> with message
        """
        Confusing wildcard name: '<section>'. It looks like section name.
        """
    Then I expect inspection warning on <<section>> in <"{<section>}" #4> with message
        """
        Confusing wildcard name: '<section>'. It looks like section name.
        """
    When I check highlighting warnings
    Examples:
      | rule_like  | section   |
      | rule       | input     |
      | rule       | output    |
      | rule       | log       |
      | rule       | threads   |
      | rule       | params    |
      | rule       | version   |
      | rule       | resources |
      | checkpoint | input     |


  Scenario Outline: No confusion if section name cannot be used as accessor
    Given a snakemake project
    Given I open a file "foo.smk" with text
        """
        <rule_like>:
            output: "{<section>}" #1
            log: "{<section>}" #2
            input: "{<section>}" #3
            params: "{<section>}" #4
            message: "{<section>}"
            shell: "{<section>}"
        """
    And SmkSLWildcardNameIsConfusingInspection inspection is enabled
    Then I expect no inspection warnings
    When I check highlighting warnings
    Examples:
      | rule_like  | section    |
      | rule       | shell      |
      | rule       | conda      |
      | rule       | message    |
      | checkpoint | shell      |


  Scenario: Confusion if wildcard looks like qualified name
    Given a snakemake project
    Given I open a file "foo.smk" with text
          """
          rule:
              output: "{input}" #1
              params:
                  "{input.smth}", "{foo.input.smth}"
          """
    And SmkSLWildcardNameIsConfusingInspection inspection is enabled
    Then I expect inspection warning on <input> in <"{input}" #1> with message
      """
      Confusing wildcard name: 'input'. It looks like section name.
      """
    Then I expect inspection error on <input.smth> in <{input.smth}> with message
      """
      Confusing wildcard name: 'input.smth', looks like call chain. Please don't use dots here.
      """
    Then I expect inspection error on <foo.input.smth> in <{foo.input.smth}> with message
      """
      Confusing wildcard name: 'foo.input.smth', looks like call chain. Please don't use dots here.
      """
    When I check highlighting warnings

  Scenario Outline: No confusion in non-wildcards sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
        """
        <rule_like>:
            message: "{<section>}"
            shell: "{<section>}"
        """
    And SmkSLWildcardNameIsConfusingInspection inspection is enabled
    Then I expect no inspection warnings
    When I check highlighting warnings
    Examples:
      | rule_like  | section   |
      | rule       | input     |
      | rule       | output    |
      | rule       | log       |
      | rule       | threads   |
      | rule       | params    |
      | rule       | version   |
      | rule       | resources |
      | checkpoint | input     |

  Scenario: rename lambda parameter quick fix
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule foo:
      output: "{input}.foo"
      message: "{input} {wildcards.input}.foo"
      params: "{input}.foo"
    """
    And SmkSLWildcardNameIsConfusingInspection inspection is enabled
    Then I expect inspection warning on <input> in <output: "{input}.foo"> with message
    """
    Confusing wildcard name: 'input'. It looks like section name.
    """
    Then I expect inspection warning on <input> in <params: "{input}.foo"> with message
    """
    Confusing wildcard name: 'input'. It looks like section name.
    """
    When I check highlighting warnings
      # PyRenameElementQuickFix
    Then I invoke quick fix Rename element and see text:
    """
    rule foo:
      output: "{INPUT}.foo"
      message: "{input} {wildcards.INPUT}.foo"
      params: "{INPUT}.foo"
    """

  Scenario Outline: Dot in wildcard name is also confusing
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like>:
        <section>:  "{wildcards.input}"
    """
    And SmkSLWildcardNameIsConfusingInspection inspection is enabled
    Then I expect inspection error on <wildcards.input> with message
    """
    Confusing wildcard name: 'wildcards.input', looks like call chain. Please don't use dots here.
    """

    When I check highlighting errors
    Examples:
      | rule_like   | section |
      | rule        | output  |
      | rule        | params  |
      | rule        | input   |
      | checkpoint  | output  |
