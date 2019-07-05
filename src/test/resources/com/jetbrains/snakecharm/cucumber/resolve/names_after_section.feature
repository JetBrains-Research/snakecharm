Feature: Resolve section names to their corresponding declarations

  Scenario Outline: Resolve for a particular section name when '<section>s' is used inside a rule section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section> aaaa:
      input: "path/to/input"
      output: "path/to/output"
      shell: "shell command"

    <section> bbbb:
      input: "path/to/input"
      output: "path/to/output"
      script: "script.py"

    <section> cccc:
      input: <section>s.aaaa
    """
    When I put the caret after   input: <section>s.aa
    Then reference should resolve to "aaaa" in "foo.smk"
    Examples:
      | section    |
      | rule       |
      | checkpoint |

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


  Scenario Outline: Multi resolve for sections with same name
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section> foo:
      input: "path/to/input"
      output: "path/to/output"
      shell: "shell command"

    <section> foo:
      input: "path/to/input"
      output: "path/to/output"
      script: "script.py"

    <section>s.foo
    """
    When I put the caret after <section>s.f
    Then reference should multi resolve to name, file, times
    | foo | foo.smk | 2 |
    Examples:
      | section    |
      | rule       |
      | checkpoint |
