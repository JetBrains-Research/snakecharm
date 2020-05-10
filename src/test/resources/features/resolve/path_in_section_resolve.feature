Feature: Resolve file in conda/notebook sections

  Scenario Outline: Resolve to a file in the same directory
    Given a snakemake project
    Given a file "roo.<ext>" with text
    """
    TEXT
    """
    Given I open a file "foo.smk" with text
    """
    <rule_like>:
      <section>: "roo.<ext>"
    """
    When I put the caret at roo
    Then reference should resolve to "TEXT" in "roo.<ext>"
    Examples:
      | rule_like  | section  | ext      |
      | rule       | conda    | yaml     |
      | checkpoint | conda    | yaml     |
      | rule       | conda    | yml      |
      | rule       | notebook | py.ipynb |
      | rule       | notebook | r.ipynb  |
      | checkpoint | notebook | py.ipynb |

  Scenario Outline: Reference doesn't resolve to inappropriate file
    Given a snakemake project
    Given a file "roo.smk" with text
    """
    """
    Given I open a file "foo.smk" with text
    """
    <rule_like>:
      <section>: "roo.<ext>"
    """
    When I put the caret at roo
    Then reference should not resolve
    Examples:
      | rule_like  | section  | ext      |
      | rule       | conda    | yaml     |
      | checkpoint | conda    | yaml     |
      | rule       | notebook | py.ipynb |
      | rule       | notebook | r.ipynb  |
      | checkpoint | notebook | py.ipynb |

  Scenario Outline: Resolve to a file in a sub directory
    Given a snakemake project
    Given a file "dir/roo.<ext>" with text
    """
    TEXT
    """
    Given I open a file "foo.smk" with text
    """
    <rule_like>:
      <section>: "dir/roo.<ext>"
    """
    When I put the caret at roo
    Then reference should resolve to "TEXT" in "roo.<ext>"
    Examples:
      | rule_like  | section  | ext      |
      | rule       | conda    | yaml     |
      | checkpoint | conda    | yaml     |
      | rule       | notebook | py.ipynb |
      | rule       | notebook | r.ipynb  |
      | checkpoint | notebook | py.ipynb |

  Scenario Outline: Resolve for strings in different quotes
    Given a snakemake project
    Given a file "roo.<ext>" with text
    """
    TEXT
    """
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      <section>: <quote>roo.<ext><quote>
    """
    When I put the caret after roo
    Then reference should resolve to "TEXT" in "roo.<ext>"
    Examples:
      | quote | section  | ext      |
      | "     | conda    | yaml     |
      | '     | conda    | yaml     |
      | """   | conda    | yaml     |
      | '     | notebook | py.ipynb |
      | "     | notebook | py.ipynb |
      | """   | notebook | py.ipynb |

  Scenario Outline: Resolve is off for fstrings
    Given a snakemake project
    Given a file "roo.<ext>" with text
    """
    TEXT
    """
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      <section>: f<quote>roo.<ext><quote>
    """
    When I put the caret after roo
    Then reference should not resolve
    Examples:
      | quote | section  | ext      |
      | "     | conda    | yaml     |
      | '     | conda    | yaml     |
      | """   | conda    | yaml     |
      | '     | notebook | py.ipynb |
      | "     | notebook | py.ipynb |
      | """   | notebook | py.ipynb |

  Scenario Outline: Resolve when string literal is divided
    Given a snakemake project
    Given a file "roo.<ext>" with text
    """
    TEXT
    """
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      <section>: "roo"".<ext>"
    """
    When I put the caret at roo
    Then reference should resolve to "TEXT" in "roo.<ext>"
    Examples:
      | rule_like  | section  | ext      |
      | rule       | conda    | yaml     |
      | checkpoint | conda    | yaml     |
      | rule       | notebook | py.ipynb |

  Scenario Outline: Resolve when string literal is crazily divided
    Given a snakemake project
    Given a file "boo.yaml" with text
    """
    TEXT
    """
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      conda: "" 'b' "o" "o" '.' "yaml" ''
      #co#nda: "" 'b' "o" "o" f'.' f"yaml" ''
    """
    When I put the caret after  conda: "" 'b
    Then reference should resolve to "TEXT" in "boo.yaml"
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Resolve when language is injected
    Given a snakemake project
    Given a file "roo.<ext>" with text
    """
    """
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      <section>: "{roo}.<ext>"
    """
    When I put the caret at ro
    Then there should be no reference
    Examples:
      | rule_like  | section | ext  |
      | rule       | conda   | yaml |
      | checkpoint | conda   | yaml |

  Scenario Outline: Unresolved if complicated
    Given a snakemake project
    Given a file "roo.<ext>" with text
      """
      TEXT
      """
    Given I open a file "foo.smk" with text
      """
      <rule_like>:
        <section>: f"{wd}/roo.<ext>"
      """
    When I put the caret at roo
    Then reference should not resolve
    Examples:
      | rule_like  | section  | ext      |
      | rule       | conda    | yaml     |
      | checkpoint | conda    | yaml     |
      | rule       | notebook | py.ipynb |

  Scenario Outline: Resolve if rule file in subdirectory
    Given a snakemake project
    Given a file "<full_path>" with text
     """
     TEXT
     """
    Given I open a file "rules/foo.smk" with text
     """
     <rule_like> NAME:
       <section>: "<relative_path>"
     """
    When I put the caret after <section>: "
    Then reference should resolve to "TEXT" in "<full_path>"
    Examples:
      | rule_like  | section  | full_path              | relative_path             |
      | rule       | conda    | roo.yaml               | ../roo.yaml               |
      | rule       | conda    | envs/roo.yaml          | ../envs/roo.yaml          |
      | rule       | conda    | rules/roo.yaml         | roo.yaml                  |
      | checkpoint | conda    | envs/roo.yaml          | ../envs/roo.yaml          |
      | rule       | notebook | roo.py.ipynb           | ../roo.py.ipynb           |
      | rule       | notebook | notebooks/roo.py.ipynb | ../notebooks/roo.py.ipynb |
      | rule       | notebook | rules/roo.py.ipynb     | roo.py.ipynb              |