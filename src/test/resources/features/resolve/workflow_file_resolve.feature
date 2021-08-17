Feature: Resolve workflow file names to their corresponding files

  Scenario Outline: Resolve to a file in the same directory
    Given a snakemake project
    Given a file "boo.<file_type>" with text
    """
    TEXT
    """
    Given I open a file "foo.smk" with text
    """
    <workflow>: "boo.<file_type>"
    """
    When I put the caret at boo
    Then reference should resolve to "TEXT" in "boo.<file_type>"
    Examples:
      | workflow   | file_type |
      | include    | smk       |
      | configfile | yaml      |
      | report     | html      |
      | pepfile    | yaml      |
      | pepschema  | yml       |

  Scenario Outline: Reference doesn't resolve to inappropriate file
    Given a snakemake project
    Given a file "boo.<file_type>" with text
    """
    """
    Given I open a file "foo.smk" with text
    """
    <workflow>: "doo.<file_type>"
    """
    When I put the caret at doo
    Then reference should not resolve
    Examples:
      | workflow   | file_type |
      | include    | smk       |
      | configfile | yaml      |
      | report     | html      |
      | pepfile    | yaml      |
      | pepschema   | yml       |

  Scenario: Reference doesn't resolve to inappropriate Directory
    Given a snakemake project
    Given a directory "Dir"
    Given I open a file "foo.smk" with text
    """
    workdir: "Folder"
    """
    When I put the caret at Folder
    Then reference should not resolve

  Scenario Outline: Resolve to a file in a sub directory
    Given a snakemake project
    Given a file "A/boo.<file_type>" with text
    """
    TEXT
    """
    Given I open a file "foo.smk" with text
    """
    <workflow>: "A/boo.<file_type>"
    """
    When I put the caret at boo
    Then reference should resolve to "TEXT" in "boo.<file_type>"
    Examples:
      | workflow   | file_type |
      | include    | smk       |
      | configfile | yaml      |
      | report     | html      |
      | pepfile    | yaml      |
      | pepschema   | yml       |

  Scenario Outline: Resolve for multiple files
    Given a snakemake project
    Given a file "file1.<file_type>" with text
    """
    TEXT
    """
    Given a file "file2.<file_type>" with text
    """
    TEXT
    """
    Given I open a file "foo.smk" with text
    """
    <workflow>: "file1.<file_type>", "file2.<file_type>"
    """
    When I put the caret at file1
    Then reference should resolve to "TEXT" in "file1.<file_type>"
    When I put the caret at file2
    Then reference should resolve to "TEXT" in "file2.<file_type>"
    Examples:
      | workflow   | file_type |
      | include    | smk       |
      | configfile | yaml      |
      | report     | html      |
      | pepfile    | yaml      |
      | pepschema   | yml       |

  Scenario Outline: Resolve for strings in different quotes
    Given a snakemake project
    Given a file "boo.smk" with text
    """
    rule NAME:
    """
    Given I open a file "foo.smk" with text
    """
    include: <quote>boo.smk<quote>
    """
    When I put the caret after boo
    Then reference should resolve to "rule" in "boo.smk"
    Examples:
      | quote |
      | "     |
      | '     |
      | """   |

  Scenario Outline: Do not resolve fstrings
    Given a snakemake project
    Given a file "boo.smk" with text
    """
    rule NAME:
    """
    Given I open a file "foo.smk" with text
    """
    include: f<quote>boo.smk<quote>
    """
    When I put the caret after boo
    Then reference should not resolve
    Examples:
      | quote |
      | "     |
      | '     |
      | """   |

  Scenario: Resolve in 'workdir' section for directory
    Given a snakemake project
    Given a directory "Dir"
    Given I open a file "foo.smk" with text
    """
    workdir: "Dir"
    """
    When I put the caret after Dir
    Then reference should resolve to "Dir" directory

  Scenario: Reference doesn't resolve to inappropriate directory
    Given a snakemake project
    Given a directory "Folder"
    Given I open a file "foo.smk" with text
    """
    workdir: "Dir"
    """
    When I put the caret after Dir
    Then reference should not resolve

  Scenario: Resolve in 'workdir' section for subdirectory
    Given a snakemake project
    Given a directory "Dir1/Dir2"
    Given I open a file "foo.smk" with text
    """
    workdir: "Dir1/Dir2"
    """
    When I put the caret after Dir1/Dir2
    Then reference should resolve to "Dir2" directory

  Scenario: Resolve in 'workdir' section when string literal is divided
    Given a snakemake project
    Given a directory "Dir"
    Given I open a file "foo.smk" with text
    """
    workdir: "D""ir"
    """
    When I put the caret after D
    Then reference should resolve to "Dir" directory

  Scenario Outline: Resolve when string literal is divided
    Given a snakemake project
    Given a file "boo.<file_type>" with text
    """
    TEXT
    """
    Given I open a file "foo.smk" with text
    """
    <workflow>: "boo"".<file_type>"
    """
    When I put the caret at boo
    Then reference should resolve to "TEXT" in "boo.<file_type>"
    Examples:
      | workflow   | file_type |
      | include    | smk       |
      | configfile | yaml      |
      | report     | html      |
      | pepfile    | yaml      |
      | pepschema   | yml       |

  Scenario Outline: Resolve when string literal is crazily divided
    Given a snakemake project
    Given a file "boo.<file_type>" with text
    """
    TEXT
    """
    Given I open a file "foo.smk" with text
    """
    <workflow>: "" "b" 'oo' "." '<file_type>' ''
    """
    When I put the caret at b
    Then reference should resolve to "TEXT" in "boo.<file_type>"
    Examples:
      | workflow   | file_type |
      | include    | smk       |
      | configfile | yaml      |
      | report     | html      |
      | pepfile    | yaml      |
      | pepschema   | yml       |

  Scenario Outline: Resolve to a section relative content root in different subdirectories
    Given a snakemake project
    Given a file "Dir1/boo.<file_type>" with text
    """
    TEXT
    """
    Given I open a file "Dir2/foo.smk" with text
    """
    <workflow>: "Dir1/boo.<file_type>"
    """
    When I put the caret at boo
    Then reference should resolve to "TEXT" in "Dir1/boo.<file_type>"
    Examples:
      | workflow   | file_type |
      | configfile | yaml      |
      | pepfile    | yml       |

  Scenario Outline: Reference doesn't resolve to section relative content root with wrong path
    Given a snakemake project
    Given a file "Dir1/boo.<file_type>" with text
    """
    TEXT
    """
    Given I open a file "Dir2/foo.smk" with text
    """
    <workflow>: "../Dir1/boo.<file_type>"
    """
    When I put the caret at boo
    Then reference should not resolve
    Examples:
      | workflow   | file_type |
      | configfile | yaml      |
      | pepfile    | yml       |

  Scenario Outline: Resolve to a section relative current file in different subdirectories
    Given a snakemake project
    Given a file "Dir1/boo.<file_type>" with text
    """
    TEXT
    """
    Given I open a file "Dir2/foo.smk" with text
    """
    <workflow>: "../Dir1/boo.<file_type>"
    """
    When I put the caret at boo
    Then reference should resolve to "TEXT" in "Dir1/boo.<file_type>"
    Examples:
      | workflow  | file_type |
      | include   | smk       |
      | report    | html      |
      | pepschema | yaml      |

  Scenario Outline: Reference doesn't resolve to section relative content root with wrong path
    Given a snakemake project
    Given a file "Dir1/boo.<file_type>" with text
    """
    TEXT
    """
    Given I open a file "Dir2/foo.smk" with text
    """
    <workflow>: "Dir1/boo.<file_type>"
    """
    When I put the caret at boo
    Then reference should not resolve
    Examples:
      | workflow  | file_type |
      | include   | smk       |
      | report    | html      |
      | pepschema | yaml      |