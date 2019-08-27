Feature: Rename elements in SnakemakeSL

  Scenario Outline: Rename wildcard from first occurrence
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      input: "{foo}"
      output: "{foo}"
      log: "{foo}"
      shell: "{wildcards.foo}"
    """
    When I put the caret after input: "{fo
    When I invoke rename with name "boo"
    Then the file "foo.smk" should have text
    """
    <rule_like> foo:
      input: "{boo}"
      output: "{boo}"
      log: "{boo}"
      shell: "{wildcards.boo}"
    """
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Rename from next occurrence
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      input: "{foo}"
      output: "{foo}"
      log: "{foo}"
      shell: "{wildcards.foo}"
    """
    When I put the caret after output: "{fo
    When I invoke rename with name "boo"
    Then the file "foo.smk" should have text
    """
    <rule_like> foo:
      input: "{boo}"
      output: "{boo}"
      log: "{boo}"
      shell: "{wildcards.boo}"
    """
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Rename from wildcards.
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      input: "{foo}"
      output: "{foo}"
      log: "{foo}"
      shell: "{wildcards.foo}"
    """
    When I put the caret after shell: "{wildcards.fo
    When I invoke rename with name "boo"
    Then the file "foo.smk" should have text
    """
    <rule_like> foo:
      input: "{boo}"
      output: "{boo}"
      log: "{boo}"
      shell: "{wildcards.boo}"
    """
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Rename from wildcard constraints
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      input: "{foo}"
      output: "{foo}"
      wildcard_constraints: foo="\d+"
      log: "{foo}"
      shell: "{wildcards.foo}"
    """
    When I put the caret after wildcard_constraints: fo
    When I invoke rename with name "boo"
    Then the file "foo.smk" should have text
    """
    <rule_like> foo:
      input: "{boo}"
      output: "{boo}"
      wildcard_constraints: boo="\d+"
      log: "{boo}"
      shell: "{wildcards.boo}"
    """
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Rename from first occurrence with wildcard constraints
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      input: "{foo}"
      output: "{foo}"
      wildcard_constraints: foo="\d+"
      log: "{foo}"
      shell: "{wildcards.foo}"
    """
    When I put the caret after input: "{fo
    When I invoke rename with name "boo"
    Then the file "foo.smk" should have text
    """
    <rule_like> foo:
      input: "{boo}"
      output: "{boo}"
      wildcard_constraints: boo="\d+"
      log: "{boo}"
      shell: "{wildcards.boo}"
    """
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Rename from wildcards. with wildcard constraints
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      input: "{foo}"
      output: "{foo}"
      wildcard_constraints: foo="\d+"
      log: "{foo}"
      shell: "{wildcards.foo}"
    """
    When I put the caret after shell: "{wildcards.fo
    When I invoke rename with name "boo"
    Then the file "foo.smk" should have text
    """
    <rule_like> foo:
      input: "{boo}"
      output: "{boo}"
      wildcard_constraints: boo="\d+"
      log: "{boo}"
      shell: "{wildcards.boo}"
    """
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Rename from subscription expression
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      input: a="input.txt"
      shell: "{input.a} {input[a]}"
    """
    When I put the caret after input[a
    When I invoke rename with name "boo"
    Then the file "foo.smk" should have text
    """
    <rule_like> foo:
      input: boo="input.txt"
      shell: "{input.boo} {input[boo]}"
    """
    When I put the caret after input[bo
    Then reference in injection should resolve to "boo=" in "foo.smk"
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Rename from wildcard constraints in file
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    wildcard_constraints: foo="\d+"

    <rule_like> NAME:
        input: "{foo}"
        output: "{foo}"
        shell: "{wildcards.foo}"
    """
    When I put the caret after wildcard_constraints: fo
    When I invoke rename with name "boo"
    Then the file "foo.smk" should have text
    """
    wildcard_constraints: boo="\d+"

    <rule_like> NAME:
        input: "{boo}"
        output: "{boo}"
        shell: "{wildcards.boo}"
    """
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |
