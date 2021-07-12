Feature: Tests on snakemake string language injection

  Scenario Outline: Injection for different quotes
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      shell: <quote>{input}<quote>
    """
    When I put the caret after input
    Then I expect language injection on "{input}"
    Examples:
      | quote |
      | "     |
      | '     |
      | """   |

  Scenario Outline: Ordinary injection for rule/checkpoint
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section> NAME:
      shell: "{input}"
    """
    When I put the caret after input
    Then I expect language injection on "{input}"
    Examples:
      | section      |
      | rule         |
      | checkpoint   |

  Scenario: No injection in fstrings with unescaped brackets
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      shell: f"{input}"
    """
    When I put the caret after input
    Then I expect no language injection

  Scenario: Injection in fstrings with escaped brackets
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      shell: f"{{input}}"
    """
    When I put the caret after input
    Then I expect language injection on "{{input}}"

  Scenario: No injection in lambdas
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule:
      output: "foo"
      log: lambda wd, output: "{output}.log"
    """
    When I put the caret after {output
    Then I expect no language injection

   Scenario: Allow injection in lambdas in expand call
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule:
      output: "foo"
      log: lambda wd: expand("{output}.log", output="out.txt")
    """
    When I put the caret after {output
     Then I expect language injection on "{output}.log"

  Scenario: Injection in split string literal
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      shell: "somecommand" "{wildcards.group}"  f'{10}' "{output}"
    """
    When I put the caret after somecommand
    Then I expect language injection on "somecommand{wildcards.group}{output}"

  Scenario: No injection for ordinary string literals
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    str = "NAME"
    """
    When I put the caret after NAME
    Then I expect no language injection

  Scenario: No injection in ordinary function calls
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    def f(s)
      return s

    rule NAME:
      output: f("{foo}")
    """
    When I put the caret after foo
    Then I expect no language injection

  Scenario Outline: No injection in strings without braces
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      input: "<content>"#here
    """
    When I put the caret at "#here
    Then I expect no language injection
    Examples:
    | content |
    | text    |
    | text }  |

  Scenario Outline: Injection in empty strings
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      input: <quote><quote>#here
    """
    When I put the caret at <quote>#here
    Then I expect language injection on ""
    Examples:
      | quote |
      | '    |
      | "    |

  Scenario Outline: No injection in top-level workflow sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section>: "{foo}"
    """
    When I put the caret after foo
    Then I expect no language injection
    Examples:
    | section    |
    | include    |
    | workdir    |
    | configfile |
    | report     |

  Scenario: Injection in concatenated strings
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      input: "{foo}" + "{boo}" + "{doo}"
    """
    When I put the caret after foo
    Then I expect language injection on "{foo}{boo}{doo}"

  Scenario: Injection in concatenated strings with divided braces
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      input: "{foo" + "}"
    """
    When I put the caret after foo
    Then I expect language injection on "{foo}"

  Scenario Outline: No injection in some sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      <section>: "{foo}"
    """
    When I put the caret after foo
    Then I expect no language injection
    Examples:
    | section              |
    | shadow               |
    | wildcard_constraints |
    | wrapper              |
    | version              |
    | cache                |
    | threads              |
    | priority             |
    | singularity          |
    | container            |
    | containerized        |
    | notebook             |

  Scenario Outline: Inject in snakemake function calls
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      input: <function>("{foo}")
    """
    When I put the caret after foo
    Then I expect language injection on "{foo}"
    Examples:
    | function     |
    | ancient      |
    | directory    |
    | temp         |
    | pipe         |
    | temporary    |
    | protected    |
    | dynamic      |
    | touch        |
    | repeat       |
    | report       |
    | local        |
    | expand       |
    | shell        |
    | join         |
    | path.join    |
    | os.path.join |
    | multiext     |
