Feature: Find Usages for wildcards

  Scenario Outline: Usages for wildcard keyword
    Given a snakemake project
    Given I open a file "foo.smk" with text
        """
        <rule_like> foo:
          output: "{sample}"
          message: "{wildcards.sample}"
          shell: "{wildcards.sample}" # here
          run:
            p1 = wildcards.sample
        """
    When I put the caret at wildcards.sample}" # here
    And I invoke find usages
    Then find usages shows me following references:
      | file    | offset    | length   |
      | foo.smk | <offset1> | <length> |
      | foo.smk | <offset2> | <length> |
      | foo.smk | <offset3> | <length> |
    Examples:
      | rule_like  | offset1 | offset2 | offset3 | length |
      | rule       | 44      | 74      |    116     | 9      |
      | checkpoint | 50      | 80      |    122     | 9      |

  Scenario Outline: Usages for wildcard names
    Given a snakemake project
    Given I open a file "foo.smk" with text
        """
        <rule_like> foo:
          output: "{sample}"
          message: "{wildcards.sample}"
          shell: "{wildcards.sample}" # here
          run:
            p1 = wildcards.sample
        """
    When I put the caret at sample}" # here
    And I invoke find usages
    Then find usages shows me following references:
      | file    | offset    | length |
      | foo.smk | <offset1> | 6      |
      | foo.smk | <offset2> | 16     |
      | foo.smk | <offset3> | 16     |
      | foo.smk | <offset4> | 16     |
    Examples:
      | rule_like  | offset1 | offset2 | offset3 | offset4 |
      | rule       | 22      | 44      | 74      | 116     |
      | checkpoint | 28      | 50      | 80      | 122     |