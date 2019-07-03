Feature: Resolve rule names used with rules (e.g. 'rules.NAME')
  to their corresponding declarations

  Scenario: Resolve for a particular rule name when 'rules' is used inside a rule section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule aaaa:
      input: "path/to/input"
      output: "path/to/output"
      shell: "shell command"

    rule bbbb:
      input: "path/to/input"
      output: "path/to/output"
      script: "script.py"

    rule cccc:
      input: rules.aaaa
    """
    When I put the caret after   input: rules.aa
    Then reference should resolve to "aaaa" in "foo.smk"

  Scenario Outline: Resolve for all rule names when 'rules' is used inside a rule section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule aaaa:
      input: "path/to/input"
      output: "path/to/output"
      shell: "shell command"

    rule bbbb:
      input: "path/to/input"
      output: "path/to/output"
      script: "script.py"

    rule cccc:
      input: rules.<text>
    """
    When I put the caret after <ptn>
    Then reference should resolve to "<symbol_name>" in "<file>"

    Examples:
      | ptn       | text      | symbol_name | file         |
      | rules.aa  | aaaa      | aaaa        | foo.smk      |
      | rules.bbb | bbbb      | bbbb        | foo.smk      |
      | rules.c   | cccc      | cccc        | foo.smk      |

  Scenario Outline: Resolve for all rule names when 'rules' is used at top level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule aaaa:
      input: "path/to/input"
      output: "path/to/output"
      shell: "shell command"

    rule bbbb:
      input: "path/to/input"
      output: "path/to/output"
      script: "script.py"

    rules.<text>
    """
    When I put the caret after <ptn>
    Then reference should resolve to "<symbol_name>" in "<file>"

    Examples:
      | ptn       | text      | symbol_name | file         |
      | rules.aa  | aaaa      | aaaa        | foo.smk      |
      | rules.bbb | bbbb      | bbbb        | foo.smk      |


  Scenario: Multi resolve for rules with same name
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule foo:
      input: "path/to/input"
      output: "path/to/output"
      shell: "shell command"

    rule foo:
      input: "path/to/input"
      output: "path/to/output"
      script: "script.py"

    rules.foo
    """
    When I put the caret after rules.f
    Then reference should multi resolve to name, file, times
    | foo | foo.smk | 2 |
