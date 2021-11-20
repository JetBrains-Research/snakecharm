Feature: Complete file name in conda section

  Scenario Outline: Completion list in path dependent section
    Given a snakemake project
    Given a file "<file>" with text
    """
    """
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      <section>: ""
    """
    When I put the caret after : "
    And I invoke autocompletion popup and see a text:
    """
    <rule_like> NAME:
      <section>: "<file>"
    """
    Examples:
      | rule_like  | section   | file                  |
      | rule       | conda     | xoo.yaml              |
      | checkpoint | conda     | xoo.yaml              |
      | rule       | conda     | xoo.yml               |
      | rule       | notebook  | xoo.py.ipynb          |
      | rule       | notebook  | xoo.r.ipynb           |
      | module     | snakefile | xoo.smk               |
      | module     | snakefile | other/files/foo.smk   |
      | module     | snakefile | other/files/Snakefile |

  Scenario Outline: Completion list when there are no appropriate files
    Given a snakemake project
    Given a file "roo.smk" with text
    """
    """
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      <section>: "ro"
    """
    When I put the caret after ro
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | roo.smk |
    Examples:
      | rule_like  | section  |
      | rule       | conda    |
      | checkpoint | conda    |
      | rule       | notebook |

  Scenario Outline: Completion for file in subdirectory
    Given a snakemake project
    Given a file "dir/roo.<ext>" with text
    """
    """
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      <section>: "di"
    """
    When I put the caret after di
    Then I invoke autocompletion popup and see a text:
    """
    <rule_like> NAME:
      <section>: "dir/roo.<ext>"
    """
    Examples:
      | rule_like  | section  | ext      |
      | rule       | conda    | yaml     |
      | checkpoint | conda    | yaml     |
      | rule       | notebook | py.ipynb |

  Scenario Outline: Complete in conda section for different quotes
    Given a snakemake project
    Given a file "roo.<ext>" with text
    """
    """
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      <section>: <quote>ro<quote>
    """
    When I put the caret after ro
    Then I invoke autocompletion popup and see a text:
    """
    rule NAME:
      <section>: <quote>roo.<ext><quote>
    """
    Examples:
      | quote | section  | ext      |
      | "     | conda    | yaml     |
      | '     | conda    | yaml     |
      | """   | conda    | yaml     |
      | '     | notebook | py.ipynb |
      | "     | notebook | py.ipynb |
      | """   | notebook | py.ipynb |

  Scenario Outline: Complete in conda section for fstrings in different quotes
    Given a snakemake project
    Given a file "roo.<ext>" with text
    """
    """
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      <section>: f<quote>ro<quote>
    """
    When I put the caret after ro
    Then I invoke autocompletion popup and see a text:
    """
    rule NAME:
      <section>: f<quote>roo.<ext><quote>
    """
    Examples:
      | quote | section  | ext      |
      | "     | conda    | yaml     |
      | '     | conda    | yaml     |
      | """   | conda    | yaml     |
      | '     | notebook | py.ipynb |
      | "     | notebook | py.ipynb |
      | """   | notebook | py.ipynb |

  Scenario Outline: Completion if rule file in subdirectory
    Given a snakemake project
    Given a file "prevent_single_entry_completion.<ext>" with text
    """
    """
    Given a file "<full_path>" with text
    """
    """
    Given I open a file "rules/foo.smk" with text
    """
    <rule_like> NAME:
      <section>: ""
    """
    When I put the caret after <section>: "
    Then I invoke autocompletion popup
    Then completion list should contain:
      | <relative_path> |
    Examples:
      | rule_like  | section  | full_path              | relative_path             | ext      |
      | rule       | conda    | roo.yaml               | ../roo.yaml               | yaml     |
      | rule       | conda    | envs/roo.yaml          | ../envs/roo.yaml          | yaml     |
      | rule       | conda    | rules/roo.yaml         | roo.yaml                  | yaml     |
      | checkpoint | conda    | envs/roo.yaml          | ../envs/roo.yaml          | yaml     |
      | rule       | notebook | roo.py.ipynb           | ../roo.py.ipynb           | py.ipynb |
      | rule       | notebook | notebooks/roo.py.ipynb | ../notebooks/roo.py.ipynb | py.ipynb |
      | rule       | notebook | rules/roo.py.ipynb     | roo.py.ipynb              | py.ipynb |