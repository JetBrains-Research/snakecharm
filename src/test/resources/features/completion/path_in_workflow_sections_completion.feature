Feature: Complete file names in workflow sections

  Scenario Outline: Completion list in sections
    Given a snakemake project
    Given a file "boo.<file_type>" with text
    """
    """
    Given a file "bob.<file_type>" with text
    """
    """
    Given I open a file "foo.smk" with text
    """
    <section>: "b "
    """
    When I put the caret after b
    And I invoke autocompletion popup
    Then completion list should contain:
      | boo.<file_type> |
      | bob.<file_type> |
    Examples:
      | section    | file_type |
      | include    | smk       |
      | configfile | yaml      |
      | configfile | yml       |
      | report     | html      |

  Scenario: Completion list for directories in 'workdir' section
    Given a snakemake project
    Given a directory "Dir1"
    Given a directory "Dir2"
    Given I open a file "foo.smk" with text
    """
    workdir: "D "
    """
    When I put the caret after D
    And I invoke autocompletion popup
    Then completion list should contain:
      | Dir1 |
      | Dir2 |

  Scenario Outline: Complete in sections
    Given a snakemake project
    Given a file "boo.<file_type>" with text
    """
    """
    Given I open a file "foo.smk" with text
    """
    <section>: "b"
    """
    When I put the caret after b
    Then I invoke autocompletion popup and see a text:
    """
    <section>: "boo.<file_type>"
    """
    Examples:
      | section    | file_type |
      | include    | smk       |
      | configfile | yaml      |
      | configfile | yml       |
      | report     | html      |

  Scenario: Complete in 'workdir' section
    Given a snakemake project
    Given a directory "Dir"
    Given I open a file "foo.smk" with text
    """
    workdir: "D"
    """
    When I put the caret after D
    Then I invoke autocompletion popup and see a text:
    """
    workdir: "Dir"
    """

  Scenario Outline: Completion list when there are no appropriate files
    Given a snakemake project
    Given a file "doo.<file_type>" with text
    """
    """
    Given I open a file "foo.smk" with text
    """
    <section>: "b "
    """
    When I put the caret after b
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      |doo.<file_type>|
    Examples:
      | section    | file_type |
      | include    | smk       |
      | configfile | yaml      |
      | configfile | yml       |
      | report     | html      |

  Scenario: Completion list in 'workdir' section when there are no appropriate directories
    Given a snakemake project
    Given a directory "Folder"
    Given I open a file "foo.smk" with text
    """
    workdir: "X "
    """
    When I put the caret after X
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | Folder |

  Scenario Outline: Completion list has only files with appropriate file types
    Given a snakemake project
    Given a file "boo.smk" with text
    """
    """
    Given a file "boo.yaml" with text
    """
    """
    Given a file "boo.html" with text
    """
    """
    Given I open a file "foo.smk" with text
    """
    <section>: "boo "
    """
    When I put the caret after boo
    And I invoke autocompletion popup
    Then completion list should only contain:
      | boo.<file_type> |
    Examples:
      | section    | file_type |
      | include    | smk       |
      | configfile | yaml      |
      | report     | html      |

  Scenario Outline: Completion list if it isn't configfile doesn't contain files from top-level directories
    Given a snakemake project
    Given a file "boo.<file_type>" with text
    """
    """
    Given I open a file "Dir/foo.smk" with text
    """
    <section>: "boo "
    """
    When I put the caret after boo
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | boo.<file_type> |
    Examples:
      | section | file_type |
      | include | smk       |
      | report  | html      |

  Scenario: Completion list in 'workdir' section doesn't contain top-level directories
    Given a snakemake project
    Given I open a file "Dir1/Dir2/foo.smk" with text
    """
    workdir: "D "
    """
    When I put the caret after D
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | Dir1 |

  Scenario Outline: Completion for file in subdirectory
    Given a snakemake project
    Given a file "Dir/boo.<file_type>" with text
    """
    """
    Given I open a file "foo.smk" with text
    """
    <section>: "D"
    """
    When I put the caret after D
    Then I invoke autocompletion popup and see a text:
    """
    <section>: "Dir/boo.<file_type>"
    """
    Examples:
      | section    | file_type |
      | include    | smk       |
      | configfile | yaml      |
      | configfile | yml       |
      | report     | html      |

  Scenario: Completion in 'workdir' section for subdirectory
    Given a snakemake project
    Given a directory "Dir1/Dir2"
    Given I open a file "foo.smk" with text
    """
    workdir: "D"
    """
    When I put the caret after D
    Then I invoke autocompletion popup, select "Dir1/Dir2" lookup item and see a text:
    """
    workdir: "Dir1/Dir2"
    """

  Scenario Outline: Completion list for strings in different quotes
    Given a snakemake project
    Given a file "boo.smk" with text
    """
    """
    Given I open a file "foo.smk" with text
    """
    include: <quote>boo <quote>
    """
    When I put the caret after boo
    And I invoke autocompletion popup
    Then completion list should contain:
      | boo.smk |
    Examples:
      | quote |
      | "     |
      | '     |
      | """   |

  Scenario Outline: Completion list for fstrings in different quotes
    Given a snakemake project
    Given a file "boo.smk" with text
    """
    """
    Given I open a file "foo.smk" with text
    """
    include: f<quote>boo <quote>
    """
    When I put the caret after boo
    And I invoke autocompletion popup
    Then completion list should contain:
      | boo.smk |
    Examples:
      | quote |
      | "     |
      | '     |
      | """   |

  Scenario Outline: Completion in empty fstring
    Given a snakemake project
    Given a file "boo.<file_type>" with text
    """
    """
    Given I open a file "foo.smk" with text
    """
    <section>: f""
    """
    When I put the caret after f"
    Then I invoke autocompletion popup and see a text:
    """
    <section>: f"boo.<file_type>"
    """
    Examples:
      | section    | file_type |
      | include    | smk       |
      | configfile | yaml      |
      | report     | html      |

  Scenario Outline: Completion list to a configfile in different subdirectories
    Given a snakemake project
    Given a file "Dir1/boo.<file_type>" with text
    """
    """
    Given I open a file "Dir2/foo.smk" with text
    """
    <section>: "D"
    """
    When I put the caret after D
    Then I invoke autocompletion popup and see a text:
    """
    <section>: "Dir1/boo.<file_type>"
    """
    Examples:
      | section    | file_type |
      | configfile | yaml      |
      | configfile | yml       |

  Scenario Outline: Completion list in configfile doesn't contain path from current folder
    Given a snakemake project
    Given a file "Dir1/boo.<file_type>" with text
    """
    """
    Given I open a file "Dir2/foo.smk" with text
    """
    configfile: ".."
    """
    When I put the caret after ..
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | ../Dir1/boo.<file_type> |
    Examples:
      | file_type |
      | yaml      |
      | yml       |