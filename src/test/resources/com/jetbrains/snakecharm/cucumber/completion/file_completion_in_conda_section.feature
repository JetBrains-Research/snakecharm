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
    | bob.yml |
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
    Given a file "Dir/boo.yaml" with text
    """
    """
    Given I open a file "foo.smk" with text
    """
    <section> NAME:
      conda: "D"
    """
    When I put the caret after D
    Then I invoke autocompletion popup and see a text:
    """
    <section> NAME:
      conda: "Dir/boo.yaml"
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