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

# output[0] for all ars + [IndexError: list index out of range] + inspection on IE? ]