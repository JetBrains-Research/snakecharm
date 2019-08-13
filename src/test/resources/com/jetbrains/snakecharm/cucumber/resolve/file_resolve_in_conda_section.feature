Feature: Resolve file in conda section
  Scenario Outline: Resolve to a file in the same directory
    Given a snakemake project
    Given a file "boo.yaml" with text
    """
    TEXT
    """
    Given I open a file "foo.smk" with text
    """
    <section>:
      conda: "boo.yaml"
    """
    When I put the caret at boo
    Then reference should resolve to "TEXT" in "boo.yaml"
    Examples:
      | section    |
      | rule       |
      | checkpoint |

  Scenario Outline: Reference doesn't resolve to inappropriate file
    Given a snakemake project
    Given a file "boo.smk" with text
    """
    """
    Given I open a file "foo.smk" with text
    """
    <section>:
      conda: "boo.yaml"
    """
    When I put the caret at boo
    Then reference should not resolve
    Examples:
      | section    |
      | rule       |
      | checkpoint |

  Scenario Outline: Resolve to a file in a sub directory
    Given a snakemake project
    Given a file "Dir/boo.yaml" with text
    """
    TEXT
    """
    Given I open a file "foo.smk" with text
    """
    <section>:
      conda: "Dir/boo.yaml"
    """
    When I put the caret at boo
    Then reference should resolve to "TEXT" in "boo.yaml"
    Examples:
      | section    |
      | rule       |
      | checkpoint |

  Scenario Outline: Resolve for strings in different quotes
    Given a snakemake project
    Given a file "boo.yaml" with text
    """
    TEXT
    """
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      conda: <quote>boo.yaml<quote>
    """
    When I put the caret after boo
    Then reference should resolve to "TEXT" in "boo.yaml"
    Examples:
      | quote |
      | "     |
      | """   |
      | '     |

  Scenario Outline: Resolve for fstrings in different quotes
    Given a snakemake project
    Given a file "boo.yaml" with text
    """
    TEXT
    """
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      conda: f<quote>boo.yaml<quote>
    """
    When I put the caret after boo
    Then reference should resolve to "TEXT" in "boo.yaml"
    Examples:
      | quote |
      | "     |
      | """   |
      | '     |

  Scenario Outline: Resolve when string literal is divided
    Given a snakemake project
    Given a file "boo.yaml" with text
    """
    TEXT
    """
    Given I open a file "foo.smk" with text
    """
    <section> NAME:
      conda: "boo"".yaml"
    """
    When I put the caret at boo
    Then reference should resolve to "TEXT" in "boo.yaml"
    Examples:
      | section    |
      | rule       |
      | checkpoint |

  Scenario Outline: Resolve when string literal is crazily divided
    Given a snakemake project
    Given a file "boo.yaml" with text
    """
    TEXT
    """
    Given I open a file "foo.smk" with text
    """
    <section> NAME:
      conda: "" 'b' "o" "o" f'.' f"yaml" ''
    """
    When I put the caret at b
    Then reference should resolve to "TEXT" in "boo.yaml"
    Examples:
      | section    |
      | rule       |
      | checkpoint |

  Scenario Outline: Resolve when language is injected
    Given a snakemake project
    Given a file "boo.yaml" with text
    """
    """
    Given I open a file "foo.smk" with text
    """
    <section> NAME:
      conda: "{boo}.yaml"
    """
    When I put the caret at b
    Then there should be no reference
    Examples:
      | section    |
      | rule       |
      | checkpoint |