Feature: Resolve name after 'rules.' and 'checkpoints.' to their corresponding declarations

  Scenario Outline: Resolve for rule/checkpoint name when inside a rule section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> aaaa:
      input: "path/to/input"
      output: "path/to/output"
      shell: "shell command"

    <rule_like> bbbb:
      input: "path/to/input"
      output: "path/to/output"
      script: "script.py"

    <target> cccc:
      input: <rule_like>s.aaaa
    """
    When I put the caret after input: <rule_like>s.aa
    Then reference should resolve to "aaaa" in "foo.smk"
    Examples:
      | target     | rule_like  |
      | rule       | rule       |
      | checkpoint | rule       |
      | rule       | checkpoint |
      | checkpoint | checkpoint |

  Scenario Outline: Resolve for rule/checkpoint names inside a rule section (different declarations)
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> aaaa:
      input: "path/to/input"
      output: "path/to/output"
      shell: "shell command"

    <rule_like> bbbb:
      input: "path/to/input"
      output: "path/to/output"
      script: "script.py"

    <rule_like> cccc:
      input: <rule_like>s.<symbol_name>
    """
    When I put the caret after <rule_like>s.<ptn>
    Then reference should resolve to "<symbol_name>" in "<file>"

    Examples:
      | ptn | symbol_name | file    | rule_like  |
      | aa  | aaaa        | foo.smk | rule       |
      | bbb | bbbb        | foo.smk | rule       |
      | c   | cccc        | foo.smk | rule       |
      | aa  | aaaa        | foo.smk | checkpoint |
      | bbb | bbbb        | foo.smk | checkpoint |
      | c   | cccc        | foo.smk | checkpoint |

  Scenario Outline: Resolve for all rule/checkpoint names at top level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> aaaa:
      input: "path/to/input"
      output: "path/to/output"
      shell: "shell command"

    <rule_like> bbbb:
      input: "path/to/input"
      output: "path/to/output"
      script: "script.py"

    <rule_like>s.<symbol_name>
    """
    When I put the caret after <rule_like>s.<ptn>
    Then reference should resolve to "<symbol_name>" in "<file>"

    Examples:
      | rule_like  | ptn | symbol_name | file    |
      | rule       | aa  | aaaa        | foo.smk |
      | rule       | bbb | bbbb        | foo.smk |
      | checkpoint | aa  | aaaa        | foo.smk |
      | checkpoint | bbb | bbbb        | foo.smk |


  Scenario Outline: Multi resolve for rule/checkpoint with same name
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      input: "path/to/input"
      output: "path/to/output"
      shell: "shell command"

    <rule_like> foo:
      input: "path/to/input"
      output: "path/to/output"
      script: "script.py"

    <rule_like>s.foo
    """
    When I put the caret after <rule_like>s.f
    Then reference should multi resolve to name, file, times
      | foo (<class>) | foo.smk | 2 |
    Examples:
      | rule_like  | class         |
      | rule       | SMKRule       |
      | checkpoint | SMKCheckPoint |
