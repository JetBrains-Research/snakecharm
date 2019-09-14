Feature: Resolve wildcards in SnakemakeSL

  Scenario Outline: Resolve to definition
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input: "{foo}"
        output: "{foo} here"
    """
    When I put the caret after input: "{fo
    Then reference in injection should resolve to "foo" in context "foo} here" in file "foo.smk"
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
    When I put the caret after input: "{fo
    Then reference in injection should resolve to "foo=""" in "foo.smk"
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Resolve to definition from wildcards.
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input: "{foo}"
        output: "{foo} here"
        shell: "{wildcards<accessor>}"
    """
    When I put the caret after <signature>
    Then reference in injection should resolve to "foo" in context "foo} here" in file "foo.smk"
    Examples:
      | rule_like  | accessor | signature             |
      | rule       | .foo     | shell: "{wildcards.fo |
      | rule       | [foo]    | shell: "{wildcards[fo |
      | checkpoint | .foo     | shell: "{wildcards.fo |

  Scenario Outline: Resolve to second definition from wildcards.
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input: "{foo}"
        output: "{aaa}"
        log: "{foo} here"
        benchmark: "{foo}"
        shell: "{wildcards.foo}"
    """
    When I put the caret after shell: "{wildcards.fo
    Then reference in injection should resolve to "foo" in context "foo} here" in file "foo.smk"
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

  Scenario Outline: Do not resolve expand injections as wildcards
    Given a snakemake project
    Given I open a file "foo.smk" with text
     """
     <rule_like> NAME:
         output:
             expand("{prefix}", prefix="p")
         shell: "{wildcards.prefix}"
     """
    When I put the caret after wildcards.pref
    Then there should be no reference
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Resolve in rule run section to wildcard declaration
    Given a snakemake project
    Given I open a file "foo.smk" with text
       """
       <rule_like> NAME:
           output:  "{w1}/{w2}"
           log:  "{w3}"
           benchmark:  "{w4}"
           run:
              wildcards.w<idx>
       """
    And I put the caret after wildcards.w
    Then reference should resolve to "w<idx>" in "foo.smk"
    Examples:
      | rule_like  | idx |
      | rule       |  1  |
      | rule       |  2  |
      | rule       |  3  |
      | rule       |  4  |
      | checkpoint |  1  |

  Scenario Outline: Resolve non-declared wildcards to expanding section (run section)
    Given a snakemake project
    Given I open a file "foo.smk" with text
       """
       <rule_like> NAME:
           <section>:  "{sample}" #def
           run:
              wildcards.sample
       """
    And I put the caret after wildcards.sa
    Then reference should resolve to "sample" in context "sample}" #def" in file "foo.smk"
    Examples:
      | rule_like  | section |
      | rule       |  input  |
      | rule       |  params  |
      | checkpoint |  input  |

  Scenario Outline: Resolve non-declared wildcards to expanding section (in injection)
    Given a snakemake project
    Given I open a file "foo.smk" with text
          """
          <rule_like> NAME:
              <section>:  "{sample}" #def
              <text>sample}" # here

          """
    And I put the caret at ample}" # here
    Then reference in injection should resolve to "sample" in context "sample}" #def" in file "foo.smk"
    Examples:
      | rule_like  | section | text                |
      | rule       | input   | shell: "{wildcards. |
      | rule       | params  | shell: "{wildcards. |
      | rule       | input   | params: "{          |
      | rule       | params  | input: "{           |
      | checkpoint | input   | shell: "{wildcards. |

  Scenario Outline: Do not resolve in rule run section if wildcard from non-declaring and non-expanding section
    Given a snakemake project
    Given I open a file "foo.smk" with text
       """
       <rule_like> NAME:
           output:  "{w1}/{w2}"
           <section>:  "{sample}"
           run:
              wildcards.sample
       """
    And I put the caret after wildcards.sa
    Then reference should not resolve
    Examples:
      | rule_like | section |
      | rule      | message |
      | rule      | shell   |

  Scenario Outline: Do not resolve after dot in wildcard references which looks like qualified
    Given a snakemake project
    Given I open a file "foo.smk" with text
       """
       <rule_like> NAME:
           output: arg = "{input}"
           params: <text>
       """
    And I put the caret after <signature>
    Then reference in injection should not resolve
    Examples:
      | rule_like  | text                | signature     |
      | rule       | "{wildcards.input}" | {wildcards.in |
      | rule       | "{output.arg}"      | {output.ar    |
      | checkpoint | "{wildcards.input}" | {wildcards.in |

  Scenario Outline: Resolve first part of qualified like wildcard references into itself
    Given a snakemake project
    Given I open a file "foo.smk" with text
       """
       <rule_like> NAME:
           output: arg = "{input}"
           params: "{<text>
       """
    And I put the caret after <signature>
    Then reference in injection should resolve to "<resolve_target>" in context "<text>" in file "foo.smk"
    Examples:
      | rule_like  | text                | signature     | resolve_target |
      | rule       | wildcards.input}" | {wildcar      | wildcards      |
      | rule       | output.arg}"      | {outp         | output         |
      | rule       | input.arg}"       | params: "{inp | input          |
      | checkpoint | output.arg}"      | {outp         | output         |
