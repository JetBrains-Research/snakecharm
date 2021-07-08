Feature: Resolve to section args after section name

  Scenario Outline: Resolve in shell section in rules/checkpoints
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> aaaa:
      input: "path/to/input"
      output: "path/to/output"
      params:
        outdir=lambda wildcards, output: os.path.dirname(str(output)),
        xmx=lambda wildcards: str(800 // int(wildcards.bin)),
        file1="path",
        _file1="path"
      shell: "command {params.<text>}"
    """
    When I put the caret after <ptn>
    Then reference in injection should resolve to "<symbol_name>" in "<file>"
    Examples:
    | rule_like  | ptn         | text          | symbol_name | file         |
    | rule       | {params.out | outdir        | outdir      | foo.smk      |
    | rule       | {params.xm  | xmx           | xmx         | foo.smk      |
    | rule       | {params.fi  | file1         | file1       | foo.smk      |
    | rule       | {params._fi | _file1        | _file1      | foo.smk      |
    | checkpoint | {params.out | outdir        | outdir      | foo.smk      |
    | checkpoint | {params.xm  | xmx           | xmx         | foo.smk      |
    | checkpoint | {params.fi  | file1         | file1       | foo.smk      |
    | checkpoint | {params._fi | _file1        | _file1      | foo.smk      |

  Scenario Outline: Resolve in shell section in case of 'nested' parameters in rules/checkpoints
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      import os
      <rule_like> aaaa:
        input: "path/to/input"
        output: "path/to/output"
        params:
          xmx=os
        shell: "command {params.xmx<suffix>}"
      """
    When I put the caret after {params.xm
    Then reference in injection should resolve to "xmx" in "foo.smk"
    Examples:
    | rule_like  | suffix   |
    | rule       | .path    |
    | rule       | [path]   |
    | rule       | {'path'} |
    | checkpoint | .path    |
    | checkpoint | [path]   |
    | checkpoint | {'path'} |

  Scenario Outline: Resolve in run section with shell call in rules/checkpoints
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> aaaa:
      input: "path/to/input"
      output: "path/to/output"
      params:
        outdir=lambda wildcards, output: os.path.dirname(str(output)),
        xmx=lambda wildcards: str(800 // int(wildcards.bin)),
        file1="path",
        _file1="path"
      run:
        shell("command {params.<text>}")
    """
    When I put the caret after <ptn>
    Then reference in injection should resolve to "<symbol_name>" in "<file>"

    Examples:
      | rule_like  | ptn         | text          | symbol_name | file         |
      | rule       | {params.out | outdir        | outdir      | foo.smk      |
      | rule       | {params.xm  | xmx           | xmx         | foo.smk      |
      | rule       | {params.fi  | file1         | file1       | foo.smk      |
      | rule       | {params._fi | _file1        | _file1      | foo.smk      |
      | checkpoint | {params.out | outdir        | outdir      | foo.smk      |
      | checkpoint | {params.xm  | xmx           | xmx         | foo.smk      |
      | checkpoint | {params.fi  | file1         | file1       | foo.smk      |
      | checkpoint | {params._fi | _file1        | _file1      | foo.smk      |

  Scenario Outline: Resolve in run section with shell call in case of 'nested' parameters in rules/checkpoints
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      import os
      <rule_like> aaaa:
        input: "path/to/input"
        output: "path/to/output"
        params:
          xmx=os
        run:
          shell("command {params.xmx<suffix>}")
      """
    When I put the caret after {params.xm
    Then reference in injection should resolve to "xmx" in "foo.smk"
    Examples:
      | rule_like  | suffix   |
      | rule       | .path    |
      | rule       | [path]   |
      | rule       | {'path'} |
      | checkpoint | .path    |
      | checkpoint | [path]   |
      | checkpoint | {'path'} |

  Scenario Outline: Resolve to section keyword arguments in smksl injection
      Given a snakemake project
      Given I open a file "foo.smk" with text
      """
      <rule_like> NAME:
        <section>: kwd1="arg1"
        shell: "{<rule_like>s.NAME.<section>.kwd1}"
      """
      When I put the caret after NAME.<section>.kw
      Then reference in injection should resolve to "kwd1" in "foo.smk"
      Examples:
        | section   | rule_like  |
        | input     | rule       |
        | output    | rule       |
        | resources | rule       |
        | log       | rule       |
        | input     | checkpoint |
        | output    | checkpoint |
        | resources | checkpoint |
        | log       | checkpoint |

  Scenario Outline: Resolve to section keyword arguments in smksl subscription expression
      Given a snakemake project
      Given I open a file "foo.smk" with text
      """
      <rule_like> NAME:
        <section>: kwd1="arg1"
        shell: "{<rule_like>s.NAME.<section>[kwd1]}"
      """
      When I put the caret after <section>[kw
      Then reference in injection should resolve to "kwd1" in "foo.smk"
      Examples:
        | section   | rule_like  |
        | input     | rule       |
        | output    | rule       |
        | resources | rule       |
        | log       | rule       |
        | input     | checkpoint |
        | output    | checkpoint |
        | resources | checkpoint |
        | log       | checkpoint |

  Scenario Outline: Resolve to section keyword arguments in section python args
      Given a snakemake project
      Given I open a file "foo.smk" with text
      """
      <rule_like> NAME:
        <section>: kwd1="arg1"

      <rule_like> ANOTHER_NAME:
        input: <rule_like>s.NAME.<section>.kwd1
      """
      When I put the caret after NAME.<section>.kw
      Then reference should resolve to "kwd1" in "foo.smk"
      Examples:
        | section   | rule_like  |
        | input     | rule       |
        | output    | rule       |
        | resources | rule       |
        | log       | rule       |
        | input     | checkpoint |
        | output    | checkpoint |
        | resources | checkpoint |
        | log       | checkpoint |

  Scenario Outline: Resolve to section keyword arguments in run section python code
      Given a snakemake project
      Given I open a file "foo.smk" with text
      """
      <rule_like> NAME:
        <section>: kwd1="arg1"
        run:
            <section>.kwd1
      """
      When I put the caret after <section>.kw
      Then reference should resolve to "kwd1" in "foo.smk"
      Examples:
        | rule_like  | section   |
        | rule       | input     |
        | rule       | output    |
        | rule       | params    |
        | rule       | log       |
        | rule       | resources |
        | checkpoint | input     |
        | checkpoint | output    |
        | checkpoint | params    |
        | checkpoint | log       |
        | checkpoint | resources |

  Scenario Outline: Resolve to section keyword arguments for rules in top level
      Given a snakemake project
      Given I open a file "foo.smk" with text
      """
      <rule_like> NAME:
        <section>: kwd1="arg1"

      <rule_like>s.NAME.<section>.kwd1
      """
      When I put the caret after NAME.<section>.kw
      Then reference should resolve to "kwd1" in "foo.smk"
      Examples:
        | section   | rule_like  |
        | input     | rule       |
        | output    | rule       |
        | resources | rule       |
        | log       | rule       |
        | input     | checkpoint |
        | output    | checkpoint |
        | resources | checkpoint |
        | log       | checkpoint |

  Scenario Outline: Resolve of section indexes and args
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      output:
        "in1.txt",
        "in2.txt",
         arg1 = "in3.txt",
         arg2 = "in4.txt"

      <section>: "{output[<key>]}" #here1
      run:
          output[<key>] # here2
    """
    When I put the caret at <key><signature>
    Then <ref_type> should resolve to "<result>" in "foo.smk"

    Examples:
      | rule_like  | section | key | signature  | result    | ref_type               |
      | rule       | shell   | 0   | ]}" #here1 | "in1.txt" | reference in injection |
      | rule       | message | 1   | ]}" #here1 | "in2.txt" | reference in injection |
      | checkpoint | shell   | 2   | ]}" #here1 | arg1      | reference in injection |
      | checkpoint | message | 3   | ]}" #here1 | arg2      | reference in injection |
    # Not supported at the moment:
#      | rule       | shell   | 0   | ] # here2  | "in1.txt"   | reference              |
#      | rule       | shell   | 1   | ] # here2  | "in2.txt"   | reference              |
#      | checkpoint | shell   | 2   | ] # here2  | arg1        | reference              |
#      | checkpoint | shell   | 3   | ] # here2  | arg2        | reference              |

Scenario Outline: Unresolved index if out of bounds
     Given a snakemake project
     Given I open a file "foo.smk" with text
     """
     <rule_like> NAME:
       output:
         "in1.txt",
         "in2.txt",
          arg1 = "in3.txt",
          arg2 = "in4.txt"

       <section>: "{output[<key>]}" #here1
       run:
           output[<key>] # here2
     """
     When I put the caret at <key><signature>
     Then <ref_type> should not resolve
     Examples:
       | rule_like  | section | key | signature  | ref_type               |
       | rule       | shell   | -1  | ]}" #here1 | reference in injection |
       | rule       | message | 4   | ]}" #here1 | reference in injection |
       | rule       | message | 5   | ]}" #here1 | reference in injection |
       | rule       | message | 4   | ] # here2  | reference              |
       | checkpoint | shell   | 4   | ] # here2  | reference              |

  Scenario Outline: Unresolved index  when unpack or */**
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      output:
        <complicated_item>,
        "in2.txt",
         arg1 = "in3.txt",
         arg2 = "in4.txt"

      <section>: "{output[<key>]}" #here1
      run:
          output[<key>] # here2
    """
    When I put the caret at <key><signature>
    Then <ref_type> should not resolve

    Examples:
      | rule_like  | section | key | signature  | ref_type               | complicated_item |
      | rule       | shell   | 0   | ]}" #here1 | reference in injection | unpack(foo)      |
      | rule       | shell   | 1   | ]}" #here1 | reference in injection | **foo            |
      | rule       | shell   | 1   | ]}" #here1 | reference in injection | **foo()          |
      | checkpoint | message | 0   | ]}" #here1 | reference in injection | *foo             |
      | checkpoint | message | 0   | ]}" #here1 | reference in injection | *foo()           |
      | rule       | shell   | 0   | ] # here2  | reference              | unpack(foo)      |
      | rule       | shell   | 1   | ] # here2  | reference              | **foo            |
      | checkpoint | shell   | 2   | ] # here2  | reference              | *foo             |

  Scenario Outline: Resolve of section args inside py string
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      output:
        "in1.txt",
        "in2.txt",
         arg1 = "in3.txt",
         arg2 = "in4.txt"

      run:
          output<key> # here2
    """
    When I put the caret at <signature>
    Then reference should resolve to "<result>" in "foo.smk"

    Examples:
      | rule_like  | key                | signature | result |
      | rule       | ['arg1']           | arg1']    | arg1   |
      | rule       | .get('arg1')       | arg1')    | arg1   |
      | rule       | .get('arg1', None) | arg1',    | arg1   |
      | checkpoint | ['arg2']           | rg2']     | arg2   |
      | checkpoint | .get('arg1')       | arg1')    | arg1   |

    Scenario Outline: Resolve index in sections with 'multiext' function
      Given a snakemake project
      And I open a file "foo.smk" with text
      """
        def additional_func(a, b, c, d):
         return a + b + c + d

        rule NAME:
         <data_section>:
            <line1>,
            "file",
            <line3>,
            foo = "too"
         shell:
            <key>
        """
      When I put the caret at <signature>
      Then reference in injection should resolve to "<result>" in "foo.smk"
      Examples:
        | data_section | line1                    | line3                    | key           | signature | result                   |
        | input        | multiext("f.", "1", "2") | multiext("f.", "2", "1") | "{input[1]}"  | 1]}       | multiext("f.", "1", "2") |
        | input        | multiext("f.", "1", "2") | multiext("f.", "2", "1") | "{input[2]}"  | 2]}       | "file"                   |
        | input        | multiext("f.", "1", "2") | multiext("f.", "2", "1") | "{input[3]}"  | 3]}       | multiext("f.", "2", "1") |
        | input        | multiext("f.", "1", "2") | multiext("f.", "2", "1") | "{input[5]}"  | 5]}       | foo                      |
        | output       | additional_func(1,2,3,4) | multiext("f.", "2", "1") | "{output[0]}" | 0]}       | additional_func(1,2,3,4) |
        | output       | additional_func(1,2,3,4) | multiext("f.", "2", "1") | "{output[1]}" | 1]}       | "file"                   |
        | output       | additional_func(1,2,3,4) | multiext("f.", "2", "1") | "{output[2]}" | 2]}       | multiext("f.", "2", "1") |
        | output       | additional_func(1,2,3,4) | multiext("f.", "2", "1") | "{output[3]}" | 3]}       | multiext("f.", "2", "1") |
        | output       | additional_func(1,2,3,4) | multiext("f.", "2", "1") | "{output[4]}" | 4]}       | foo                      |