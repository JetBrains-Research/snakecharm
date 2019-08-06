Feature: Tests on snakemake string language injection

  Scenario Outline: Injection for different quotes
    Given a snakemake project with language injection
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
    Given a snakemake project with language injection
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

  Scenario: No injection in fstrings
    Given a snakemake project with language injection
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      shell: f"{input}"
    """
    When I put the caret after input
    Then I expect no language injection

  Scenario: Injection in split string literal
    Given a snakemake project with language injection
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      shell: "somecommand" "{wildcards.group}"  f'{10}' "{output}"
    """
    When I put the caret after somecommand
    Then I expect language injection on "somecommand"
    When I put the caret after wildcards
    Then I expect language injection on "{wildcards.group}"
    When I put the caret after 10
    Then I expect no language injection
    When I put the caret after output
    Then I expect language injection on "{output}"

  Scenario: No injection for ordinary string literals
    Given a snakemake project with language injection
    Given I open a file "foo.smk" with text
    """
    str = "NAME"
    """
    When I put the caret after NAME
    Then I expect no language injection

  Scenario: Injection in concatenated string literal
    Given a snakemake project with language injection
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      shell: "{input}" + "{output}"
    """
    When I put the caret after input
    Then I expect language injection on "{input}"
    When I put the caret after output
    Then I expect language injection on "{output}"

  Scenario: No injection in function calls
    Given a snakemake project with language injection
    Given I open a file "foo.smk" with text
    """
    def f(s)
      return s

    rule NAME:
      input: expand("{foo}")
      output: f("{boo}")
    """
    When I put the caret after foo
    Then I expect no language injection
    When I put the caret after boo
    Then I expect no language injection

  Scenario: No injection in empty strings
    Given a snakemake project with language injection
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      input: ""#here
    """
    When I put the caret at "#here
    Then I expect no language injection

  Scenario Outline: No injection in top-level workflow sections
    Given a snakemake project with language injection
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