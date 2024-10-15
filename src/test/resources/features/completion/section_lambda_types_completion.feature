Feature: Completion for section lambda params attrs

  #TODO: If lambda args need to be changed, please refactor all test here & move cases into mock Snakemake API YAML file

  Scenario Outline: Completion attrs of params section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      <lambda_param>:
          arg1 = "foo",
          arg2 = "boo"
      <lambda_section>: lambda wildcards, <lambda_param>: <lambda_param>.
    """
    When I put the caret after <lambda_param>.
    And I invoke autocompletion popup
    Then completion list should contain:
      | arg1 |
      | arg2 |
    Examples:
      | rule_like  | lambda_section | lambda_param |
      | rule       | params         | input        |
      | rule       | params         | output       |
      | rule       | params         | resources    |
      | rule       | resources      | input        |
      | rule       | threads        | input        |
      | rule       | conda          | input        |
      | rule       | conda          | params       |
      | checkpoint | params         | resources    |
      | checkpoint | resources      | input        |
      | checkpoint | threads        | input        |

  Scenario Outline: Completion for wildcards params section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      output:
          arg1 = "{foo}",
          arg2 = "{boo}"
      <lambda_section>: lambda <wildcards_params>: <wildcards_params>.
    """
    When I put the caret after <wildcards_params>.
    And I invoke autocompletion popup
    Then completion list should contain:
      | foo |
      | boo |
    Examples:
      | rule_like  | lambda_section | wildcards_params |
      | rule       | params         | wildcards        |
      | rule       | params         | wd               |
      | rule       | input          | wd               |
      | rule       | group          | wd               |
      | rule       | resources      | wd               |
      | rule       | threads        | wd               |
      | rule       | conda          | wd               |
      | checkpoint | params         | wildcards        |
      | checkpoint | input          | wd               |

  Scenario Outline: No completion for lambda callable
    Given a snakemake project
    Given I open a file "foo.smk" with text
        """
        <rule_like> NAME:
          <lambda_param>:
              arg1 = "foo",
              arg2 = "boo"
          <lambda_section>: (lambda wildcards, <lambda_param>: <lambda_param>.kk)()
        """
    When I put the caret after <lambda_param>.
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | arg1 |
      | arg2 |
    Examples:
      | rule_like  | lambda_section | lambda_param |
      | rule       | params         | input        |
      | rule       | threads        | input        |
      | rule       | resources      | input        |
      | rule       | conda          | params       |
      | checkpoint | params         | output       |
      | checkpoint | threads        | input        |