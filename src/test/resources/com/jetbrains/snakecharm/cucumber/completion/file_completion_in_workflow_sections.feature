Feature: Complete file names in workflow sections

  Scenario Outline: Completion list in sections
    Given a snakemake project
    Given a file "boo.<file_type>" with text
    """
    """
    Given I open a file "foo.smk" with text
    """
    <section>: "b "
    """
    When I put the caret after b
    And I invoke autocompletion popup
    Then completion list should contain:
      |boo.<file_type>|
    Examples:
      | section    | file_type |
      | include    | smk       |
      | configfile | yaml      |
      | configfile | yml       |
      | report     | html      |

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
      |boo.smk|
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
      |boo.smk|
    Examples:
      | quote |
      | "     |
      | '     |
      | """   |

  Scenario Outline: Complete in sections
    Given a snakemake project
    Given a file "boo.<file_type>" with text
    """
    """
    Given I open a file "foo.smk" with text
    """
    <section>: "boo."
    """
    When I put the caret after boo.
    Then I invoke autocompletion popup, select "a.<file_type>" lookup item and see a text:
    """
    <section>: "boo.<file_type>"
    """
    Examples:
      | section    | file_type |
      | include    | smk       |
      | configfile | yaml      |
      | configfile | yml       |
      | report     | html      |

  Scenario: Completion list has only files with appropriate file types
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
    include: "boo "
    """
    When I put the caret after boo
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | a.yaml |
      | a.html |

  Scenario: Completion list for workdir section
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