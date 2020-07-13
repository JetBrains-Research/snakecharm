Feature: Inspection for redundant coma in the end of the line

  Scenario Outline: Redundant coma in the end of the line
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      <section>: "input.txt", # redundant coma
    """
    And SmkRedundantComaInspection inspection is enabled
    Then I expect inspection warning on <,> in <<section>: "input.txt", # redundant coma> with message
    """
    Coma is unnecessary
    """
    When I check highlighting warnings
    Examples:
      | rule_like   | section    |
      | rule        | input      |
      | rule        | params     |
      | checkpoint  | input      |
      | subworkflow | workdir    |
      | subworkflow | snakefile  |
      | subworkflow | configfile |

  Scenario Outline: Fix redundant coma in the end of the line
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      <section>: <content>, # redundant coma
    """
    And SmkRedundantComaInspection inspection is enabled
    Then I expect inspection warning on <,> in <, # redundant coma> with message
    """
    Coma is unnecessary
    """
    When I check highlighting warnings
    Then I invoke quick fix Remove redundant coma and see text:
     """
    <rule_like> <signature>:
       <section>: <content>
    """
    Examples:
      | rule_like   | section    | content                    |
      | rule        | input      | "input1.txt"               |
      | rule        | params     | "input1.txt"               |
      | checkpoint  | input      | "input1.txt"               |
      | rule        | input      | "input1.txt", "input2.txt" |
      | rule        | params     | "input1.txt", "input2.txt" |
      | checkpoint  | input      | "input1.txt", "input2.txt" |
      | subworkflow | workdir    | "/"                        |
      | subworkflow | snakefile  | "/Snakefile"               |
      | subworkflow | configfile | "/custom_configfile.yaml"  |

  Scenario: Check no inspection in python code in dict line
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    dict(a=0,
         b=1,
         c=2,
         )
    """
    And SmkRedundantComaInspection inspection is enabled
    When I expect no inspection warnings

  Scenario: Check no inspection in python code in call function line
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    def foo(a, b):
        pass

    foo(a=1,
        b=2,
        )
    """
    And SmkRedundantComaInspection inspection is enabled
    When I expect no inspection warnings