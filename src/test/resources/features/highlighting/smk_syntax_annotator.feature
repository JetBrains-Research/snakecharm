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
    SMK_KEYWORD
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
    SMK_KEYWORD
    """
    Then I expect inspection info on <NAME> with message
    """
    SMK_FUNC_DEFINITION
    """
    Then I expect inspection info on <<section>> with message
    """
    <highlighting>
    """
    When I check highlighting infos
    Examples:
      | rule_like   | section                  | text       | highlighting  |
      | rule        | input                    | "file.txt" | SMK_DECORATOR |
      | rule        | new_unrecognized_section | "file.txt" | SMK_DECORATOR |
      | checkpoint  | input                    | "file.txt" | SMK_DECORATOR |
      | checkpoint  | new_unrecognized_section | "file.txt" | SMK_DECORATOR |
      | subworkflow | snakefile                | "file.txt" | SMK_DECORATOR |
      | subworkflow | new_unrecognized_section | "file.txt" | SMK_DECORATOR |

  Scenario Outline: Annotate Rules and Checkpoints
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        <section>: <text>
    """
    Then I expect inspection info on <<rule_like>> with message
    """
    SMK_KEYWORD
    """
    Then I expect inspection info on <NAME> with message
    """
    SMK_FUNC_DEFINITION
    """
    Then I expect inspection info on <<section>> with message
    """
    <highlighting>
    """
    When I check highlighting infos
    Examples:
      | rule_like  | section              | text       | highlighting             |
      | rule       | output               | "file.txt" | SMK_DECORATOR            |
      | rule       | input                | "file.txt" | SMK_DECORATOR            |
      | rule       | params               | "file.txt" | SMK_DECORATOR            |
      | rule       | log                  | "file.txt" | SMK_DECORATOR            |
      | rule       | resources            | foo        | SMK_DECORATOR            |
      | rule       | version              | ""         | SMK_DECORATOR            |
      | rule       | cache                | ""         | SMK_DECORATOR            |
      | rule       | message              | ""         | SMK_DECORATOR            |
      | rule       | threads              | ""         | SMK_DECORATOR            |
      | rule       | singularity          | ""         | SMK_DECORATOR            |
      | rule       | priority             | ""         | SMK_DECORATOR            |
      | rule       | benchmark            | ""         | SMK_DECORATOR            |
      | rule       | wildcard_constraints | ""         | SMK_DECORATOR            |
      | rule       | group                | ""         | SMK_DECORATOR            |
      | rule       | envmodules           | ""         | SMK_DECORATOR            |
      | rule       | shadow               | ""         | SMK_DECORATOR            |
      | rule       | conda                | ""         | SMK_DECORATOR            |
      | rule       | cwl                  | ""         | SMK_DECORATOR            |
      | rule       | script               | ""         | SMK_DECORATOR            |
      | rule       | shell                | ""         | SMK_DECORATOR            |
      | rule       | run                  | ""         | PY.PREDEFINED_DEFINITION |
      | rule       | wrapper              | ""         | SMK_DECORATOR            |
      | rule       | name                 | ""         | SMK_DECORATOR            |
      | rule       | handover             | ""         | SMK_DECORATOR            |
      | rule       | default_target       | ""         | SMK_DECORATOR            |
      | rule       | retries              | ""         | SMK_DECORATOR            |
      | rule       | template_engine      | ""         | SMK_DECORATOR            |
      | checkpoint | output               | "file.txt" | SMK_DECORATOR            |
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
    SMK_KEYWORD
    """
    Then I expect inspection info on <NAME> with message
    """
    SMK_FUNC_DEFINITION
    """
    Then I expect inspection info on <<section>> with message
    """
    <highlighting>
    """
    When I check highlighting infos
    Examples:
      | section              | text       | highlighting             |
      | workdir              | "file.txt" | SMK_DECORATOR             |
      | snakefile            | "file.txt" | SMK_DECORATOR             |
      | configfile           | "file.txt" | SMK_DECORATOR             |

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
      SMK_KEYWORD
      """
      Then I expect inspection info on <foo> with message
      """
      SMK_FUNC_DEFINITION
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

  Scenario: 'use' section highlighting
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    use rule A as B with:
        input: "data"
    """
    Then I expect inspection info on <use> with message
    """
    SMK_KEYWORD
    """
    Then I expect inspection info on <rule> with message
    """
    SMK_KEYWORD
    """
    Then I expect inspection info on <as> with message
    """
    SMK_KEYWORD
    """
    Then I expect inspection info on <B> with message
    """
    SMK_FUNC_DEFINITION
    """
    Then I expect inspection info on <with> with message
    """
    SMK_KEYWORD
    """
    Then I expect inspection info on <input> with message
    """
    SMK_DECORATOR
    """
    When I check highlighting infos

  Scenario: 'use' section highlighting, part 2
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    use rule * from module exclude xxx as other_*
    """
    Then I expect inspection info on <use> with message
    """
    SMK_KEYWORD
    """
    Then I expect inspection info on <rule> with message
    """
    SMK_KEYWORD
    """
    Then I expect inspection info on <from> with message
    """
    SMK_KEYWORD
    """
    Then I expect inspection info on <exclude> with message
    """
    SMK_KEYWORD
    """
    Then I expect inspection info on <as> with message
    """
    SMK_KEYWORD
    """
    Then I expect inspection info on <other_*> with message
    """
    SMK_FUNC_DEFINITION
    """
    When I check highlighting infos