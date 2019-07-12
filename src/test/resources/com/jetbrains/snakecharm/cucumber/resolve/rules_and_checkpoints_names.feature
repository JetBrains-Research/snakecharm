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
    Then reference should multi resolve to name, file, times[, class name]
      | foo | foo.smk | 2 | <class> |
    Examples:
      | rule_like  | class             |
      | rule       | SmkRuleImpl       |
      | checkpoint | SmkCheckPointImpl |

  Scenario Outline: Resolve for rule/checkpoint names from different files
    Given a snakemake project
    And a file "boo1.smk" with text
     """
     <rule_like> boo1:
       input: "path/to/input"

     <rule_like> foo:
       input: "path/to/input"
     """
    And a file "boo2.smk" with text
     """
     <rule_like> boo2:
       input: "path/to/input"

     <rule_like> boo2:
       input: "path/to/input"

     rule rule2:
       input: "path/to/input"

     checkpoint checkpoint2:
       input: "path/to/input"

     subworkflow subworkflow2:
       snakefile: "s"
     """
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

     <rule_like> cccc:
       input: <rule_like>s.<symbol_name>
     """
    When I put the caret after <rule_like>s.
    Then reference should multi resolve to name, file, times[, class name]
      | <symbol_name> | <file> | <usages> | <class> |
    Examples:
      | rule_like  | symbol_name | file     | usages | class             |
      | rule       | foo         | foo.smk  | 2      | SmkRuleImpl       |
      | rule       | foo         | boo1.smk | 1      | SmkRuleImpl       |
      | rule       | boo1        | boo1.smk | 1      | SmkRuleImpl       |
      | rule       | boo2        | boo2.smk | 2      | SmkRuleImpl       |
      | checkpoint | foo         | foo.smk  | 2      | SmkCheckPointImpl |
      | checkpoint | foo         | boo1.smk | 1      | SmkCheckPointImpl |
      | checkpoint | boo1        | boo1.smk | 1      | SmkCheckPointImpl |
      | checkpoint | boo2        | boo2.smk | 2      | SmkCheckPointImpl |

  Scenario Outline: No resolve for subworkflow nam instead fo rule/checkpoint names from different files
    Given a snakemake project
    And a file "foo2.smk" with text
       """
       subworkflow subworkflow2:
         snakefile: "s"
       """
    Given I open a file "foo1.smk" with text
       """
       subworkflow subworkflow1:
         snakefile: "s"

       <rule_like> foo:
         input: <rule_like>s.<symbol_name>
       """
    When I put the caret after <rule_like>s.
    Then reference should not resolve
    Examples:
      | rule_like  | symbol_name  |
      | rule       | subworkflow1 |
      | rule       | subworkflow2 |
      | checkpoint | subworkflow1 |
      | checkpoint | subworkflow2 |