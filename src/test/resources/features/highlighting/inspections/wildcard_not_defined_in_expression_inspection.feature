Feature: Inspection for defining wildcard in expression

  Scenario Outline: Defining incorrect wildcard in expression
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule foo1:
        output:
            out1 = "/path/{sample}_{genome}.1",
            out2 = "/path/{sample}_{genome}.2"

    rule foo2:
        output:
            "/path/{sample}.txt"

    rule foo3:
        output:  "/path/{genome}.txt",

    <rule_like> NAME:
        output: "{sample}.txt"
        <section>: <expression_first_part><expression_second_part>
    """
    And SmkWildcardNotDefinedInExpressionInspection inspection is enabled
    Then I expect inspection error on <<expression_second_part>> in <<section>: <expression_first_part><expression_second_part>> with message
    """
    Wildcard 'genome' isn't properly defined.
    """
    When I check highlighting errors
    Examples:
      | rule_like   | section    | expression_first_part  | expression_second_part |
      | rule        | input      | rules.foo1.output.     | out1                   |
      | rule        | input      | rules.foo3.            | output                 |
      | rule        | params     | rules.foo1.output.     | out1                   |
      | rule        | params     | rules.foo3.            | output                 |
      | checkpoint  | input      | rules.foo1.output.     | out1                   |
      | checkpoint  | input      | rules.foo3.            | output                 |
      | checkpoint  | params     | rules.foo1.output.     | out1                   |
      | checkpoint  | params     | rules.foo3.            | output                 |

  Scenario Outline: Defining correct wildcard in expression
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule foo1:
        output:
            out1 = "/path/{sample}_{genome}.1",
            out2 = "/path/{sample}_{genome}.2"

    rule foo2:
        output:
            "/path/{sample}.txt"

    rule foo3:
        output:  "/path/{genome}.txt",

    <rule_like> NAME:
        output: "{sample}.txt"
        <section>: <expression_first_part>
    """
    And SmkWildcardNotDefinedInExpressionInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | rule_like   | section    | expression_first_part          |
      | rule        | input      | rules.foo2.output              |
      | rule        | input      | expand(rules.foo1.output.out2) |
      | checkpoint  | input      | rules.foo2.output              |
      | checkpoint  | input      | expand(rules.foo1.output.out2) |