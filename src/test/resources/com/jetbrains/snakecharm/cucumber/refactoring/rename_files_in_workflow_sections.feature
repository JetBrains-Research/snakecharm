Feature: Rename files in workflow sections

  Scenario Outline: Rename in sections
    Given a snakemake project
    Given a file "boo.<file_type>" with text
    """
    TEXT
    """
    Given I open a file "foo.smk" with text
    """
    <section>: "boo.<file_type>"
    """
    When I put the caret after boo
    When I invoke rename with name "doo.<file_type>"
    Then the file "foo.smk" should have text
    """
    <section>: "doo.<file_type>"
    """
    And reference should resolve to "TEXT" in "doo.<file_type>"
    Examples:
      | section    | file_type |
      | include    | smk       |
      | configfile | yaml      |
      | configfile | yml       |
      | report     | html      |

  Scenario Outline: Rename in sections for relative paths
    Given a snakemake project
    Given a file "Dir/boo.<file_type>" with text
    """
    TEXT
    """
    Given I open a file "foo.smk" with text
    """
    <section>: "Dir/boo.<file_type>"
    """
    When I put the caret after boo
    When I invoke rename with name "foo.<file_type>"
    Then the file "foo.smk" should have text
    """
    <section>: "Dir/foo.<file_type>"
    """
    And reference should resolve to "TEXT" in "foo.<file_type>"
    Examples:
      | section    | file_type |
      | include    | smk       |
      | configfile | yaml      |
      | configfile | yml       |
      | report     | html      |

  Scenario: Rename for subdirectories in 'workdir' section
    Given a snakemake project
    Given a directory "Dir1/Dir2"
    Given I open a file "foo.smk" with text
    """
    workdir: "Dir1/Dir2"
    """
    When I put the caret after Dir2
    When I invoke rename with name "Folder"
    Then the file "foo.smk" should have text
    """
    workdir: "Dir1/Folder"
    """
    And reference should resolve to "Folder" directory

  Scenario: Rename for directories in 'workdir' section
    Given a snakemake project
    Given a directory "Dir"
    Given I open a file "foo.smk" with text
    """
    workdir: "Dir"
    """
    When I put the caret after Dir
    When I invoke rename with name "Folder"
    Then the file "foo.smk" should have text
    """
    workdir: "Folder"
    """
    And reference should resolve to "Folder" directory