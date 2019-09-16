Feature: Annotate wildcards

  Scenario Outline: Simple wildcard name in injection
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      <section>: "{sample}"
    """
    Then I expect inspection info on <sample> with message
    """
    PY.NUMBER
    """
    When I check highlighting infos ignoring extra highlighting
    Examples:
      | rule_like  | section |
      | rule       | output  |
      | rule       | input   |
      | rule       | params  |
      | checkpoint | output  |

  Scenario Outline: Wildcards usage via wildcards. prefix
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      <text>
    """
    Then I expect inspection info on <sample> with message
    """
    PY.NUMBER
    """
    When I check highlighting infos ignoring extra highlighting
    Examples:
      | rule_like  | text                             |
      | rule       | message: "{wildcards.sample}"    |
      | rule       | shell: "{wildcards.sample}"      |
      | rule       | run: shell("{wildcards.sample}") |
      | checkpoint | shell: "{wildcards.sample}"      |

  Scenario Outline: Wildcards in python code
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      run:
        print(wildcards.sample)
    """
    Then I expect inspection info on <sample> with message
    """
    PY.NUMBER
    """
    When I check highlighting infos ignoring extra highlighting
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Wildcards in python code (via type inference)
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      run:
        boo = wildcards
        print(boo.sample)
    """
    Then I expect inspection info on <sample> with message
    """
    PY.NUMBER
    """
    When I check highlighting infos ignoring extra highlighting
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Wildcards in python code lambda args
    Given a snakemake project
    Given I open a file "foo.smk" with text
     """
     <rule_like> foo:
       input: lambda wd: print(wd.sample)
     """
    Then I expect inspection info on <sample> with message
     """
     PY.NUMBER
     """
    When I check highlighting infos ignoring extra highlighting
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Simple wildcard name in allows calls
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      input: <call>("{sample}")
    """
    Then I expect inspection info on <sample> with message
    """
    PY.NUMBER
    """
    When I check highlighting infos ignoring extra highlighting
    Examples:
      | rule_like  | call    |
      | rule       | ancient |
      | rule       | temp    |
      | checkpoint | temp    |

  # TODO: Scenario Outline: Simple wildcard name in banned calls
  # (e.g. expand), at the moment not clear how to write test for such highlighting