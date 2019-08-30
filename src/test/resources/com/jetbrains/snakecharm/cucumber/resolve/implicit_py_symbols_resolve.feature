Feature: Resolve implicitly imported python names
  Resolve runtime magic from snakemake

  Scenario Outline: Resolve at top-level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <text>
    """
    When I put the caret at <ptn>
    Then reference should resolve to "<symbol_name>" in "<file>"

    Examples:
      | ptn | text        | symbol_name | file         |
      | exp | expand()    | expand      | io.py        |
      | tem | temp()      | temp        | io.py        |
      | dir | directory() | directory   | io.py        |
      | dir | directory() | directory   | io.py        |
      | pro | protected() | protected   | io.py        |
      | tou | touch()     | touch       | io.py        |
      | dyn | dynamic()   | dynamic     | io.py        |
      | un  | unpack()    | unpack      | io.py        |
      | anc | ancient()   | ancient     | io.py        |
      | she | shell()     | __new       | shell.py     |
      | con | config      | config      | workflow.py  |
      | con | config["a"] | config      | workflow.py  |
      | ru  | rules       | rules       | workflow.py  |
      | ru  | rules.foo   | rules       | workflow.py  |
      | inp | input       | input       | builtins.pyi |

  Scenario Outline: Also available on top-level at runtime, but not API
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <text>
    """
    When I put the caret at <ptn>
    Then reference should resolve to "<symbol_name>" in "<file>"

    Examples:
      | ptn | text     | symbol_name | file        |
      | wor | workflow | workflow    | workflow.py |

  Scenario Outline: Not-resolved at top-level
    Given a snakemake project
    Given I open a file "foo.smk" with text
     """
     <text>
     """
    When I put the caret at <ptn>
    Then reference should not resolve

    Examples:
      | ptn | text       |
      | out | output.foo |
      | par | params     |
      | wil | wildcards  |


  Scenario Outline: Resolve inside rule parameters
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule all:
      input: <text>
    """
    When I put the caret at <ptn>
    Then reference should resolve to "<symbol_name>" in "<file>"

    Examples:
      | ptn   | text        | symbol_name | file        |
      | exp   | expand()    | expand      | io.py       |
      | con   | config["a"] | config      | workflow.py |
      | rules | rules.foo   | rules       | workflow.py |
      | she   | shell()     | __new       | shell.py    |

  Scenario Outline: Resolve inside run section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      output: "path/to/output"
      run:
        <text> #here
    """
    When I put the caret at <ptn>
    Then reference should multi resolve to name, file, times[, class name]
      | <symbol_name> | <file> | <times> |

    Examples:
      | ptn         | text        | symbol_name | file        | times |
      | exp         | expand()    | expand      | io.py       | 1     |
      | she         | shell()     | __new__     | shell.py    | 1     |
      | con         | config["a"] | config      | workflow.py | 1     |
      | rules       | rules.foo   | rules       | workflow.py | 1     |
      | checkpoints | checkpoints | checkpoints | workflow.py | 1     |
      | inp         | input[0]    | InputFiles  | io.py       | 1     |
      | output.foo  | output.foo  | OutputFiles | io.py       | 1     |
      | par         | params      | Params      | io.py       | 1     |
      | wil         | wildcards   | Wildcards   | io.py       | 1     |
      | res         | resources   | Resources   | io.py       | 1     |
      | lo          | log         | Log         | io.py       | 1     |

  Scenario: Resolve results priority
      Given a snakemake project
      Given I open a file "foo.smk" with text
      """
      rule NAME:
        output: "path/to/output"
        run:
          input #here
      """
      When I put the caret at input #here
      Then reference should multi resolve to name, file in same order
        | InputFiles | io.py        |
        | input      | builtins.pyi |

  Scenario Outline: Resolve threads inside run section (threads is fake implicit symbol)
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      output: "path/to/output"
      <text>
      run:
        threads #here
    """
    When I put the caret at threads #here
    Then reference should multi resolve to name, file, times[, class name]
      | <symbol_name> | <file> | <times> | <class> |

    Examples:
      | rule_like  | text       | symbol_name | file    | times | class                              |
      | rule       | threads: 1 | threads: 1  | foo.smk | 1     | SmkRuleOrCheckpointArgsSectionImpl |
      | checkpoint | threads: 1 | threads: 1  | foo.smk | 1     | SmkRuleOrCheckpointArgsSectionImpl |

  Scenario Outline: Not-resolved threads if part of reference
    Given a snakemake project
    Given I open a file "foo.smk" with text
        """
        <rule_like> NAME:
            threads: 1
            run:
              a.threads #here
        """
    When I put the caret after a.thr
    Then reference should not multi resolve to files
      | foo.smk |
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Not-resolved in rule outside run section
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <rule_like> NAME:
        log:
          <text> #here
      """
    When I put the caret at <ptn>
    Then reference should not resolve

    Examples:
      | rule_like  | ptn       | text       |
      | rule       | out       | output.foo |
      | rule       | par       | params     |
      | rule       | wild      | wildcards  |
      | rule       | res       | resources  |
      | rule       | log #here | log        |
      | rule       | thr       | threads    |
      | checkpoint | out       | output.foo |
      | checkpoint | wild      | wildcards  |

  Scenario Outline: Not-resolved in top level python block
    Given a snakemake project
    Given I open a file "foo.smk" with text
       """
       <block>:
           <text>
       """
    When I put the caret at <ptn>
    Then reference should not resolve

    Examples:
      | block     | ptn | text       |
      | onstart   | out | output.foo |
      | onstart   | par | params     |
      | onstart   | wil | wildcards  |
      | onstart   | res | resources  |
      | onstart   | lo  | log        |
      | onstart   | thr | threads    |
      | onerror   | wil | wildcards  |
      | onsuccess | wil | wildcards  |

  Scenario: Resolve also works inside call args
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule all:
      input: directory(expand("{dataset}/dir", dataset=[]))
    """
    When I put the caret at exp
    Then reference should resolve to "expand" in "io.py"

  Scenario Outline: Implicit resolve is off in python dialects files
    Given a snakemake project
    Given I open a file "foo.<ext>" with text
    """
    <symbol_name>()
    """
    When I put the caret at <ptn>
    Then reference should not resolve

    Examples:
      | ptn | symbol_name | ext |
      | exp | expand      | py  |
      | exp | expand      | pyi |

  Scenario Outline: Resolve in injections
    Given a snakemake project
    Given I open a file "foo.smk" with text
        """
        <rule_like> NAME:
           <section>: "{<text>}"
        """
    When I put the caret after "{
    And I invoke autocompletion popup
    Then completion list should contain:
      | rules       |
      | checkpoints |
      | config      |

    Examples:
      | rule_like  | section |
      | rule       | shell   |
      | rule       | message |
      | checkpoint | shell   |

  Scenario Outline: No resolve in injections for defining expanding sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
        """
        <rule_like> NAME:
           <section>: "{<text>}"
        """
    When I put the caret after "{

    Then reference in injection should multi resolve to name, file, times[, class name]
      | <symbol_name> | <file> | <times> |

    Examples:
      | rule_like  | section   | text        | symbol_name | file        | times |
      | rule       | output    | rules       | rules       | workflow.py | 0     |
      | rule       | log       | checkpoints | checkpoints | workflow.py | 0     |
      | rule       | benchmark | config      | config      | workflow.py | 0     |
      | checkpoint | output    | rules       | rules       | workflow.py | 0     |

  Scenario Outline: No resolve in injections for wildcards expanding sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
        """
        <rule_like> NAME:
           <section>: "{<text>}"
        """
    When I put the caret after "{
    Then reference in injection should not resolve

    Examples:
      | rule_like  | section | text        |
      | rule       | input   | rules       |
      | rule       | input   | checkpoints |
      | rule       | input   | config      |
      | checkpoint | input   | rules       |
