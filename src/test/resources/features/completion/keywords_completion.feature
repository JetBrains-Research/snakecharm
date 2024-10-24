Feature: Completion for snakemake keyword-like things

  Scenario Outline: Complete at top-level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    "foo"
    """
    When I put the caret at "foo"
    Then I invoke autocompletion popup, select "<item>" lookup item and see a text:
    """
    <item>: "foo"
    """
    Examples:
      | item                 |
      | configfile           |
      | singularity          |
      | include              |
      | workdir              |
      | wildcard_constraints |
      | scattergather        |
      | onsuccess            |
      | onstart              |
      | onerror              |
      | localrules           |
      | ruleorder            |
      | pepfile              |
      | pepschema            |
      | resource_scopes      |
      | conda                |

  Scenario Outline: Complete at top-level with respect to deprecations
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "2.0.0"
        introduced:
        - name: "container"
          type: "top-level"
        removed:
        - name: "singularity"
          type: "top-level"
        deprecated:
        - name: "wildcard_constraints"
          type: "top-level"

      - version: "1.0.0"
        deprecated:
        - name: "singularity"
          type: "top-level"
        removed:
        - name: "subworkflow"
          type: "top-level"
      - version: "0.0.1"
        introduced:
        - name: "wildcard_constraints"
          type: "top-level"
    """
    And I set snakemake language version to "<lang_version>"
    Given I open a file "foo.smk" with text
    """
    "foo"
    """
    When I put the caret at "foo"
    Then I invoke autocompletion popup
    Then completion list should contain these items with type text:
      | <expected_item> | <expected_item_type> |
    Then completion list shouldn't contain:
      | <missing_item> |
    Examples:
      | lang_version | expected_item        | expected_item_type              | missing_item |
      | 0.0.1        | singularity          | deprecated 1.0.0, removed 2.0.0 | xxx          |
      | 1.0.0        | singularity          | deprecated 1.0.0, removed 2.0.0 | subworkflow  |
      | 2.0.0        | container            | >=2.0.0                         | singularity  |
      | 0.0.0        | wildcard_constraints | >=0.0.1, deprecated 2.0.0       | xxx          |
      | 1.0.0        | wildcard_constraints | >=0.0.1, deprecated 2.0.0       | xxx          |
      | 2.0.0        | wildcard_constraints | >=0.0.1, deprecated 2.0.0       | xxx          |
      | 3.0.0        | wildcard_constraints | >=0.0.1, deprecated 2.0.0       | xxx          |

  Scenario: Complete report at top-level
    # We have 2 'report' in completion list, so prev scenario outline doesn't work  for 'report'
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    "foo"
    """
    When I put the caret at "foo"
    And I invoke autocompletion popup
    Then completion list should contain:
    | report |

  Scenario Outline: Complete rule-like at top-level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    "foo"
    """
    When I put the caret at "foo"
    Then I invoke autocompletion popup, select "<item>" lookup item and see a text:
    """
    <item> "foo"
    """
    Examples:
      | item       |
      | rule       |
      | checkpoint |
      | module     |

  Scenario Outline: Replace at toplevel
      Given a snakemake project
      Given I open a file "foo.smk" with text
      """
      roo: "foo"
      """
      When I put the caret at roo:
      Then I invoke autocompletion popup, select "<item>" lookup item in replace mode and see a text:
      """
      <item>: "foo"
      """
      Examples:
      | item                  |
      | configfile            |
      | singularity           |
      | include               |
      | workdir               |
      | wildcard_constraints  |
      | onsuccess             |
      | onstart               |
      | onerror               |
      | localrules            |
      | ruleorder             |

  Scenario: No smk keywords in py file
      Given a snakemake project
      Given I open a file "foo.py" with text
      """
      foo = 1;
      """
      When I put the caret after foo = 1;
      And I invoke autocompletion popup
      Then completion list shouldn't contain:
      | configfile            |
      | singularity           |
      | include               |
      | workdir               |
      | wildcard_constraints  |
      | onsuccess             |
      | onstart               |
      | onerror               |
      | localrules            |
      | ruleorder             |
      | rule                  |
      | checkpoint            |

  Scenario Outline: Complete at rule/checkpoint level after comma
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      output: "out.txt",
      <text>
    """
    When I put the caret at <text>
    And I invoke autocompletion popup
    Then completion list should contain:
       | input  |
       | output |
       | run    |
    Examples:
      | rule_like | text            |
      | rule      | # here          |
      | rule      | input: "in.txt" |
      | checkpoint | # here          |
      | checkpoint | input: "in.txt" |

  Scenario Outline: Complete at rule/checkpoint level after comma (args on new line)
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      output:
         "out.txt",
      <text>
    """
    When I put the caret at <text>
    And I invoke autocompletion popup
    Then completion list should contain:
      | input  |
      | output |
      | run    |
    Examples:
      | rule_like  | text            |
      | rule       | # here          |
      | rule       | input: "in.txt" |
      | checkpoint | # here          |
      | checkpoint | input: "in.txt" |

  Scenario: Complete and replace at subworkflow level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    subworkflow NAME:
      configfile: "in.txt"
    """
    When I put the caret at configfile
    Then I invoke autocompletion popup, select "snakefile" lookup item in replace mode and see a text:
    """
    subworkflow NAME:
      snakefile: "in.txt"
    """

  Scenario Outline: Complete at subworkflow level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    subworkflow NAME:
      <str>#here
    """
    When I put the caret at #here
    Then I invoke autocompletion popup and see a text:
    """
    subworkflow NAME:
      <result>: #here
    """
    Examples:
      | str | result               |
      | con | configfile           |
      | sna | snakefile            |
      | wor | workdir              |

  Scenario: Completion list at subworkflow level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    subworkflow a:
    """
    When I put the caret after subworkflow a:
    And I invoke autocompletion popup
    Then completion list should contain:
      | configfile |
      | snakefile  |
      | workdir    |
    And completion list shouldn't contain:
      | subworkflow |
      | rule        |

  Scenario Outline: Complete at rule/checkpoint level (single variant) before 8.0
    Given a snakemake:7.32.4 project
    And I set snakemake language version to "7.35.0"
    Given I open a file "foo.smk" with text
      """
      <rule_like> NAME:
        <str>#here
      """
    When I put the caret after <str>
    Then I invoke autocompletion popup and see a text:
      """
      <rule_like> NAME:
        <result>: #here
      """
    Examples:
      | rule_like  | str  | result         |
      | rule       | vers | version        |
      | checkpoint | vers | version        |

  Scenario Outline: Complete at rule/checkpoint level (single variant)
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <rule_like> NAME:
        <str>#here
      """
    When I put the caret after <str>
    Then I invoke autocompletion popup and see a text:
      """
      <rule_like> NAME:
        <result>: #here
      """
    Examples:
      | rule_like  | str  | result         |
      | rule       | inp  | input          |
      | checkpoint | inp  | input          |
      | rule       | out  | output         |
      | checkpoint | out  | output         |
      | rule       | par  | params         |
      | checkpoint | par  | params         |
      | rule       | be   | benchmark      |
      | checkpoint | be   | benchmark      |
      | rule       | cac  | cache          |
      | checkpoint | cac  | cache          |
      | rule       | mes  | message        |
      | checkpoint | mes  | message        |
      | rule       | pr   | priority       |
      | checkpoint | pr   | priority       |
      | rule       | gr   | group          |
      | checkpoint | gr   | group          |
      | rule       | envm | envmodules     |
      | checkpoint | envm | envmodules     |
      | rule       | cw   | cwl            |
      | checkpoint | cw   | cwl            |
      | rule       | sc   | script         |
      | checkpoint | sc   | script         |
      | rule       | run  | run            |
      | checkpoint | run  | run            |
      | rule       | wr   | wrapper        |
      | checkpoint | wr   | wrapper        |
      | rule       | na   | name           |
      | checkpoint | na   | name           |
      | rule       | han  | handover       |
      | checkpoint | han  | handover       |
      | rule       | defa | default_target |
      | checkpoint | defa | default_target |
      | rule       | retr | retries        |
      | rule       | retr | retries        |
      | rule       | loc  | localrule      |

  Scenario Outline: Complete at rule/checkpoint level (single variant) with trailing ws
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <rule_like> NAME:
        <str>
      """
    When I put the caret after <str>
    Then I invoke autocompletion popup and see a text:
      """
      <rule_like> NAME:
        <result>: 
      """
    Examples:
      | rule_like | str | result |
      | rule      | inp | input  |

  Scenario Outline: Complete at rule/checkpoint/module level (multiple variants) before 8.0
    Given a snakemake:7.32.4 project
    And I set snakemake language version to "7.35.0"
    Given I open a file "foo.smk" with text
      """
      <rule_like> NAME:
        <str>#here
      """
    When I put the caret at #here
    Then I invoke autocompletion popup, select "<result>" lookup item and see a text:
      """
      <rule_like> NAME:
        <result>: #here
      """
    Examples:
      | rule_like  | str | result               |
      | rule       | si  | singularity          |
      | checkpoint | si  | singularity          |

  Scenario Outline: Complete at rule/checkpoint/module level (multiple variants)
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <rule_like> NAME:
        <str>#here
      """
    When I put the caret at #here
    Then I invoke autocompletion popup, select "<result>" lookup item and see a text:
      """
      <rule_like> NAME:
        <result>: #here
      """
    Examples:
      | rule_like  | str | result               |
      | rule       | re  | resources            |
      | checkpoint | re  | resources            |
      | rule       | th  | threads              |
      | checkpoint | th  | threads              |
      | rule       | wi  | wildcard_constraints |
      | checkpoint | wi  | wildcard_constraints |
      | rule       | sh  | shadow               |
      | checkpoint | sh  | shadow               |
      | rule       | co  | conda                |
      | checkpoint | co  | conda                |
      | rule       | sh  | shell                |
      | checkpoint | sh  | shell                |
      | rule       | lo  | log                  |
      | checkpoint | lo  | log                  |
      | module     | c   | config               |
      | module     | m   | meta_wrapper         |
      | module     | s   | skip_validation      |
      | module     | r   | replace_prefix       |
      | module     | p   | prefix               |

  Scenario Outline: Complete at rule/checkpoint/module level with respect to deprecations
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "2.0.0"
        introduced:
        - name: "<introduced_key>"
          type: "<rule_like>"
        removed:
        - name: "<deprecated_key>"
          type: "<rule_like>"
        deprecated:
        - name: "threads"
          type: "rule"
        - name: "skip_validation"
          type: "module"

      - version: "1.0.0"
        deprecated:
        - name: "<deprecated_key>"
          type: "<rule_like>"
        removed:
        - name: "<removed_key>"
          type: "<rule_like_removed>"
      - version: "0.0.1"
        introduced:
        - name: "threads"
          type: "rule"
        - name: "skip_validation"
          type: "module"
    """
    And I set snakemake language version to "<lang_version>"
    Given I open a file "foo.smk" with text
      """
      <rule_like> NAME:
        #here
      """
    When I put the caret at #here
    Then I invoke autocompletion popup
    Then completion list should contain these items with type text:
      | <expected_item> | <expected_item_type> |
      Then completion list shouldn't contain:
        | <missing_item> |
    Examples:
      | rule_like  | rule_like_removed | lang_version | expected_item   | expected_item_type              | missing_item | deprecated_key | removed_key | introduced_key |
      | rule       | rule              | 1.0.0        | input           | deprecated 1.0.0, removed 2.0.0 | output       | input          | output      | log            |
      | rule       | checkpoint        | 1.0.0        | output          |                                 | xxx          | input          | output      | log            |
      | rule       | module            | 1.0.0        | input          |         deprecated 1.0.0, removed 2.0.0                        | output          | input          | output      | log            |
      | checkpoint | checkpoint        | 1.0.0        | input           | deprecated 1.0.0, removed 2.0.0 | output       | input          | output      | log            |
      | rule       | rule              | 2.0.0        | log             | >=2.0.0                         | input        | input          | output      | log            |
      | rule       | rule              | 1.0.0        | threads         | >=0.0.1, deprecated 2.0.0       | xxx          | input          | output      | log            |
      | rule       | rule              | 2.0.0        | threads         | >=0.0.1, deprecated 2.0.0       | xxx          | input          | output      | log            |
      | module     | module            | 1.0.0        | config          | deprecated 1.0.0, removed 2.0.0 | snakefile    | config         | snakefile   | meta_wrapper   |
      | module     | module            | 1.0.0        | skip_validation | >=0.0.1, deprecated 2.0.0       | snakefile    | input          | snakefile   | log            |
      | module     | module            | 2.0.0        | skip_validation | >=0.0.1, deprecated 2.0.0       | snakefile    | input          | snakefile   | log            |

  Scenario Outline: Completion in use declaration
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <given># here
    """
    When I put the caret at # here
    Then I invoke autocompletion popup, select "<choice>" lookup item and see a text:
    """
    <result> here
    """
    Examples:
      | given               | choice  | result                      |
      | u                   | use     | use rule #                  |
      | use r               | rule    | use rule #                  |
      | use rule * f        | from    | use rule * from #           |
      | use rule a as b w   | with    | use rule a as b with:#      |
      | use rule * from M e | exclude | use rule * from M exclude # |

  Scenario Outline: Complete and insert in use body
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      use rule NAME1 from <use_declaration> with:
        <str>#here
      """
    When I put the caret at <str>#here
    Then I invoke autocompletion popup
    And completion list should contain:
      | <result> |
    When I put the caret at #here
    Then I invoke autocompletion popup, select "<result>" lookup item and see a text:
      """
      use rule NAME1 from <use_declaration> with:
        <result>: #here
      """
    Examples:
      | use_declaration | str | result  |
      | MODULE as NAME2 | in  | input   |
      | MODULE as new*  | in  | input   |
      | MODULE          | in  | input   |
      | MODULE as NAME2 | ou  | output  |
      | MODULE as NAME2 | l   | log     |
      | MODULE as NAME2 | th  | threads |
      | MODULE as NAME2 | c   | conda   |

  Scenario Outline: Complete in use with respect to deprecations
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "2.0.0"
        introduced:
        - name: "log"
          type: "use"
        removed:
        - name: "input"
          type: "use"
        deprecated:
        - name: "threads"
          type: "use"

      - version: "1.0.0"
        deprecated:
        - name: "input"
          type: "use"
        removed:
        - name: "output"
          type: "use"
      - version: "0.0.1"
        introduced:
        - name: "threads"
          type: "use"
    """
    And I set snakemake language version to "<lang_version>"
    Given I open a file "foo.smk" with text
      """
      use rule NAME1 from MODULE as NAME2 with:
        #here
      """
    When I put the caret at #here
    Then I invoke autocompletion popup
    Then completion list should contain these items with type text:
      | <expected_item> | <expected_item_type> |
    Then completion list shouldn't contain:
      | <missing_item> |
    Examples:
      | lang_version | expected_item | expected_item_type              | missing_item |
      | 1.0.0        | input         | deprecated 1.0.0, removed 2.0.0 | output       |
      | 2.0.0        | log           | >=2.0.0                         | input        |
      | 1.0.0        | threads       | >=0.0.1, deprecated 2.0.0       | xxx          |
      | 2.0.0        | threads       | >=0.0.1, deprecated 2.0.0       | xxx          |

  Scenario: No rule execution sections in use body
    Given a snakemake project
    Given I open a file "foo.py" with text
    """
    use rule NAME1 from MODULE as NAME2 with:
      #here
    """
    When I put the caret at #here
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | run      |
      | shell    |
      | notebook |
      | script   |
      | cwl      |

  Scenario Outline: Complete and replace at rule/checkpoint level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      input: "in.txt"
    """
    When I put the caret at input
    Then I invoke autocompletion popup, select "output" lookup item in replace mode and see a text:
    """
    <rule_like> NAME:
      output: "in.txt"
    """
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Complete at rule/checkpoint level after comma
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      output: "out.txt",
      input: "in.txt"
    """
    When I put the caret at input
    And I invoke autocompletion popup
    Then completion list should contain:
      | input  |
      | output |
      | run    |
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Complete at rule/checkpoint section level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      output: "out.txt"
      input: "in.txt"
    """
    When I put the caret at "in
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | output     |
      | run        |
      | checkpoint |
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Do not show rule/checkpoint section keywords where not needed
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <rule_like> all:
        <text>
      """
    When I put the caret at <ptn>
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | message    |
      | run        |
    Examples:
      | rule_like  | text              | ptn   |
      | rule       | input: foo        | foo   |
      | rule       | input: foo, #here | #here |
      | rule       | input: foo.boo    | boo   |
      | rule       | run: foo          | foo   |
      | checkpoint | input: foo        | foo   |
      | checkpoint | input: foo, #here | #here |
      | checkpoint | run: foo          | foo   |

  Scenario Outline: Do not show subworkflow section keywords where not needed
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      subworkflow all:
        snakefile: <text>
      """
    When I put the caret at <ptn>
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | snakefile  |
      | configfile |
    Examples:
      | text       | ptn   |
      | foo        | foo   |
      | foo, #here | #here |
      | foo.boo    | boo   |

  Scenario: Do not show toplevel keywords in dot reference #97 completion
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rules.
    """
    When I put the caret after rules.
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | localrules |
      | configfile |
      | workdir    |
      | onstart    |

  Scenario Outline: Do not show toplevel keywords in rule like declarations
    Given a snakemake project
    Given I open a file "foo.smk" with text
     """
     <rule_like> all:
       <text>
     """
    When I put the caret at <ptn>
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | localrules |
      | onstart    |
    Examples:
      | rule_like   | text                 | ptn   |
      | rule        | input: foo           | foo   |
      | rule        | input: foo, #here    | #here |
      | rule        | input: foo.boo       | boo   |
      | rule        | run: foo             | foo   |
      | checkpoint  | input: foo           | foo   |
      | checkpoint  | input: foo, #here    | #here |
      | checkpoint  | input: foo.boo       | boo   |
      | checkpoint  | run: foo             | foo   |
      | subworkflow | workdir: foo         | foo   |
      | subworkflow | workdir: foo, #here  | #here |
      | subworkflow | workdir: foo.boo     | boo   |
      | module      | nakefile: foo        | foo   |
      | module      | nakefile: foo, #here | #here |
      | module      | nakefile: foo.boo    | boo   |

  Scenario Outline: Do not show toplevel keywords in workflow sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
     """
     <section>: #here
     """
    When I put the caret at #here
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | localrules |
      | onstart    |
      | ruleorder  |
    Examples:
      | section    |
      | include    |
      | configfile |

  Scenario Outline: Do not show toplevel keywords in python expressions
    Given a snakemake project
    Given I open a file "foo.smk" with text
     """
     <text>
     """
    When I put the caret at <ptn>
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | localrules |
      | onstart    |
    Examples:
      | text         | ptn     |
      | "" #here     | " #here |
      | import #here | #here   |
      | from #here   | #here   |

  Scenario Outline: Complete toplevel keywords in python blocks
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      if True:
        # here 1
      elif True:
        # here 2
      else:
        # here 3

      try:
        # here 4
      except:
        # here 5
      """
    When I put the caret at # here <ptn>
    And I invoke autocompletion popup
    Then completion list should contain:
      | localrules |
      | onstart    |
      | rule       |
      | checkpoint |
    Examples:
      | ptn |
      | 1   |
      | 2   |
      | 3   |
      | 4   |
      | 5   |

  Scenario Outline: Do not show items from toplevel completion in comments
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    # comment_toplevel
    <toplevel_section> foo: # comment_in_toplevel_section
      <section>: "_arg" # comment_subsection
    """
    When I put the caret at <signature>
    And I invoke autocompletion popup
    And completion list shouldn't contain:
      | checkpoint           |
      | configfile           |
      | rule                 |
      | subworkflow          |
      | onsuccess            |
      | onstart              |
      | onerror              |
      | wildcard_constraints |
      | localrules           |
      | ruleorder            |
      | configfile           |
      | include              |
      | workdir              |
      | singularity          |
      | container            |
      | containerized        |
    Examples:
      | signature                   | toplevel_section | section    |
      | comment_toplevel            | rule             | input      |
      | comment_toplevel            | rule             | output     |
      | comment_toplevel            | rule             | run        |
      | comment_toplevel            | checkpoint       | log        |
      | comment_in_toplevel_section | rule             | input      |
      | comment_in_toplevel_section | checkpoint       | log        |
      | comment_in_toplevel_section | subworkflow      | configfile |
      | comment_subsection          | rule             | input      |
      | comment_subsection          | checkpoint       | log        |
      | comment_subsection          | subworkflow      | configfile |


  Scenario Outline: Do not show items from rule-like or subworkflows completion in comments
    Given a snakemake project
    Given I open a file "foo.smk" with text
     """
     # comment_toplevel
     <toplevel_section> foo: # comment_in_toplevel_section
       <section>: "_arg" # comment_subsection
     """
    When I put the caret at <signature>
    And I invoke autocompletion popup
    And completion list shouldn't contain:
      | input         |
      | output        |
      | log           |
      | container     |
      | containerized |
      | configfile    |
      | workdir       |
    Examples:
      | signature                   | toplevel_section | section    |
      | comment_toplevel            | rule             | input      |
      | comment_toplevel            | rule             | output     |
      | comment_toplevel            | rule             | run        |
      | comment_toplevel            | checkpoint       | log        |
      | comment_in_toplevel_section | rule             | input      |
      | comment_in_toplevel_section | checkpoint       | log        |
      | comment_in_toplevel_section | subworkflow      | configfile |
      | comment_subsection          | rule             | input      |
      | comment_subsection          | checkpoint       | log        |
      | comment_subsection          | subworkflow      | configfile |
