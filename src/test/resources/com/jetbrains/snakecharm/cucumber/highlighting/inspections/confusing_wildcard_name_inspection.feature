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
    Then I expect no inspection warning
    When I check highlighting warnings
    Examples:
      | rule_like  | section    |
      | rule       | shell      |
      | rule       | conda      |
      | rule       | message    |
      | checkpoint | shell      |


  Scenario: No confusion if wildcard looks like qualified name
      Given a snakemake project
      Given I open a file "foo.smk" with text
          """
          rule:
              output: "{input}" #1
              params:
                  "{input.smth}", "{foo.input.smth}"
          """
      And SmkSLWildcardNameIsConfusingInspection inspection is enabled
      # Only one warning
      Then I expect inspection warning on <input> in <"{input}" #1> with message
        """
        Confusing wildcard name: 'input'. It looks like section name.
        """

      Then I expect no inspection warning
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
    Then I expect no inspection warning
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
    And I invoke quick fix Rename element and see text:
    """
    rule foo:
      output: "{INPUT}.foo"
      message: "{input} {wildcards.INPUT}.foo"
      params: "{INPUT}.foo"
    """
