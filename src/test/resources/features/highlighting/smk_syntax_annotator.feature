Feature: Annotate additional syntax
  This is not for syntax errors highlighting

  Scenario Outline: Annotate toplevel args sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section>: <text>
    """
    Then I expect inspection info on <<section>> with message
    """
    PY.KEYWORD
    """
    When I check highlighting infos
    Examples:
      | section              | text     |
      | configfile           | "in.txt" |
      | workdir              | "dir"    |
      | wildcard_constraints | ""       |
      | report               | "r.txt"  |
      | singularity          | ""       |
      | ruleorder            | r1 > r2  |
      | localrules           | r1,r2    |

  Scenario Outline: Annotate rule-like section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        <section>: <text>
    """
    Then I expect inspection info on <<rule_like>> with message
    """
    PY.KEYWORD
    """
    Then I expect inspection info on <NAME> with message
    """
    PY.FUNC_DEFINITION
    """
    Then I expect inspection info on <<section>> with message
    """
    <highlighting>
    """
    When I check highlighting infos
    Examples:
      | rule_like   | section                  | text       | highlighting |
      | rule        | input                    | "file.txt" | PY.DECORATOR |
      | rule        | new_unrecognized_section | "file.txt" | PY.DECORATOR |
      | checkpoint  | input                    | "file.txt" | PY.DECORATOR |
      | checkpoint  | new_unrecognized_section | "file.txt" | PY.DECORATOR |
      | subworkflow | snakefile                | "file.txt" | PY.DECORATOR |
      | subworkflow | new_unrecognized_section | "file.txt" | PY.DECORATOR |

  Scenario Outline: Annotate Rules and Checkpoints
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        <section>: <text>
    """
    Then I expect inspection info on <<rule_like>> with message
    """
    PY.KEYWORD
    """
    Then I expect inspection info on <NAME> with message
    """
    PY.FUNC_DEFINITION
    """
    Then I expect inspection info on <<section>> with message
    """
    <highlighting>
    """
    When I check highlighting infos
    Examples:
      | rule_like  | section              | text       | highlighting             |
      | rule       | output               | "file.txt" | PY.DECORATOR             |
      | rule       | input                | "file.txt" | PY.DECORATOR             |
      | rule       | params               | "file.txt" | PY.DECORATOR             |
      | rule       | log                  | "file.txt" | PY.DECORATOR             |
      | rule       | resources            | foo        | PY.DECORATOR             |
      | rule       | version              | ""         | PY.DECORATOR             |
      | rule       | cache                | ""         | PY.DECORATOR             |
      | rule       | message              | ""         | PY.DECORATOR             |
      | rule       | threads              | ""         | PY.DECORATOR             |
      | rule       | singularity          | ""         | PY.DECORATOR             |
      | rule       | priority             | ""         | PY.DECORATOR             |
      | rule       | benchmark            | ""         | PY.DECORATOR             |
      | rule       | wildcard_constraints | ""         | PY.DECORATOR             |
      | rule       | group                | ""         | PY.DECORATOR             |
      | rule       | envmodules           | ""         | PY.DECORATOR             |
      | rule       | shadow               | ""         | PY.DECORATOR             |
      | rule       | conda                | ""         | PY.DECORATOR             |
      | rule       | cwl                  | ""         | PY.DECORATOR             |
      | rule       | script               | ""         | PY.DECORATOR             |
      | rule       | shell                | ""         | PY.DECORATOR             |
      | rule       | run                  | ""         | PY.PREDEFINED_DEFINITION |
      | rule       | wrapper              | ""         | PY.DECORATOR             |
      | rule       | name                 | ""         | PY.DECORATOR             |
      | rule       | handover             | ""         | PY.DECORATOR             |
      | checkpoint | output               | "file.txt" | PY.DECORATOR             |
      | checkpoint | run                  | ""         | PY.PREDEFINED_DEFINITION |

  Scenario Outline: Annotate Subworkflows
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    subworkflow NAME:
        <section>: <text>
    """
    Then I expect inspection info on <subworkflow> with message
    """
    PY.KEYWORD
    """
    Then I expect inspection info on <NAME> with message
    """
    PY.FUNC_DEFINITION
    """
    Then I expect inspection info on <<section>> with message
    """
    <highlighting>
    """
    When I check highlighting infos
    Examples:
      | section              | text       | highlighting             |
      | workdir              | "file.txt" | PY.DECORATOR             |
      | snakefile            | "file.txt" | PY.DECORATOR             |
      | configfile           | "file.txt" | PY.DECORATOR             |

  Scenario: Do not annotate keyword-like identifiers in run section
      Given a snakemake project
      Given I open a file "foo.smk" with text
      """
      rule foo:
          run:
              rule = 1
              checkpoints = 1
              subworkflow = 1

              configfile = 1
              workdir = 1
              wildcard_constraints = 1
              report = 1
              singularity = 1

              ruleorder  = 1
              localrules = 1

              onstart = 1
              onsuccess = 1
              onerror = 1
      """
      Then I expect inspection info on <rule> with message
      """
      PY.KEYWORD
      """
      Then I expect inspection info on <foo> with message
      """
      PY.FUNC_DEFINITION
      """
      Then I expect inspection info on <run> with message
      """
      PY.PREDEFINED_DEFINITION
      """
      # AND NO HIGHLIGHTING ON VARIABLES WITH KEYWORD LIKE NAMES!
      When I check highlighting infos

  Scenario Outline: Do not annotate keyword-like identifiers in py context
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    import <identifier>
    """
    # AND NO HIGHLIGHTING ON IDENTIFIERS WITH KEYWORD LIKE NAMES!
    Then I expect no inspection infos
    When I check highlighting infos
    Examples:
      | identifier           |
      | rule                 |
      | checkpoints          |
      | subworkflow          |
      | configfile           |
      | workdir              |
      | wildcard_constraints |
      | report               |
      | singularity          |
      | ruleorder            |
      | localrules           |
      | onstart              |
      | onsuccess            |
      | onerror              |