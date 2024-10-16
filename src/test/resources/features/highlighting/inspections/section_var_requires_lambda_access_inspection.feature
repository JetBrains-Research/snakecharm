Feature: Rule section access requires lambda

  #TODO: If lambda args need to be changed, please refactor all test here & move cases into mock Snakemake API YAML file

  Scenario Outline: Variable name matches declared section and not hidden by outer variable, configured in YAML
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "3.0.0"
        override:
        - name: "foobooo"
          type: "rule-like"
          lambda_args:
            - "wildcards"
            - "section_10"
            - "section_11"

      - version: "2.0.0"
        introduced:
        - name: "foobooo"
          type: "rule-like"
          lambda_args:
            - "wildcards"
            - "section_2"
            - "section_3"
    """
    And I set snakemake language version to "<lang_version>"
    Given I open a file "foo.smk" with text
        """
        rule all:
          <var_name>: <ref_section_content>
          foobooo: <section_context>
          shell: "echo {<var_name>} {params.p}"
        """
    And SmkSectionVariableRequiresLambdaAccessInspection inspection is enabled
    #noinspection CucumberUndefinedStep
    Then I expect inspection error on <<var_name>> in <foobooo: <section_context>> with message
       """
       To access '<var_name>' section use lambda here, e.g. `lambda wildcards, <var_name>: <var_name>.foo`.
       """
    # Weak warning check includes error check, use `weak warning` because our inspection returns both types
    When I check highlighting weak warnings
    Examples:
      | lang_version | section_context                 | var_name   | ref_section_content |
      | 2.0.0        | p=section_2                     | section_2  | f="s"               |
      | 2.0.0        | p=section_3                     | section_3  | 4                   |
      | 2.0.0        | p=section_3                     | section_3  | f=1                 |
      | 3.0.0        | p=section_10                    | section_10 | f=1                 |
      | 3.0.0        | p=section_10[0]                 | section_10 | f=1                 |
      | 3.0.0        | p=section_10.f                  | section_10 | f=1                 |
      | 3.0.0        | p=section_10.f.boo              | section_10 | f=1                 |
      | 2.0.0        | p=lambda wildcards: section_2   | section_2  | f=1                 |
      | 2.0.0        | p=lambda wildcards: section_2.f | section_2  | f=1                 |
      | 2.0.0        | p=lambda wildcards: section_3.f | section_3  | f=1                 |

  Scenario Outline: Variable name matches declared section and not hidden by outer variable in latest language level
    Given a snakemake project
    Given I open a file "foo.smk" with text
        """
        rule all:
          <var_name>: <ref_section_content>
          <section_context>
          shell: "echo {<var_name>} {params.p}"
        """
    And SmkSectionVariableRequiresLambdaAccessInspection inspection is enabled
    #noinspection CucumberUndefinedStep
    Then I expect inspection error on <<var_name>> in <<section_context>> with message
       """
       To access '<var_name>' section use lambda here, e.g. `lambda wildcards, <var_name>: <var_name>.foo`.
       """
    # Weak warning check includes error check, use `weak warning` because our inspection returns both types
    When I check highlighting weak warnings
    Examples:
      | section_context                      | var_name  | ref_section_content |
      | params: p=input                      | input     | f="s"               |
      | params: p=resources                  | resources | f=1                 |
      | params: p=lambda wildcards: input[1] | input     | f="s"               |
      | conda: lambda wildcards: params.f    | params    | f="s"               |

  Scenario Outline: Variable name matches undeclared section and not hidden by outer variable in latest language level
    Given a snakemake project
    Given I open a file "foo.smk" with text
          """
          rule all:
            <section_context>
            shell: "echo {<var_name>} {params.p}"
          """
    And SmkSectionVariableRequiresLambdaAccessInspection inspection is enabled
    #noinspection CucumberUndefinedStep
    Then I expect inspection error on <<var_name>> in <<section_context>> with message
         """
         To access '<var_name>' section use lambda here, e.g. `lambda wildcards, <var_name>: <var_name>.foo`.
         """
    # Weak warning check includes error check, use `weak warning` because our inspection returns both types
    When I check highlighting weak warnings
    Examples:
      | section_context                    | var_name  |
      | params: p=output                   | output    |
      | params: p=threads                  | threads   |
      | params: p=resources                | resources |
      | resources: p=threads               | threads   |
      | params: p=lambda wildcards: output | output    |
      | conda: params                      | params    |

  Scenario Outline: Variable name matches undeclared section and hidden by outer variable in latest language level
    Given a snakemake project
    Given I open a file "foo.smk" with text
          """
          <outer_context>
          rule all:
            <section_context>
            shell: "echo {<var_name>} {params.p}"
          """
    And SmkSectionVariableRequiresLambdaAccessInspection inspection is enabled
    #noinspection CucumberUndefinedStep
    Then I expect inspection weak warning on <<var_name>> in <<section_context>> with message
         """
         Access to outer scope '<var_name>' object with the name similar to snakemake specific variable. To use snakemake '<var_name>' variable, use lambda expression here, e.g. `lambda wildcards, <var_name>: <var_name>.foo`.
         """
    # Weak warning check includes error check, use `weak warning` because our inspection returns both types
    When I check highlighting weak warnings

    Examples:
      | section_context                    | var_name  | outer_context |
      | params: p=input                    | input     |               |
      | params: p=output                   | output    | output = 2    |
      | params: p=threads                  | threads   | threads = 2   |
      | params: p=resources                | resources | resources = 2 |
      | resources: p=input                 | input     |               |
      | resources: p=threads               | threads   | threads = 2   |
      | threads: p=input                   | input     |               |
      | params: p=lambda wildcards: input  | input     |               |
      | params: p=lambda wildcards: output | output    | output = 2    |
      | conda: params                      | params    | params = 2    |

  Scenario Outline: Variable name matches declared section and referenced section is hidden by outer variable (case1) in latest language level
    Given a snakemake project
    Given I open a file "foo.smk" with text
        """
        <var_name> = 2
        rule all:
          <ref_section>
          <section_context>
          shell: "echo {<var_name>} {params.p}"
        """
    And SmkSectionVariableRequiresLambdaAccessInspection inspection is enabled
    # XXX Due to current behavior of resolve when section is in rule, `ref_section_name` is considered as unresolved
    # XXX it isn't correct, and here is desired warning that Possible a rule is.
    #noinspection CucumberUndefinedStep
    Then I expect inspection error on <<var_name>> in <<section_context>> with message
       """
       To access '<var_name>' section use lambda here, e.g. `lambda wildcards, <var_name>: <var_name>.foo`.
       """
    # Weak warning check includes error check, use `weak warning` because our inspection returns both types
    When I check highlighting weak warnings
    Examples:
      | section_context      | var_name  | ref_section    |
      | params: p=input      | input     | input: f=""    |
      | params: p=output     | output    | output: f=""   |
      | params: p=threads    | threads   | threads: 2     |
      | params: p=resources  | resources | resources: f=2 |
      | resources: p=input   | input     | input: f=""    |
      | resources: p=threads | threads   | threads: 2     |
      | threads: p=input     | input     | input: f=""    |
      | conda: params        | params  | params: f=""   |

  Scenario Outline: Variable name matches declared section and referenced section is hidden by outer variable (case 2) in latest language level
    Given a snakemake project
    Given I open a file "foo.smk" with text
        """
        <var_name> = 2
        rule all:
          <ref_section>
          <section_context>
          shell: "echo {<var_name>} {params.p}"
        """
    And SmkSectionVariableRequiresLambdaAccessInspection inspection is enabled
    #noinspection CucumberUndefinedStep
    Then I expect inspection error on <<var_name>> in <<section_context>> with message
       """
       Access to outer scope '<var_name>' object with the name similar to snakemake specific variable. To use snakemake '<var_name>' variable, use lambda expression here, e.g. `lambda wildcards, <var_name>: <var_name>.foo`.
       """
    # Weak warning check includes error check, use `weak warning` because our inspection returns both types
    When I check highlighting weak warnings
    Examples:
      | section_context                    | var_name | ref_section  |
      | params: p=lambda wildcards: input  | input    | input: f=""  |
      | params: p=lambda wildcards: output | output   | output: f="" |
      | conda: p=lambda wildcards: params  | params   | params: f="" |

  Scenario Outline: Variable not matches section and not hidden by outer variable in latest language level
    Given a snakemake project
    Given I open a file "foo.smk" with text
          """
          rule all:
            <section_context>
            shell: "echo {<var_name>} {params.p}"
          """
    And SmkSectionVariableRequiresLambdaAccessInspection inspection is enabled
    #noinspection CucumberUndefinedStep
    Then I expect inspection error on <<var_name>> in <<section_context>> with message
         """
         To access '<var_name>' section use lambda here, e.g. `<lambda_example_prefix> <var_name>: <var_name>.foo`.
         """
    # Weak warning check includes error check, use `weak warning` because our inspection returns both types
    When I check highlighting weak warnings

    Examples:
      | section_context                      | var_name  | lambda_example_prefix |
      | input: p=wildcards                   | wildcards | lambda                |
      | group: p=wildcards                   | wildcards | lambda                |
      | params: p=wildcards                  | wildcards | lambda                |
      | resources: p=wildcards               | wildcards | lambda                |
      | resources: p=attempt                 | attempt   | lambda wildcards,     |
      | threads: p=wildcards                 | wildcards | lambda                |
      | threads: p=attempt                   | attempt   | lambda wildcards,     |
      | threads: p=lambda wildcards: attempt | attempt   | lambda wildcards,     |
      | conda: wildcards                     | wildcards | lambda                |
      | conda: params                        | params    | lambda wildcards,     |
      | conda: lambda wildcards: params      | params    | lambda wildcards,     |

  Scenario Outline: Variable not matches section and hidden by outer variable in latest language level
    Given a snakemake project
    Given I open a file "foo.smk" with text
        """
        <outer_context>
        rule all:
          <section_context>
          shell: "echo {<var_name>} {params.p}"
        """
    And SmkSectionVariableRequiresLambdaAccessInspection inspection is enabled
    #noinspection CucumberUndefinedStep
    Then I expect inspection weak warning on <<var_name>> in <<section_context>> with message
       """
       Access to outer scope '<var_name>' object with the name similar to snakemake specific variable. To use snakemake '<var_name>' variable, use lambda expression here, e.g. `<lambda_example_prefix> <var_name>: <var_name>.foo`.
       """
    # Weak warning check includes error check, use `weak warning` because our inspection returns both types
    When I check highlighting weak warnings
    Examples:
      | section_context                      | var_name  | outer_context | lambda_example_prefix |
      | input: p=wildcards                   | wildcards | wildcards = 2 | lambda                |
      | group: p=wildcards                   | wildcards | wildcards = 2 | lambda                |
      | conda: wildcards                     | wildcards | wildcards = 2 | lambda                |
      | params: p=wildcards                  | wildcards | wildcards = 2 | lambda                |
      | resources: p=wildcards               | wildcards | wildcards = 2 | lambda                |
      | resources: p=attempt                 | attempt   | attempt = 2   | lambda wildcards,     |
      | threads: p=wildcards                 | wildcards | wildcards = 2 | lambda                |
      | threads: p=attempt                   | attempt   | attempt = 2   | lambda wildcards,     |
      | threads: p=lambda wildcards: attempt | attempt   | attempt = 2   | lambda wildcards,     |

  Scenario Outline: No error in latest language level
    Given a snakemake project
    Given I open a file "foo.smk" with text
          """
          <outer_context>
          rule all:
            <section_context>
            shell: "echo {<var_name>} {params.p}"
          """
    And SmkSectionVariableRequiresLambdaAccessInspection inspection is enabled
    And I expect no inspection weak warnings
    # Weak warning check includes error check, use `weak warning` because our inspection returns both types
    When I check highlighting weak warnings
    Examples:
      | section_context                                                       | var_name  | outer_context |
      | input: p=output                                                       | output    | output=1      |
      | group: p=output                                                       | output    | output=1      |
      | params: p=attempt                                                     | attempt   | attempt=1     |
      | resources: p=output                                                   | output    | output=1      |
      | threads: p=output                                                     | output    | output=1      |
      | output: p=input                                                       | input     |               |
      | output: p=input                                                       | input     | input=1       |
      | output: p=params                                                      | params    | params=1      |
      | log: p=output                                                         | output    | output=1      |
      | message: p=output                                                     | output    | output=1      |
      | conda: output                                                         | output    | output=1      |
      | input: p=f.output                                                     | output    | f = 1         |
      | input: p=lambda wildcards: wildcards                                  | wildcards |               |
      | input: p=lambda wildcards: output                                     | output    | output=1      |
      | params: p=lambda wildcards: wildcards                                 | wildcards |               |
      | params: p=lambda wildcards, output: output                            | output    |               |
      | params: p=lambda wildcards, input, output: output                     | input     |               |
      | params: p=lambda wildcards, input, output, threads: output            | threads   |               |
      | params: p=lambda wildcards, input, output, threads, resources: output | resources |               |
      | group: p=lambda wildcards: output                                     | output    | output=1      |

  Scenario Outline: Snakemake variable undefined and cannot be used in this context in latest language level
    Given a snakemake project
    Given I open a file "foo.smk" with text
          """
          rule all:
             <section_name>: <section_context>
          """
    And SmkSectionVariableRequiresLambdaAccessInspection inspection is enabled
      #noinspection CucumberUndefinedStep
    Then I expect inspection error on <<var_name>> in <<section_context>> with message
         """
         Snakemake variable '<var_name>' cannot be used in '<section_name>' directly or using lambdas.
         """
    # Weak warning check includes error check, use `weak warning` because our inspection returns both types
    When I check highlighting weak warnings

    Examples:
      | section_context            | var_name | section_name |
      | p=output                   | output   | input        |
      | p=attempt                  | attempt  | input        |
      | p=output                   | output   | group        |
      | p=attempt                  | attempt  | group        |
      | p=attempt                  | attempt  | params       |
      | p=output                   | output   | resources    |
      | p=output                   | output   | threads      |
      | p=params                   | params   | output       |
      | p=output                   | output   | log          |
      | p=lambda wildcards: output | output   | group        |
      | attempt                    | attempt  | conda        |
