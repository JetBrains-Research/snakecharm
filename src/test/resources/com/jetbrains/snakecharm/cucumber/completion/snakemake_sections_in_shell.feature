Feature: Completion for params in shell section
  Complete params section arguments in shell section

  Scenario Outline: Complete for params in shell section
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

  Scenario Outline: No completion for various sections in wrapper section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> rule1:
      <section>: kwd1="arg1", kwd2="arg2"
      wrapper: "{<section>.}"
    """
    When I put the caret after {<section>.
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
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

  Scenario Outline: Completion for various sections in shell function call from run section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> rule1:
      <section>: kwd1="arg1", kwd2="arg2"
      run:
        shell("echo {<section>.}")
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

  Scenario Outline: No completion for various sections in wrapper function call from run section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> rule1:
      <section>: kwd1="arg1", kwd2="arg2"
      run:
        wrapper("echo {<section>.}")
    """
    When I put the caret after {<section>.
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
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


  Scenario Outline: completion for sections without arguments
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      input: fa="text"
      threads: 4
      shell: "command {}"
    """
    When I put the caret after command {
    And I invoke autocompletion popup
    Then completion list should contain:
      | input   |
      | threads |
    And completion list shouldn't contain:
      | output               |
      | version              |
      | log                  |
      | resources            |
      | params               |
      | shadow               |
      | group                |
      | singularity          |
      | wildcard_constraints |
      | conda                |
      | benchmark            |
      | message              |
      | priority             |
      | run                  |
      | script               |
      | shell                |
      | wrapper              |
      | cwl                  |
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |


  Scenario Outline: no completion for dot after sections not allowing or having keyword arguments
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      input: fa="text"
      output: "no_keyword_arguments.txt"
      threads: 4
      version: 2
      shell: "command {<text>.}"
    """
    When I put the caret after command {<text>.
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | threads   |
      | version   |
      | input     |
      | output    |
      | resources |
      | params    |
    Examples:
      | text    |
      | threads |
      | version |

  Scenario Outline: completion for incomplete section names in shell section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      <section>: fa="text"
      shell: "{<substring>}"
    """
    When I put the caret after "{<substring>
    Then I invoke autocompletion popup and see a text:
    """
    <rule_like> NAME:
      <section>: fa="text"
      shell: "{<section>}"
    """
    Examples:
      | rule_like  | section   | substring |
      | rule       | input     | inp       |
      | rule       | output    | out       |
      | rule       | resources | res       |
      | rule       | params    | par       |
      | rule       | threads   | thr       |
      | rule       | version   | ver       |
      | rule       | log       | lo        |
      | checkpoint | input     | inp       |
      | checkpoint | output    | out       |
      | checkpoint | resources | res       |
      | checkpoint | params    | par       |
      | checkpoint | threads   | thr       |
      | checkpoint | version   | ver       |
      | checkpoint | log       | lo        |

  Scenario Outline: completion for incomplete section names in run section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      <section>: fa="text"
      run:
        shell("{<substring>}")
    """
    When I put the caret after "{<substring>
    Then I invoke autocompletion popup and see a text:
    """
    <rule_like> NAME:
      <section>: fa="text"
      run:
        shell("{<section>}")
    """
    Examples:
      | rule_like  | section   | substring |
      | rule       | input     | inp       |
      | rule       | output    | out       |
      | rule       | resources | res       |
      | rule       | params    | par       |
      | rule       | threads   | thr       |
      | rule       | version   | ver       |
      | rule       | log       | lo        |
      | checkpoint | input     | inp       |
      | checkpoint | output    | out       |
      | checkpoint | resources | res       |
      | checkpoint | params    | par       |
      | checkpoint | threads   | thr       |
      | checkpoint | version   | ver       |
      | checkpoint | log       | lo        |