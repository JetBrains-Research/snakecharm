Feature: Resolve wildcards in SnakemakeSL

  Scenario Outline: Resolve to first occurrence
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input: "{foo} here"
        output: "{foo}"
    """
    When I put the caret after output: "{fo
    Then reference in injection should resolve to "foo} here" in "foo.smk"
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Resolve to wildcard constraints in rule
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input: "{foo}"
        output: "{foo}"
        wildcard_constraints:
          foo=""
    """
    When I put the caret after output: "{fo
    Then reference in injection should resolve to "foo=""" in "foo.smk"
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Resolve to first occurrence from wildcards.
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input: "{foo} here"
        output: "{foo}"
        shell: "{wildcards.foo}"
    """
    When I put the caret after shell: "{wildcards.fo
    Then reference in injection should resolve to "foo} here" in "foo.smk"
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Resolve to wildcard constraints from wildcards.
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input: "{foo}"
        output: "{foo}"
        wildcard_constraints:
          foo=""
        shell: "{wildcards.foo}"
    """
    When I put the caret after shell: "{wildcards.fo
    Then reference in injection should resolve to "foo=""" in "foo.smk"
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Resolve to wildcard constraints in file
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    wildcard_constraints:
          foo="\d+"
    <rule_like> NAME:
        input: "{foo}"
        output: "{foo}"
        wildcard_constraints:
          foo=""
    """
    When I put the caret after output: "{fo
    Then reference in injection should resolve to "foo="\d+"" in "foo.smk"
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Resolve to wildcard constraints in file from wildcards.
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    wildcard_constraints:
          foo="\d+"

    <rule_like> NAME:
        input: "{foo}"
        output: "{foo}"
        wildcard_constraints:
          foo=""
        shell: "{wildcards.foo}"
    """
    When I put the caret after shell: "{wildcards.fo
    Then reference in injection should resolve to "foo="\d+"" in "foo.smk"
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |
