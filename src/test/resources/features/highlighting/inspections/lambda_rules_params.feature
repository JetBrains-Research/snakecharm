Feature: Inspection checking lambda parameter names in various sections

  Scenario Outline: Correct lambda parameters in section
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <rule_like> NAME:
        <section>:
          <lambda>
        output:
          "somedir/{sample}.csv"
      """
    And SmkLambdaRuleParamsInspection inspection is enabled
    Then I expect no inspection weak warnings
    When I check highlighting weak warnings
    Examples:
      | rule_like  | section   | lambda                                                |
      | rule       | input     | lambda wildcards: ""                                  |
      | rule       | group     | lambda wildcards: config["samples"][wildcards.sample] |
      | rule       | params    | p=lambda wildcards, output: ""                        |
      | rule       | threads   | lambda wildcards, input, attempt: 10                  |
      | rule       | resources | p=lambda wildcards, input, threads, attempt: 1        |
      | rule       | conda     | lambda wildcards, params, input: ""                   |
      | checkpoint | threads   | lambda wildcards, input, attempt: 10                  |

  Scenario Outline: Too many lambda parameters in section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      <section>:
        <lambda>
    """
    And SmkLambdaRuleParamsInspection inspection is enabled
    Then I expect inspection error on <<error_place>> with messages
      | Don't use more than <n> lambda parameter(s) in '<section>' section.       |
      | Only use 'wildcards<allowed>' as lambda parameter in '<section>' section. |
    When I check highlighting weak warnings
    Examples:
      | rule_like  | section   | lambda                                                         | error_place | n | allowed                         |
      | rule       | input     | lambda wildcards, aaa: ""                                      | aaa         | 1 |                                 |
      | rule       | group     | lambda wildcards, aaa: ""                                      | aaa         | 1 |                                 |
      | rule       | params    | p=lambda wildcards, output, input, resources, threads, log: "" | log         | 5 | /input/output/resources/threads |
      | rule       | threads   | lambda wildcards, input, attempt, params: 1                    | params      | 3 | /input/attempt                  |
      | rule       | conda     | lambda wildcards, params, input, aaa: ""                       | aaa         | 3 | /params/input                   |
      | rule       | resources | p=lambda wildcards, input, threads, attempt, aaa: ""           | aaa         | 4 | /input/threads/attempt          |
      | checkpoint | threads   | lambda wildcards, input, attempt, params: 1                    | params      | 3 | /input/attempt                  |

  Scenario Outline: Only used supported lambda parameters in section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      input: "input.txt"
      <section>:
        <lambda>
      output:
        "somedir/{sample}.csv"
      shell:
        "somecommand -o {params.prefix}"
    """
    And SmkLambdaRuleParamsInspection inspection is enabled
    Then I expect inspection error on <<error_place>> with message
    """
    Only use 'wildcards/<allowed>' as lambda parameter in '<section>' section.
    """
    When I check highlighting weak warnings
    Examples:
      | rule_like  | section   | lambda                                                            | error_place | allowed                        |
      | rule       | params    | p=lambda wildcards, output, input, resources, log: output[0][:-4] | log         | input/output/resources/threads |
      | rule       | resources | p=lambda wildcards, input, threads, params: threads * 100         | params      | input/threads/attempt          |
      | rule       | conda     | lambda wildcards, aaa: 10                                         | aaa         | params/input                   |
      | checkpoint | params    | p=lambda wildcards, output, input, resources, log: output[0][:-4] | log         | input/output/resources/threads |

  Scenario Outline: Wildcards param should be first in lambda
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <rule_like> NAME:
        input: "input.txt"
        <section>:
          lambda <section_param>, wildcards: <lambda_body>
      """
    And SmkLambdaRuleParamsInspection inspection is enabled
    Then I expect inspection error on <wildcards> with message
      """
      'wildcards' has to be the first lambda parameter.
      """
    And I expect inspection error on <<section_param>> in <lambda <section_param>> with message
      """
      '<section_param>' cannot be the first lambda parameter in '<section>' section.
      """
    When I check highlighting weak warnings
    Examples:
      | rule_like  | section   | section_param | lambda_body    |
      | rule       | params    | output        | output[0][:-4] |
      | rule       | threads   | input         | 10             |
      | rule       | resources | input         | 10             |
      | rule       | conda     | input         | 10             |
      | checkpoint | conda     | input         | 10             |

  Scenario Outline: lambda functions not allowed in section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      <section>: lambda wildcards: wildcards
    """
    And SmkLambdaRuleParamsInspection inspection is enabled
    Then I expect inspection error on <lambda wildcards: wildcards> with message
    """
    Function object cannot be used as a value in '<section>' section.
    """
    When I check highlighting weak warnings
    Examples:
      | section              | rule_like  |
      | benchmark            | rule       |
      | output               | rule       |
      | log                  | rule       |
      | singularity          | rule       |
      | priority             | rule       |
      | wildcard_constraints | rule       |
      | shell                | rule       |
      | wrapper              | rule       |
      | script               | rule       |
      | cwl                  | rule       |
      | version              | rule       |
      | benchmark            | checkpoint |
      | version              | checkpoint |

  Scenario Outline: First params is preferable to be wildcards
    Given a snakemake project
    Given I open a file "foo.smk" with text
        """
        <rule_like> NAME:
          <section>:
            lambda aaa: aaa
          output:
            "output.txt"
        """
    And SmkLambdaRuleParamsInspection inspection is enabled
    Then I expect inspection weak warning on <aaa> with message
        """
        Snakemake documentation suggests it's preferable to name the first parameter 'wildcards'.
        """
    When I check highlighting weak warnings
    Examples:
      | rule_like  | section   |
      | rule       | input     |
      | rule       | group     |
      | rule       | params    |
      | rule       | threads   |
      | rule       | resources |
      | rule       | conda     |
      | checkpoint | input     |

  Scenario Outline: lambda invocations in sections where lambdas are not allowed
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      <section>: (lambda wildcards: wildcards)("")
    """
    And SmkLambdaRuleParamsInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting weak warnings
    Examples:
      | section              | rule_like  |
      | benchmark            | rule       |
      | conda                | rule       |
      | output               | rule       |
      | log                  | rule       |
      | singularity          | rule       |
      | priority             | rule       |
      | wildcard_constraints | rule       |
      | shell                | rule       |
      | wrapper              | rule       |
      | script               | rule       |
      | cwl                  | rule       |
      | version              | rule       |
      | benchmark            | checkpoint |
      | conda                | checkpoint |
      | output               | checkpoint |
      | log                  | checkpoint |
      | singularity          | checkpoint |
      | priority             | checkpoint |
      | wildcard_constraints | checkpoint |
      | shell                | checkpoint |
      | wrapper              | checkpoint |
      | script               | checkpoint |
      | cwl                  | checkpoint |
      | version              | checkpoint |

  Scenario Outline: rename lambda parameter quick fix
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      input: lambda a: a + a.foo
    """
    And SmkLambdaRuleParamsInspection inspection is enabled
    Then I expect inspection weak warning on <a> in <a:> with message
    """
    Snakemake documentation suggests it's preferable to name the first parameter 'wildcards'.
    """
    When I check highlighting weak warnings
    And I invoke quick fix Rename to 'wildcards' and see text:
    """
    <rule_like> foo:
      input: lambda wildcards: wildcards + wildcards.foo
    """
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |
