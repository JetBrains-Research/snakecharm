Feature: Complete file names in workflow sections

  Scenario Outline: Completion list in sections
    Given a snakemake project
    Given a file "a.<file_type>" with text
    """
    """
    Given I open a file "foo.smk" with text
    """
    <section>: "a"
    """
    When I put the caret after a
    And I invoke autocompletion popup
    Then completion list should contain:
    |a.<file_type>|
    Examples:
    | section    | file_type |
    | include    | smk       |
    | configfile | yaml      |
    | configfile | yml       |
    | report     | html      |

  Scenario Outline: Complete in sections
    Given a snakemake project
    Given a file "a.<file_type>" with text
    """
    """
    Given I open a file "foo.smk" with text
    """
    <section>: "a."
    """
    When I put the caret after a.
    Then I invoke autocompletion popup, select "a.<file_type>" lookup item and see a text:
    """
    <section>: "a.<file_type>"
    """
    Examples:
    | section    | file_type |
    | include    | smk       |
    | configfile | yaml      |
    | configfile | yml       |
    | report     | html      |

  Scenario: Completion list has only files with appropriate file types
    Given a snakemake project
    Given a file "a.smk" with text
    """
    """
    Given a file "a.yaml" with text
    """
    """
    Given a file "a.html" with text
    """
    """
    Given I open a file "foo.smk" with text
    """
    include: "a"
    """
    When I put the caret after a
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
    | a.yaml |
    | a.html |

  Scenario Outline: All appropriate files in project are collected
    Given a snakemake project
    Given a file "a.<file_type>" with text
    """
    """
    Given a file "A/ab.<file_type>" with text
    """
    """
    Given a file "A/B/C/ac.<file_type>" with text
    """
    """
    Given I open a file "foo.smk" with text
    """
    <section>: "a"
    """
    When I put the caret after a
    And I invoke autocompletion popup
    Then completion list should contain:
    |a.<file_type>|
    |ac.<file_type>|
    |ab.<file_type>|
    Examples:
    | section    | file_type |
    | include    | smk       |
    | configfile | yaml      |
    | configfile | yml       |
    | report     | html      |