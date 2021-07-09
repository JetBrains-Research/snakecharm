Feature: Completion for section args after section name

  Scenario Outline: Complete in shell section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> aaaa:
      input: "path/to/input"
      output: "path/to/output"
      params:
        outdir=lambda wildcards, output: os.path.dirname(str(output)),
        xmx=lambda wildcards: str(800 // int(wildcards.bin))
      shell: "command --workdir {params.outdir}"
    """
    When I put the caret after {params.
    And I invoke autocompletion popup
    Then completion list should contain:
      | outdir      |
      | xmx         |
    Examples:
      | rule_like   |
      | rule        |
      | checkpoint  |

  Scenario Outline: Not completed in wrapper section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> aaaa:
      input: "path/to/input"
      output: "path/to/output"
      params:
        outdir=lambda wildcards, output: os.path.dirname(str(output)),
        xmx=lambda wildcards: str(800 // int(wildcards.bin))
      wrapper: "wrapper {params.outdir}"
    """
    When I put the caret after {params.
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | outdir      |
      | xmx         |
    Examples:
      | rule_like   |
      | rule        |
      | checkpoint  |

  Scenario Outline: no values for unnamed parameters in the completion list
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> aaaa:
      input: "path/to/input"
      output: "path/to/output"
      params:
        1,
        outdir=lambda wildcards, output: os.path.dirname(str(output)),
        xmx=lambda wildcards: str(800 // int(wildcards.bin))
      shell: "command {params.outdir}"
    """
    When I put the caret after {params.
    And I invoke autocompletion popup
    Then completion list should contain:
      | outdir      |
      | xmx         |
    And completion list shouldn't contain:
      | 1           |
    Examples:
      | rule_like   |
      | rule        |
      | checkpoint  |

  Scenario Outline: Completed in run section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      params: 0, a=1, b=2
      run:
        shell("echo {params.a}")
    """
    When I put the caret after {params.
    And I invoke autocompletion popup
    Then completion list should contain:
      | a      |
      | b      |
    And completion list shouldn't contain:
      | 0      |
    Examples:
      | rule_like   |
      | rule        |
      | checkpoint  |

  Scenario Outline: Not completed in calls to other functions in run section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      params: 0, a=1, b=2
      run:
        wrapper("path/to/wrapper{params.a}.py")
    """
    When I put the caret after {params.
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | a      |
      | b      |
      | 0      |
    Examples:
      | rule_like   |
      | rule        |
      | checkpoint  |

  Scenario Outline: Completion for various sections in shell section
      Given a snakemake project
      Given I open a file "foo.smk" with text
      """
      <rule_like> rule1:
        <section>: kwd1="arg1", kwd2="arg2"
        shell: "{<section>.}"
      """
      When I put the caret after {<section>.
      And I invoke autocompletion popup
      Then completion list should contain:
        | kwd1 |
        | kwd2 |
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

  Scenario Outline: Completion for section keywords in subscription expression
      Given a snakemake project
      Given I open a file "foo.smk" with text
      """
      <rule_like> rule1:
        <section>: kwd1="arg1", kwd2="arg2"
        shell: "{<section>[]}"
      """
      When I put the caret after {<section>[
      And I invoke autocompletion popup
      Then completion list should contain:
        | kwd1 |
        | kwd2 |
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

  Scenario Outline: Completion for section keyword arguments in run section python code
      Given a snakemake project
      Given I open a file "foo.smk" with text
      """
      <rule_like> NAME:
        <section>: kwd1="arg1", kwd2="arg2"
        <other_section>: kwd3="arg3"
        run:
            <section>.smth
      """
      When I put the caret after <section>.
      And I invoke autocompletion popup
      Then completion list should contain:
        | kwd1 |
        | kwd2 |
      And completion list shouldn't contain:
        | kwd3 |
    Examples:
      | rule_like  | section   | other_section |
      | rule       | input     | log           |
      | rule       | output    | log           |
      | rule       | params    | log           |
      | rule       | log       | input         |
      | rule       | resources | log           |
      | checkpoint | input     | log           |
      | checkpoint | output    | log           |
      | checkpoint | params    | log           |
      | checkpoint | log       | input         |
      | checkpoint | resources | log           |

  Scenario Outline: Completion of section indexes and args
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      output:
        "in1.txt",
        "in2.txt",
         arg1 = "in3.txt",
         arg2 = "in4.txt"

      <section>: "{output[]}" #here1
      run:
          output[] # here2
    """
    When I put the caret at <signature>
    And I invoke autocompletion popup
    Then completion list should contain:
      | 0 |
      | 1 |
      | 2 |
      | 3 |
      | <q>arg1<q> |
      | <q>arg2<q> |
    And completion list shouldn't contain:
      | 4 |

  Examples:
    | rule_like  | section | signature  | q |
    | rule       | shell   | ]}" #here1 |   |
    | rule       | shell   | ] # here2  | ' |
    | rule       | message | ]}" #here1 |   |
    | checkpoint | shell   | ]}" #here1 |   |
    | checkpoint | shell   | ] # here2  | ' |

  Scenario Outline: Completion of section args inside python string in subscription
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      output:
        "in1.txt",
         arg1 = "in1.txt",
         arg2 = "in2.txt"
      run:
          output[<q><key><q>] # here1
    """
    When I put the caret at <q>]
    And I invoke autocompletion popup
    Then completion list should contain:
      | arg1 |
      | arg2 |
    And completion list shouldn't contain:
      | 0 |
      | 1 |
      | 2 |

    Examples:
      | rule_like  | key | q |
      | rule       |     | ' |
      | rule       |     | " |
      | rule       | ar  | ' |
      | rule       | ar  | " |
      | checkpoint |     | ' |
      | checkpoint | ar  | ' |

  Scenario Outline: Completion of section args inside python string in get()
      Given a snakemake project
      Given I open a file "foo.smk" with text
      """
      <rule_like> NAME:
        output:
          "in1.txt",
           arg1 = "in1.txt",
           arg2 = "in2.txt"
        run:
            output<text> # here1
      """
      When I put the caret at <signature>
      And I invoke autocompletion popup
      Then completion list should contain:
        | arg1 |
        | arg2 |
      And completion list shouldn't contain:
        | 0 |
        | 1 |
        | 2 |

      Examples:
        | rule_like  | text | signature |
        | rule       | .get('')        | ')        |
        | rule       | .get("")        | ")        |
        | rule       | .get('a')       | ')        |
        | rule       | .get('a', None) | ', None)  |
        | checkpoint | .get('')        | ')        |

  Scenario Outline: Completion of section args and indexes names after dot
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      output:
        "in1.txt",
        "in2.txt",
         arg1 = "in3.txt",
         arg2 = "in4.txt"

      <section>: "{output.foo}" #here1
      run:
          output.foo # here2
    """
    When I put the caret at <signature>
    And I invoke autocompletion popup
    Then completion list should contain:
      | arg1 |
      | arg2 |
    And completion list shouldn't contain:
      | 0 |
      | 1 |
      | 2 |
      | 3 |
      | 4 |

  Examples:
    | rule_like  | section   | signature |
    | rule       | shell   | foo}" #here1 |
    | rule       | shell   | foo # here2  |
    | rule       | message | foo}" #here1 |
    | checkpoint | shell   | foo}" #here1 |
    | checkpoint | shell   | foo # here2  |

  Scenario Outline: Completion of section args when unpack or */**
      Given a snakemake project
      Given I open a file "foo.smk" with text
      """
      <rule_like> NAME:
        output:
          <complicated_item>,
          "in1.txt",
           arg1 = "in2.txt",
           arg2 = "in3.txt"

        <section>: "{output[]}" #here1
        run:
            output[] # here2
      """
      When I put the caret at <signature>
      And I invoke autocompletion popup
      Then completion list should contain:
        | <q>arg1<q> |
        | <q>arg2<q> |
      And completion list shouldn't contain:
        | 0 |
        | 1 |
        | 2 |
        | 3 |
        | 4 |

    Examples:
      | rule_like  | section | complicated_item | signature  | q |
      | rule       | shell   | unpack(foo)      | ]}" #here1 |   |
      | rule       | message | **foo            | ]}" #here1 |   |
      | rule       | message | **foo()          | ]}" #here1 |   |
      | checkpoint | shell   | *foo             | ]}" #here1 |   |
      | checkpoint | shell   | *foo()           | ]}" #here1 |   |
      | rule       | shell   | unpack(foo)      | ] # here2  | ' |
      | rule       | shell   | **foo            | ] # here2  | ' |
      | checkpoint | shell   | *foo             | ] # here2  | ' |

  Scenario Outline: Insert handler for py subscription elements
      Given a snakemake project
      Given I open a file "foo.smk" with text
      """
      rule NAME:
        output:
          "in1.txt",
           arg1 = "in1.txt",
           arg2 = "in2.txt"
        run:
            output<key> # here
      """
      When I put the caret at <signature>
      Then I invoke autocompletion popup, select "<item>" lookup item in <mode> mode and see a text:
      """
      rule NAME:
        output:
          "in1.txt",
           arg1 = "in1.txt",
           arg2 = "in2.txt"
        run:
            output<result> # here
      """
    Examples:
      | key         | signature     | item   | mode    | result        |
      | ['']        | '] # here     | arg1   | normal  | ['arg1']      |
      | ['']        | '] # here     | arg1   | replace | ['arg1']      |
      | [""]        | "] # here     | arg1   | normal  | ["arg1"]      |
      | [""]        | "] # here     | arg1   | replace | ["arg1"]      |
      | ['ar']      | '] # here     | arg1   | normal  | ['arg1']      |
      | ['ar']      | '] # here     | arg1   | replace | ['arg1']      |
      | ["ar"]      | "] # here     | arg1   | normal  | ["arg1"]      |
      | ["ar"]      | "] # here     | arg1   | replace | ["arg1"]      |
      | ['ar']      | ar'] # here   | arg1   | normal  | ['arg1ar']    |
      | ['ar']      | ar'] # here   | arg1   | replace | ['arg1']      |
      | ['arfoo']   | foo'] # here  | arg1   | normal  | ['arg1foo']   |
      | ['arfoo']   | foo'] # here  | arg1   | replace | ['arg1']      |
      | ["arfoo"]   | foo"] # here  | arg1   | normal  | ["arg1foo"]   |
      | ["arfoo"]   | foo"] # here  | arg1   | replace | ["arg1"]      |
      | ['ar']      | 'ar'] # here  | 'arg1' | normal  | ['arg1''ar']  |
      | ['ar']      | 'ar'] # here  | 'arg1' | replace | ['arg1']      |
      | ['foo']     | 'foo'] # here | 'arg1' | normal  | ['arg1''foo'] |
      | ['foo']     | 'foo'] # here | 'arg1' | replace | ['arg1']      |
      | ['foo']     | 'foo'] # here | 0      | normal  | [0'foo']      |
      | ['foo']     | 'foo'] # here | 0      | replace | [0]           |
      | [123]       | 123] # here   | 0      | normal  | [0123]        |
      | [123]       | 123] # here   | 0      | replace | [0]           |
      | .get('')    | ') # here     | arg1   | normal  | .get('arg1')  |
      | .get('foo') | foo') # here  | arg1   | replace | .get('arg1')  |
      | .foo        | foo           | arg1   | normal  | .arg1foo      |
      | .foo        | foo           | arg1   | replace | .arg1         |

  Scenario Outline: Insert handler for smksl subscription elements
        Given a snakemake project
        Given I open a file "foo.smk" with text
        """
        rule NAME:
          output:
            "in1.txt",
             arg1 = "in1.txt",
             arg2 = "in2.txt"
          shell:
              "{output<key>}"
        """
        When I put the caret at <signature>
        Then I invoke autocompletion popup, select "<item>" lookup item in <mode> mode and see a text:
        """
        rule NAME:
          output:
            "in1.txt",
             arg1 = "in1.txt",
             arg2 = "in2.txt"
          shell:
              "{output<result>}"
        """
      Examples:
        | key     | signature | item | mode    | result    |
        | []      | ]}        | arg1 | normal  | [arg1]    |
        | []      | ]}        | arg1 | replace | [arg1]    |
        | [ar]    | ]}        | arg1 | normal  | [arg1]    |
        | [ar]    | ]}        | arg1 | replace | [arg1]    |
        | [ar]    | ar]}      | arg1 | normal  | [arg1ar]  |
        | [ar]    | ar]}      | arg1 | replace | [arg1]    |
        | [arfoo] | foo]}     | arg1 | normal  | [arg1foo] |
        | [arfoo] | foo]}     | arg1 | replace | [arg1]    |
        | [foo]   | foo]}     | arg1 | normal  | [arg1foo] |
        | [foo]   | foo]}     | arg1 | replace | [arg1]    |
        | [foo]   | foo]}     | 0    | normal  | [0foo]    |
        | [foo]   | foo]}     | 0    | replace | [0]       |
        | [123]   | 123]}     | 0    | normal  | [0123]    |
        | [123]   | 123]}     | 0    | replace | [0]       |
        | [123]   | 123]}     | 0    | replace | [0]       |
        | .foo    | foo       | arg1 | normal  | .arg1foo  |
        | .foo    | foo       | arg1 | replace | .arg1     |

    #TODO: contributor in foo<here>.bood.doo[aa] ?

  Scenario Outline: Completion for input/output sections with 'multiext' function in 'shell' and 'run' sections
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
            foo = "ooo.txt"
         <exec_section>:
            <key>
        """
    When I put the caret after <signature>
    And I invoke autocompletion popup
    Then completion list should contain:
      | 0 |
      | 1 |
      | 2 |
      | 3 |
      | 4 |
      | 5 |
    And completion list shouldn't contain:
      | 6 |
      | 7 |
    Examples:
      | data_section | line1                    | line3                         | exec_section | key          | signature |
      | input        | multiext("f.", "1", "2") | multiext("f.", "1", "2")      | shell        | "{input[]}"  | t[        |
      | input        | multiext("f.", "1", "2") | multiext("f.", "1", "2")      | run          | input[]      | t[        |
      | output       | additional_func(1,2,3,4) | multiext("f.", "1", "2", "5") | shell        | "{output[]}" | t[        |
      | output       | additional_func(1,2,3,4) | multiext("f.", "1", "2", "5") | run          | output[]     | t[        |

  Scenario Outline: Completion for input/output sections with 'multiext' function in 'shell' and 'run' sections (item presentation)
    Given a snakemake project
    And I open a file "foo.smk" with text
        """
        rule NAME:
         <data_section>:
            "in.txt",
            "in/ver/very/yyyyyyyyyy/llllllll/long/name.txt",
            some_call1(call, 1,2,3, other, args),
            some_call2("in/ver/very/yyyyyyyyyy/llllllll/long/name.txt",),
            multiext("file", ".txt", ".log"),
            "f.txt",
            multiline_call(
                call, 1,2,3, other,
                args
            ),
            multiext(call("f"), ".1", ".2"),
            "last.file",
            foo=111,
            boo=222,
         <exec_section_text>
        """
    When I put the caret after t[
    And I invoke autocompletion popup
    Then completion list should contain:
      | <escaping>boo<escaping> | null                   | '<data_section>:' keyword arg  |
      | <escaping>foo<escaping> | null                   | '<data_section>:' keyword arg  |
      | 0                       | (in.txt)               | '<data_section>:' position arg |
      | 1                       | (...lll/long/name.txt) | '<data_section>:' position arg |
      | 2                       | (...2,3, other, args)) | '<data_section>:' position arg |
      | 3                       | (.../long/name.txt",)) | '<data_section>:' position arg |
      | 4                       | (file.txt)             | '<data_section>:' position arg |
      | 5                       | (file.log)             | '<data_section>:' position arg |
      | 6                       | (f.txt)                | '<data_section>:' position arg |
      | 7                       | (...       args    ))  | '<data_section>:' position arg |
      | 8                       | (.1)                   | '<data_section>:' position arg |
      | 9                       | (.2)                   | '<data_section>:' position arg |
      | 10                      | (last.file)            | '<data_section>:' position arg |
      | 11                      | (111)                  | '<data_section>:' position arg |
      | 12                      | (222)                  | '<data_section>:' position arg |
    Examples:
      | data_section | exec_section_text   | escaping |
      | input        | shell: "{input[]}"  |         |
      | input        | run: input[]        | '       |
      | output       | shell: "{output[]}" |         |
      | output       | run: output[]       | '       |