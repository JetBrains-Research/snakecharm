Feature: Tests on snakemake string language injection

  #TODO: If lambda args need to be changed, please refactor all test here & move cases into mock Snakemake API YAML file

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

   Scenario Outline: Allow injection in lambdas in expand call
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule:
      output: "foo"
      log: lambda wd: <expand_alias>("{output}.log", output="out.txt")
    """
    When I put the caret after {output
     Then I expect language injection on "{output}.log"
     Examples:
       | expand_alias |
       | expand       |
       | collect      |

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

  Scenario Outline: No injection in some sections in latest lang level
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
      | template_engine      |
      | containerized        |
      | notebook             |
      | handover             |
      | default_target       |
      | retries              |

  Scenario Outline: Inject in snakemake function calls in custom snakemake version
    Given a <smk_library> project
    And I set snakemake language version to "<smk_lang_level>"
    Given I open a file "foo.smk" with text
    """
    <import_statement>
    rule NAME:
      input: <function>("{foo}")
    """
    When I put the caret after foo
    Then I expect language injection on "{foo}"
    Examples:
      | function | import_statement | smk_library      | smk_lang_level |
      | dynamic  |                  | snakemake:7.32.4 | 7.35.0         |

  Scenario Outline: Inject in snakemake sections in custom snakemake version
    Given a <smk_library> project
    And I set snakemake language version to "<smk_lang_level>"
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      <section>: "{foo}"
    """
    When I put the caret after foo
    Then I expect language injection on "{foo}"
    Examples:
      | section   | smk_library | smk_lang_level |
      | container | snakemake   | 9.5.0          |

  Scenario Outline: Inject in snakemake function calls
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <import_statement>
    rule NAME:
      input: <function>("{foo}")
    """
    When I put the caret after foo
    Then I expect language injection on "{foo}"
    Examples:
      | function     | import_statement          |
      | ancient      |                           |
      | directory    |                           |
      | temp         |                           |
      | pipe         |                           |
      | temporary    |                           |
      | protected    |                           |
      | touch        |                           |
      | repeat       |                           |
      | report       |                           |
      | local        |                           |
      | expand       |                           |
      | collect      |                           |
      | shell        |                           |
      | join         | from os.path import  join |
      | path.join    | from os import  path      |
      | os.path.join | import os                 |
      | multiext     |                           |
      | exists       |                           |
      | evaluate     |                           |
      # Unresolved
      | dynamic      |                           |
      | join         |                           |
      | path.join    |                           |
      | os.path.join |                           |

  Scenario Outline: Inject in snakemake function calls configured by API
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "2.0.0"

        override:
        - name: "my_functions.fooboodoo_allowed"
          type: "function"
          placeholders_injection_allowed: False

        - name: "my_functions.fooboodoo_not"
          type: "function"
          placeholders_injection_allowed: True

      - version: "1.0.0"

        introduced:
        - name: "input"
          type: "rule-like"
          is_accessible_as_placeholder: True

        - name: "output"
          type: "rule-like"
          is_accessible_as_placeholder: True

        - name: "my_functions.fooboodoo_allowed"
          type: "function"
          placeholders_injection_allowed: True

        - name: "my_functions.fooboodoo_not"
          type: "function"
          placeholders_injection_allowed: False

        - name: "my_functions.fooboodoo_default"
          type: "function"
    """
    And I set snakemake language version to "<smk_version>"
    Given a file "my_functions.py" with text
    """
    def <function1>(p):
      pass
    def <function2>(p):
      pass
    """
    Given I open a file "foo.smk" with text
    """
    <import_statement>

    rule NAME:
      input: <function1>("{faa}")
      output: <function2>("{baa}")
    """
    When I put the caret after faa
    Then I expect language injection on "{faa}"
    When I put the caret after baa
    Then I expect no language injection
    Examples:
      | smk_version | function1         | function2         | import_statement           |
      | 1.0.0       | fooboodoo_allowed | fooboodoo_not     |                            |
      | 1.0.0       | fooboodoo_allowed | fooboodoo_not     | from my_functions import * |
      | 1.0.0       | fooboodoo_allowed | fooboodoo_default |                            |
      | 2.0.0       | fooboodoo_not     | fooboodoo_allowed |                            |
      | 2.0.0       | fooboodoo_not     | fooboodoo_default |                            |
