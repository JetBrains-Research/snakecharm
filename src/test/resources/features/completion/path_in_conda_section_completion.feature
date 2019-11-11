Feature: Complete file name in conda section

  Scenario Outline: Completion list in conda section
    Given a snakemake project
    Given a file "boo.yaml" with text
    """
    """
    Given a file "bob.yml" with text
    """
    """
    Given I open a file "foo.smk" with text
    """
    <section> NAME:
      conda: "b"
    """
    When I put the caret after b
    And I invoke autocompletion popup
    Then completion list should contain:
      | boo.yaml |
      | bob.yml  |
    Examples:
      | section    |
      | rule       |
      | checkpoint |

  Scenario Outline: Completion list when there are no appropriate files
    Given a snakemake project
    Given a file "boo.smk" with text
    """
    """
    Given I open a file "foo.smk" with text
    """
    <section> NAME:
      conda: "b"
    """
    When I put the caret after b
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | boo.smk |
    Examples:
      | section    |
      | rule       |
      | checkpoint |

  Scenario Outline: Complete in conda section
    Given a snakemake project
    Given a file "boo.yaml" with text
    """
    """
    Given I open a file "foo.smk" with text
    """
    <section> NAME:
      conda: "b"
    """
    When I put the caret after b
    Then I invoke autocompletion popup and see a text:
    """
    <section> NAME:
      conda: "boo.yaml"
    """
    Examples:
      | section    |
      | rule       |
      | checkpoint |

  Scenario Outline: Completion for file in subdirectory
    Given a snakemake project
    Given a file "dir/boo.yaml" with text
    """
    """
    Given I open a file "foo.smk" with text
    """
    <section> NAME:
      conda: "di"
    """
    When I put the caret after di
    Then I invoke autocompletion popup and see a text:
    """
    <section> NAME:
      conda: "dir/boo.yaml"
    """
    Examples:
      | section    |
      | rule       |
      | checkpoint |

  Scenario Outline: Complete in conda section for different quotes
    Given a snakemake project
    Given a file "boo.yaml" with text
    """
    """
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      conda: <quote>b<quote>
    """
    When I put the caret after b
    Then I invoke autocompletion popup and see a text:
    """
    rule NAME:
      conda: <quote>boo.yaml<quote>
    """
    Examples:
      | quote |
      | "     |
      | """   |
      | '     |

  Scenario Outline: Complete in conda section for fstrings in different quotes
    Given a snakemake project
    Given a file "boo.yaml" with text
    """
    """
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      conda: f<quote>b<quote>
    """
    When I put the caret after b
    Then I invoke autocompletion popup and see a text:
    """
    rule NAME:
      conda: f<quote>boo.yaml<quote>
    """
    Examples:
      | quote |
      | "     |
      | """   |
      | '     |

  Scenario Outline: Completion if rule file in subdirectory
    Given a snakemake project
    Given a file "prevent_single_entry_completion.yaml" with text
    """
    """
    Given a file "<yaml_path>" with text
    """
    """
    Given I open a file "rules/foo.smk" with text
    """
    <section> NAME:
      conda: ""
    """
    When I put the caret after conda: "
    Then I invoke autocompletion popup
    Then completion list should contain:
      | <relative_path> |
    Examples:
      | section    | yaml_path      | relative_path    |
      | rule       | boo.yaml       | ../boo.yaml      |
      | rule       | envs/boo.yaml  | ../envs/boo.yaml |
      | rule       | rules/boo.yaml | boo.yaml         |
      | checkpoint | envs/boo.yaml  | ../envs/boo.yaml |