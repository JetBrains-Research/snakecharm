Feature: Resolve for section lambda params attrs
  #TODO: If lambda args need to be changed, please refactor all test here & move cases into mock Snakemake API YAML file

  Scenario Outline: Resolve attrs of params section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      <lambda_param>:
          arg1 = "foo"
      <lambda_section>: lambda wildcards, <lambda_param>: <lambda_param>.arg1
    """
    When I put the caret after <lambda_param>.arg
    Then reference should resolve to "arg1 = "foo"" in "foo.smk"
    Examples:
      | rule_like  | lambda_section | lambda_param |
      | rule       | params         | input        |
      | rule       | params         | output       |
      | rule       | params         | resources    |
      | rule       | resources      | input        |
      | rule       | threads        | input        |
      | rule       | conda          | input        |
      | rule       | conda          | params       |
      | checkpoint | params         | input        |
      | checkpoint | params         | output       |
      | checkpoint | params         | resources    |
      | checkpoint | resources      | input        |
      | checkpoint | threads        | input        |

  Scenario Outline: Resolve for wildcards params section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      output:
          arg1 = "{foo}"
      <lambda_section>: lambda <wildcards_params>: <wildcards_params>.foo
    """
    When I put the caret after <wildcards_params>.fo
    Then reference should resolve to "foo" in "foo.smk"
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
      | checkpoint | params         | wd               |
      | checkpoint | input          | wd               |
      | checkpoint | group          | wd               |
      | checkpoint | resources      | wd               |
      | checkpoint | threads        | wd               |

  Scenario Outline: No resolve in lambda callable
    Given a snakemake project
    Given I open a file "foo.smk" with text
        """
        <rule_like> NAME:
          <lambda_param>:
              arg1 = "foo",
              arg2 = "boo"
          <lambda_section>: (lambda wildcards, <lambda_param>: <lambda_param>.arg1)()
        """
    When I put the caret after <lambda_param>.arg
    Then reference should not resolve
    Examples:
      | rule_like  | lambda_section | lambda_param |
      | rule       | params         | input        |
      | rule       | threads        | input        |
      | rule       | conda          | input        |
      | checkpoint | params         | output       |
      | checkpoint | threads        | input        |
