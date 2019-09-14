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
    When I put the caret after <rule_like>s.aa
    Then reference should resolve to "aaaa" in "foo.smk"
    Examples:
      | target     | rule_like  |
      | rule       | rule       |
      | checkpoint | rule       |
      | rule       | checkpoint |
      | checkpoint | checkpoint |

  Scenario Outline: Resolve for rule/checkpoint name when inside an injection
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
      message: "{<rule_like>s.aaaa}"
    """
    When I put the caret after <rule_like>s.aa
    Then reference in injection should resolve to "aaaa" in "foo.smk"
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
    Then reference should resolve to "<symbol_name>" in "foo.smk"
    Examples:
      | ptn | symbol_name | rule_like  |
      | aa  | aaaa        | rule       |
      | bbb | bbbb        | rule       |
      | c   | cccc        | rule       |
      | aa  | aaaa        | checkpoint |
      | bbb | bbbb        | checkpoint |
      | c   | cccc        | checkpoint |

  Scenario Outline: Resolve for different declarations of rule/checkpoint in language injection
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
      message: "{<rule_like>s.<symbol_name>}"
    """
    When I put the caret after <rule_like>s.<ptn>
    Then reference in injection should resolve to "<symbol_name>" in "foo.smk"
    Examples:
      | ptn | symbol_name | rule_like  |
      | aa  | aaaa        | rule       |
      | bbb | bbbb        | rule       |
      | c   | cccc        | rule       |
      | aa  | aaaa        | checkpoint |
      | bbb | bbbb        | checkpoint |
      | c   | cccc        | checkpoint |

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
    Then reference should resolve to "<symbol_name>" in "foo.smk"
    Examples:
      | rule_like  | ptn | symbol_name |
      | rule       | aa  | aaaa        |
      | rule       | bbb | bbbb        |
      | checkpoint | aa  | aaaa        |
      | checkpoint | bbb | bbbb        |


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

  Scenario Outline: No resolve for subworkflow name instead fo rule/checkpoint names from different files
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

  Scenario Outline: No resolve for subworkflow name in injection
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
         input: "{<rule_like>s.<symbol_name>}"
       """
    When I put the caret after <rule_like>s.
    Then reference in injection should not resolve
    Examples:
      | rule_like  | symbol_name  |
      | rule       | subworkflow1 |
      | rule       | subworkflow2 |
      | checkpoint | subworkflow1 |
      | checkpoint | subworkflow2 |

  Scenario Outline: No resolve for long reference with rules/checkpoints last part
     Given a snakemake project
     Given I open a file "foo1.smk" with text
        """
        <rule_like> boo:
          input: "s"

        <rule_like> foo:
          input: roo.too.<rule_like>s.<symbol_name>
        """
     When I put the caret after <rule_like>s.
     Then reference should not resolve
     Examples:
       | rule_like  | symbol_name  |
       | rule       | boo          |
       | checkpoint | boo          |

  Scenario Outline: No resolve for long reference with rules/checkpoints last part in injection
    Given a snakemake project
    Given I open a file "foo1.smk" with text
        """
        <rule_like> boo:
          input: "s"

        <rule_like> foo:
          input: "{roo.too.<rule_like>s.<symbol_name>}"
        """
    When I put the caret after <rule_like>s.
    Then reference in injection should not resolve
    Examples:
      | rule_like  | symbol_name  |
      | rule       | boo          |
      | checkpoint | boo          |

  Scenario Outline: No rule like declarations resolve for non-qualified names
      Given a snakemake project
      Given I open a file "foo.smk" with text
      """
      rule RULE:
        input: ""

      checkpoint CHECKPOINT:
        input: ""

      subworkflow SUBWORKFLOW:
        snakefile: ""

      <element># place toplevel
      rule foo:
        shell: "{<element>}" # shell injection
        run:
          <element># run section

      """
    When I put the caret at <element><place>
    Then <ref_type> should not resolve
    Examples:
      | element     | place                | ref_type               |
      | RULE        | # place toplevel     | reference              |
      | CHECKPOINT  | # place toplevel     | reference              |
      | RULE        | # run section        | reference              |
      | CHECKPOINT  | # run section        | reference              |
      | RULE        | }" # shell injection | reference in injection |
      | CHECKPOINT  | }" # shell injection | reference in injection |