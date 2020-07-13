Feature: Inspection for redundant coma in the end of the line

  Scenario Outline: Redundant coma in the end of the line
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      <section>: "input.txt", <comment>
    """
    And SmkRedundantCommaInspection inspection is enabled
    Then I expect inspection warning on <,> in <<section>: "input.txt", <comment>> with message
    """
    Coma is unnecessary
    """
    When I check highlighting warnings
    Examples:
      | rule_like   | section    | comment           |
      | rule        | input      |                   |
      | rule        | params     |                   |
      | checkpoint  | input      |                   |
      | subworkflow | workdir    |                   |
      | subworkflow | snakefile  |                   |
      | subworkflow | configfile |                   |
      | rule        | input      | # redundant comma |
      | rule        | params     | # redundant comma |
      | checkpoint  | input      | # redundant comma |
      | subworkflow | workdir    | # redundant comma |
      | subworkflow | snakefile  | # redundant comma |
      | subworkflow | configfile | # redundant comma |

  Scenario Outline: Fix redundant coma in the end of the line
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      <section>: <content>, <comment>
    """
    And SmkRedundantCommaInspection inspection is enabled
    Then I expect inspection warning on <,> in <<section>: <content>, <comment>> with message
    """
    Coma is unnecessary
    """
    When I check highlighting warnings
    Then I invoke quick fix Remove redundant coma and see text:
     """
    <rule_like> NAME:
       <section>: <content>  <comment>
    """
    Examples:
      | rule_like   | section    | content                    | comment           |
      | rule        | input      | "input1.txt"               |                   |
      | rule        | params     | "input1.txt"               |                   |
      | checkpoint  | input      | "input1.txt"               |                   |
      | rule        | input      | "input1.txt", "input2.txt" |                   |
      | rule        | params     | "input1.txt", "input2.txt" |                   |
      | checkpoint  | input      | "input1.txt", "input2.txt" |                   |
      | subworkflow | workdir    | "/"                        |                   |
      | subworkflow | snakefile  | "/Snakefile"               |                   |
      | subworkflow | configfile | "/custom_configfile.yaml"  |                   |
      | rule        | input      | "input1.txt"               | # redundant comma |
      | rule        | params     | "input1.txt"               | # redundant comma |
      | checkpoint  | input      | "input1.txt"               | # redundant comma |
      | rule        | input      | "input1.txt", "input2.txt" | # redundant comma |
      | rule        | params     | "input1.txt", "input2.txt" | # redundant comma |
      | checkpoint  | input      | "input1.txt", "input2.txt" | # redundant comma |
      | subworkflow | workdir    | "/"                        | # redundant comma |
      | subworkflow | snakefile  | "/Snakefile"               | # redundant comma |
      | subworkflow | configfile | "/custom_configfile.yaml"  | # redundant comma |

  Scenario: Check no inspection in python code in dict line
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    dict(a=0,
         b=1,
         c=2, # comment
         )
    """
    And SmkRedundantCommaInspection inspection is enabled
    When I expect no inspection warnings

  Scenario: Check no inspection in python code in call function line
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    def foo(a, b):
        pass

    foo(a=1,
        b=2, # comment
        )
    """
    And SmkRedundantCommaInspection inspection is enabled
    When I expect no inspection warnings