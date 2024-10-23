Feature: Resolve implicitly imported python names
  Resolve runtime magic from snakemake


  # TODO: Resolve config into config.yaml + remove Unresolved inspection suppress
  #    - config["a"]
  #    - config

  Scenario Outline: Check different SDK settings
    Given a snakemake with disabled framework project
    Given I open a file "foo.smk" with text
    """
    <text>
    """
    When I put the caret at <ptn>
    Then reference should not resolve
    When add snakemake framework support without wrappers loaded
    Then reference should resolve to "<symbol_name>" in "<file>"
    When set project sdk as none interpreter
    Then reference should not resolve
    When set project sdk as python with snakemake interpreter
    Then reference should resolve to "<symbol_name>" in "<file>"
    When set snakemake framework sdk to invalid interpreter
    Then reference should not resolve
    When set snakemake framework sdk to python with snakemake interpreter
    Then reference should resolve to "<symbol_name>" in "<file>"
    When set project sdk as none interpreter
    When set snakemake framework sdk to project interpreter
    Then reference should not resolve
    When set project sdk as python with snakemake interpreter
    Then reference should resolve to "<symbol_name>" in "<file>"
    When set project sdk as python only interpreter
    Then reference should not resolve

    Examples:
      | ptn | text        | symbol_name | file         |
      | exp | expand()    | expand      | io.py        |


  @ignore
  Scenario: Do not resolve at top-level if no python sdk
    # TODO

  @ignore
  Scenario: Resolve at top-level if custom python sdk
    # TODO

  Scenario Outline: Resolve at top-level
    Given a <smk_vers> project
    Given I open a file "foo.smk" with text
    """
    <text>
    """
    When I put the caret at <ptn>
    Then reference should resolve to "<symbol_name>" in "<file>"

    Examples:
      | smk_vers         | ptn | text            | symbol_name   | file         |
      | snakemake:5x     | ru  | rules           | rules         | workflow.py  |
      | snakemake:5x     | ru  | rules.foo       | rules         | workflow.py  |
      | snakemake:6.1    | ru  | rules           | Rules         | common.py    |
      | snakemake:6.1    | ru  | rules.foo       | Rules         | common.py    |
      | snakemake:6.5    | ru  | rules           | Rules         | __init__.py  |
      | snakemake:6.5    | ru  | rules.foo       | Rules         | __init__.py  |
      | snakemake:6.1    | dyn | dynamic()       | dynamic       | io.py        |
      | snakemake:7.32.4 | dyn | dynamic()       | dynamic       | io.py        |
      | snakemake        | exp | expand()        | expand        | io.py        |
      | snakemake        | tem | temp()          | temp          | io.py        |
      | snakemake        | dir | directory()     | directory     | io.py        |
      | snakemake        | dir | directory()     | directory     | io.py        |
      | snakemake        | pro | protected()     | protected     | io.py        |
      | snakemake        | upd | update()        | update        | ioflags.py   |
      | snakemake        | bef | before_update() | before_update | ioflags.py   |
      | snakemake        | lo  | lookup()        | lookup        | ioutils.py   |
      | snakemake        | br  | branch()        | branch        | ioutils.py   |
      | snakemake        | ev  | evaluate()      | evaluate      | ioutils.py   |
      | snakemake        | col | collect()       | expand        | io.py        |
      | snakemake        | ex  | exists()        | exists        | ioutils.py   |
      | snakemake        | fr  | from_queue()    | from_queue    | io.py        |
      | snakemake        | tou | touch()         | touch         | io.py        |
      | snakemake        | un  | unpack()        | unpack        | io.py        |
      | snakemake        | anc | ancient()       | ancient       | io.py        |
      | snakemake        | ens | ensure()        | ensure        | io.py        |
      | snakemake        | ru  | rules           | Rules         | __init__.py  |
      | snakemake        | ru  | rules.foo       | Rules         | __init__.py  |
      | snakemake        | inp | input           | input         | builtins.pyi |
      | snakemake        | pe  | pep             | __init__      | project.py   |
      | snakemake        | pe  | pep.config      | __init__      | project.py   |

  Scenario Outline: Resolve implicit python modules/classes at top-level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <text>
    """
    When I put the caret at <ptn>
    Then reference should resolve to "<symbol_name>" in "<file>"

    Examples:
      | ptn | text      | symbol_name | file                  |
      | os  | os        | [SKIP]      | os/__init__.pyi       |
      | sy  | sys       | [SKIP]      | sys.py                |
      | sn  | snakemake | [SKIP]      | snakemake/__init__.py |
      | Pat | Path      | Path        | pathlib.pyi           |

  Scenario: Resolve at top-level: shell()
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
      shell()
    """
    When I put the caret at she
    Then reference should multi resolve to name, file, times[, class name]
      | shell  | shell.py     | 1 |

  Scenario Outline: Also available on top-level at runtime, but not API
    Given a <smk_vers> project
    Given I open a file "foo.smk" with text
    """
    <text>
    """
    When I put the caret at <ptn>
    Then reference should resolve to "<symbol_name>" in "<file>"

    Examples:
      | smk_vers      | ptn  | text     | symbol_name       | file           |
      | snakemake:5x  | wor  | workflow | workflow          | workflow.py    |
      | snakemake:6.1 | wor  | workflow | Workflow          | workflow.py    |
      | snakemake     | wor  | workflow | Workflow          | workflow.py    |
      | snakemake     | conf | config   | overwrite_config  | types.py       |
      | snakemake     | sto  | storage  | _storage_registry | workflow.py    |
      | snakemake     | git  | github   | GithubFile        | sourcecache.py |

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
      | log | log        |

  Scenario Outline: Resolve inside rule parameters
    Given a <smk_vers> project
    Given I open a file "foo.smk" with text
    """
    rule all:
      input: <text>
    """
    When I put the caret at <ptn>
    Then reference should resolve to "<symbol_name>" in "<file>"

    Examples:
      | smk_vers      | ptn   | text       | symbol_name       | file           |
      | snakemake:5x  | rules | rules.foo  | rules             | workflow.py    |
      | snakemake:6.1 | rules | rules.foo  | Rules             | common.py      |
      | snakemake:6.5 | rules | rules.foo  | Rules             | __init__.py    |
      | snakemake     | exp   | expand()   | expand            | io.py          |
      | snakemake     | rules | rules.foo  | Rules             | __init__.py    |
      | snakemake     | pep   | pep.config | __init__          | project.py     |
      | snakemake     | sto   | storage    | _storage_registry | workflow.py    |
      | snakemake     | git   | github     | GithubFile        | sourcecache.py |
      | snakemake     | git   | gitfile    | LocalGitFile      | sourcecache.py |
      | snakemake     | con   | config     | overwrite_config  | types.py       |

  Scenario: Resolve inside rule parameters: shell()
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule all:
      input: shell()
    """
    When I put the caret at she
    Then reference should multi resolve to name, file, times[, class name]
      | shell  | shell.py     | 1 |

  Scenario Outline: Resolve inside run section
    Given a <smk_vers> project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      run:
        <text> #here
    """
    When I put the caret at <ptn>
      Then reference should multi resolve to name, file, times[, class name]
      | <symbol_name> | <file> | <times> |

    Examples:
      | smk_vers      | ptn         | text        | symbol_name | file           | times |
      | snakemake:5x  | checkpoints | checkpoints | checkpoints | workflow.py    | 1     |
      | snakemake:5x  | rules       | rules.foo   | rules       | workflow.py    | 1     |
      | snakemake:6.1 | checkpoints | checkpoints | Checkpoints | checkpoints.py | 1     |
      | snakemake:6.1 | rules       | rules.foo   | Rules       | common.py      | 1     |
      | snakemake:6.5 | rules       | rules.foo   | Rules       | __init__.py    | 1     |
      | snakemake     | exp         | expand()    | expand      | io.py          | 1     |
      | snakemake     | she         | shell()     | shell       | shell.py       | 1     |
      | snakemake     | rules       | rules.foo   | Rules       | __init__.py    | 1     |
      | snakemake     | checkpoints | checkpoints | Checkpoints | checkpoints.py | 1     |
      | snakemake     | inp         | input[0]    | InputFiles  | io.py          | 1     |
      | snakemake     | output.foo  | output.foo  | OutputFiles | io.py          | 1     |
      | snakemake     | par         | params      | Params      | io.py          | 1     |
      | snakemake     | wil         | wildcards   | Wildcards   | io.py          | 1     |
      | snakemake     | res         | resources   | Resources   | io.py          | 1     |
      | snakemake     | lo          | log         | Log         | io.py          | 1     |
      | snakemake     | pep         | pep.config  | __init__     | project.py     | 1     |

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

  Scenario Outline: Resolve section name inside run section (threads is fake implicit symbol)
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      output: "path/to/output"
      <text>
      run:
        <symbol_name> #here
    """
    When I put the caret at <symbol_name> #here
    Then reference should multi resolve to name, file, times[, class name]
      | <symbol_name> | <file> | <times> | <class> |

    Examples:
      | rule_like  | text       | symbol_name | file    | times | class                              |
      | rule       | threads: 1 | threads     | foo.smk | 1     | SmkRuleOrCheckpointArgsSectionImpl |
      | checkpoint | threads: 1 | threads     | foo.smk | 1     | SmkRuleOrCheckpointArgsSectionImpl |
      | rule       | version: 1 | version     | foo.smk | 1     | SmkRuleOrCheckpointArgsSectionImpl |
      | checkpoint | version: 1 | version     | foo.smk | 1     | SmkRuleOrCheckpointArgsSectionImpl |

  Scenario Outline: Resolve rule variable inside run section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      run:
        rule #here
    """
    When I put the caret at rule #here
    Then reference should multi resolve to name, file, times[, class name]
      | <symbol_name> | <file> | <times> | <class> |

    Examples:
      | rule_like  | symbol_name | file    | times | class             |
      | rule       | NAME        | foo.smk | 1     | SmkRuleImpl       |
      | checkpoint | NAME        | foo.smk | 1     | SmkCheckPointImpl |

  Scenario Outline: Not-resolved fake variables if part of reference
    Given a snakemake project
    Given I open a file "foo.smk" with text
        """
        <rule_like> NAME:
            threads: 1
            run:
              a.<text> #here
        """
    When I put the caret after a.<ptn>
    Then reference should not multi resolve to files
      | foo.smk |
    Examples:
      | rule_like  | text    | ptn |
      | rule       | threads | thr |
      | rule       | rule    | ru  |
      | rule       | jobid   | jo  |
      | checkpoint | threads | thr |

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
      | rule       | vers      | version    |
      | rule       | rule #    | rule       |
      | rule       | jobid     | jobid      |
      | checkpoint | out       | output.foo |
      | checkpoint | wild      | wildcards  |

    @ignore
  Scenario Outline: Resolved in top-level pipeline handler block
    Given a snakemake project
    Given I open a file "foo.smk" with text
       """
       <block>:
           print(log)
       """
    When I put the caret at log
    Then reference should resolve to NULL

    Examples:
      | block     |
      | onstart   |
      | onerror   |
      | onsuccess |

  Scenario Outline: Not-resolved in top level python block
    Given a snakemake project
    Given I open a file "foo.smk" with text
       """
       <block>:
           <text> #here
       """
    When I put the caret at <ptn>
    Then reference should not resolve

    Examples:
      | block     | ptn         | text       |
      | onstart   | out         | output.foo |
      | onstart   | par         | params     |
      | onstart   | wil         | wildcards  |
      | onstart   | res         | resources  |
      | onstart   | lo          | log        |
      | onstart   | thr         | threads    |
      | onstart   | rule #here  | rule       |
      | onstart   | jobid #here | jobid      |
      | onerror   | wil         | wildcards  |
      | onsuccess | wil         | wildcards  |

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
    Given a <smk_vers> project
    Given I open a file "foo.smk" with text
        """
        <rule_like> NAME:
           <section>: "{<text>}"
        """
    When I put the caret after "{
    Then reference in injection should resolve to "<result>" in "<file>"
    Examples:
      | rule_like  | smk_vers      | section | text        | result            | file           |
      | rule       | snakemake:5x  | shell   | rules       | rules             | workflow.py    |
      | rule       | snakemake:5x  | shell   | checkpoints | checkpoints       | workflow.py    |
      | rule       | snakemake:5x  | message | rules       | rules             | workflow.py    |
      | checkpoint | snakemake:5x  | shell   | rules       | rules             | workflow.py    |
      | rule       | snakemake:6.1 | shell   | rules       | Rules             | common.py      |
      | rule       | snakemake:6.1 | shell   | checkpoints | Checkpoints       | checkpoints.py |
      | rule       | snakemake:6.1 | message | rules       | Rules             | common.py      |
      | rule       | snakemake:6.1 | message | scatter     | Scatter           | common.py      |
      | rule       | snakemake:6.1 | message | gather      | Gather            | common.py      |
      | checkpoint | snakemake:6.1 | shell   | rules       | Rules             | common.py      |
      | rule       | snakemake:6.5 | shell   | rules       | Rules             | __init__.py    |
      | rule       | snakemake:6.5 | message | rules       | Rules             | __init__.py    |
      | rule       | snakemake:6.5 | message | scatter     | Scatter           | __init__.py    |
      | rule       | snakemake:6.5 | message | gather      | Gather            | __init__.py    |
      | checkpoint | snakemake:6.5 | shell   | rules       | Rules             | __init__.py    |
      | rule       | snakemake     | shell   | rules       | Rules             | __init__.py    |
      | rule       | snakemake     | shell   | checkpoints | Checkpoints       | checkpoints.py |
      | rule       | snakemake     | message | rules       | Rules             | __init__.py    |
      | rule       | snakemake     | message | scatter     | Scatter           | __init__.py    |
      | rule       | snakemake     | message | gather      | Gather            | __init__.py    |
      | checkpoint | snakemake     | shell   | rules       | Rules             | __init__.py    |
      | rule       | snakemake     | shell   | pep         | __init__          | project.py     |
      | rule       | snakemake     | message | pep         | __init__          | project.py     |
      | checkpoint | snakemake     | shell   | pep         | __init__          | project.py     |
      | rule       | snakemake     | message | gitfile     | LocalGitFile      | sourcecache.py |
      | rule       | snakemake     | message | storage     | _storage_registry | workflow.py    |
      | rule       | snakemake     | message | github      | GithubFile        | sourcecache.py   |

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

  Scenario Outline: Resolve undefined wildcards with names like explicit symbols not into these symbols
    Given a snakemake project
    Given I open a file "foo.smk" with text
        """
        <rule_like> NAME:
           <section>: "{<text>}"
        """
    When I put the caret after "{
    Then reference in injection should resolve to "<text>" in context "<text>}"" in file "foo.smk"
    Examples:
      | rule_like  | section | text        |
      | rule       | input   | rules       |
      | rule       | params  | rules       |
      | rule       | input   | checkpoints |
      | rule       | input   | config      |
      | checkpoint | input   | rules       |

  @ignore
  Scenario Outline: Do not warn about unresolved log variable on top-level pipeline handler block
    Given a snakemake project
    Given I open a file "foo.smk" with text
     """
     <block>:
        print("Log:")
        print(log)
     """
    And PyUnresolvedReferencesInspection inspection is enabled
    Then I expect no inspection warnings
    When I check highlighting warnings
    Examples:
      | block    |
      | onstart  |
      | onerror  |
      | onsuccess |

  Scenario: Warn about unresolved snakemake variable in run section, behaviour differs from scripts
    Given a snakemake project
    Given I open a file "foo.smk" with text
     """
     rule a:
         run:
             # this becomes too much and should be migrated into a script directive
             path = snakemake.input[0]
     """
    And PyUnresolvedReferencesInspection inspection is enabled
    Then I expect inspection warning on <input> with message
      """
      Cannot find reference 'input' in '__init__.py'
      """
    When I check highlighting warnings