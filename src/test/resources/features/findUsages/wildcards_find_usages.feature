Feature: Find Usages for wildcards

  Scenario Outline: Usages for wildcard keyword
    Given a snakemake project
    Given I open a file "foo.smk" with text
        """
        <rule_like> foo:
          output: "{sample}"
          message: "{wildcards.sample}"
          shell: "{wildcards[sample]}" # here
          run:
            p1 = wildcards.sample
            p2 = wildcards['sample']
        """
    When I put the caret at wildcards[sample]}" # here
    And I invoke find usages
    Then find usages shows me following references:
      | file    | offset    | length   |
      | foo.smk | <offset1> | <length> |
      | foo.smk | <offset2> | <length> |
      | foo.smk | <offset3> | <length> |
      | foo.smk | <offset4> | <length> |
    Examples:
      | rule_like  | offset1 | offset2 | offset3 | offset4 | length |
      | rule       | 44      | 74      | 117     | 143     | 9      |
      | checkpoint | 50      | 80      | 123     | 149     | 9      |

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

        use rule foo as foo2 with:
          log: "{sample}"
        """
    When I put the caret at sample}" # here
    And I invoke find usages
    Then find usages shows me following references:
      | file    | offset    | length |
      | foo.smk | <offset1> | 6      |
      | foo.smk | <offset2> | 16     |
      | foo.smk | <offset3> | 16     |
      | foo.smk | <offset4> | 16     |
      | foo.smk | <offset5> |6       |
    Examples:
      | rule_like  | offset1 | offset2 | offset3 | offset4 | offset5 |
      | rule       | 22      | 44      | 74      | 116     | 170     |
      | checkpoint | 28      | 50      | 80      | 122     | 176     |