Feature: Inspection for redundant coma in the end of the line

  Scenario Outline: Redundant coma in the end of the line
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      <section>: "input.txt",
    """
    And SmkRedundantComaInspection inspection is enabled
    Then I expect inspection warning on <,> in <<section>: "input.txt",> with message
    """
    Coma is unnecessary
    """
    When I check highlighting warnings
    Examples:
      | rule_like  | section |
      | rule       | input   |
      | rule       | params  |
      | checkpoint | input   |

  Scenario Outline: Fix redundant coma in the end of the line with one parameter
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      <section>: "input.txt",
    """
    And SmkRedundantComaInspection inspection is enabled
    Then I expect inspection warning on <,> in <<section>: "input.txt",> with message
    """
    Coma is unnecessary
    """
    When I check highlighting warnings
    Then I invoke quick fix Remove redundant coma and see text:
     """
    <rule_like> NAME:
       <section>: "input.txt"
    """
    Examples:
      | rule_like  | section |
      | rule       | input   |
      | rule       | params  |
      | checkpoint | input   |

  Scenario Outline: Fix redundant coma in the end of the line with two parameters
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      <section>: "input1.txt", "input2.txt",
    """
    And SmkRedundantComaInspection inspection is enabled
    Then I expect inspection warning on <,> in <"input2.txt",> with message
    """
    Coma is unnecessary
    """
    When I check highlighting warnings
    Then I invoke quick fix Remove redundant coma and see text:
     """
    <rule_like> NAME:
       <section>: "input1.txt", "input2.txt"
    """
    Examples:
      | rule_like  | section |
      | rule       | input   |
      | rule       | params  |
      | checkpoint | input   |

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
    Then I expect no inspection warning

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
    Then I expect no inspection warning