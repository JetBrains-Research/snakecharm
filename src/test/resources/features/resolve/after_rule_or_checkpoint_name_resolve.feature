Feature: Resolve for section after rule/checkpoint name e.g. rules.NAME.input

  Scenario Outline: Resolve to section at top level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input: ""

    <rule_like>s.NAME.input
    """
    When I put the caret after <rule_like>s.NAME.inp
    Then reference should resolve to "input" in "foo.smk"
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Resolve to section at section level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input: ""
        run: <rule_like>s.NAME.input
    """
    When I put the caret after <rule_like>s.NAME.inp
    Then reference should resolve to "input" in "foo.smk"
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Resolve to section in injection
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input: ""
        shell: "{<rule_like>s.NAME.input}"
    """
    When I put the caret after <rule_like>s.NAME.inp
    Then reference in injection should resolve to "input" in "foo.smk"
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Resolve for section's keyword arguments in injection
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

  Scenario Outline: Resolve for section's keyword arguments for rules in section
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

  Scenario Outline: Resolve to section's keyword arguments for rules in top level
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

  Scenario Outline: Resolve to section's keyword arguments in subscription expression
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

  Scenario Outline: No resolve for unavailable sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
         input: ""
         version: 1
         wrapper: ""
         wildcard_constraints: a=""
         conda: ""
         message: ""
         threads: 1
         benchmark: ""
         shadow: "full"
         output: ""
         group: ""
         envmodules: ""
         singularity: ""
         cwl: ""
         log: ""
         params: a=""
         priority: 1
         resources: a=""
         name: "new_rule_name"
         script: ""
         shell: ""
         cache: True
         run:

     rule ANOTHER_NAME:
        input: rules.NAME.<section>
    """
    When I put the caret after rules.NAME.
    Then reference should not resolve
    Examples:
      | section     |
      | conda       |
      | shell       |
      | threads     |
      | shadow      |
      | group       |
      | envmodules  |
      | singularity |
      | cwl         |
      | run         |
      | script      |
      | cache       |
      | name        |

  Scenario Outline: Resolve for available sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
         input: ""
         version: 1
         wrapper: ""
         wildcard_constraints: a=""
         conda: ""
         message: ""
         threads: 1
         benchmark: ""
         shadow: "full"
         output: ""
         group: ""
         envmodules: ""
         singularity: ""
         cwl: ""
         log: ""
         params: a=""
         priority: 1
         resources: a=""
         name: "new_rule_name"
         script: ""
         shell: ""
         run:

     rule ANOTHER_NAME:
        input: rules.NAME.<section>
    """
    When I put the caret after rules.NAME.
    Then reference should resolve to "<section>" in "foo.smk"
    Examples:
      | section              |
      | input                |
      | version              |
      | wrapper              |
      | wildcard_constraints |
      | message              |
      | benchmark            |
      | output               |
      | log                  |
      | params               |
      | priority             |
      | resources            |