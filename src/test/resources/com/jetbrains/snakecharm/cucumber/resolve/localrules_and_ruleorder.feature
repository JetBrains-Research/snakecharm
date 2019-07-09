Feature: Resolve for rules in localrules and ruleorder

  Scenario Outline: Resolve in localrules section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule aaaa:
      input: "input.txt"

    rule bbbb:
      output: touch("output.txt")

    rule cccc:
      output: touch("_output.txt")

    localrules: <text>
    """
    When I put the caret after <ptn>
    Then reference should resolve to "<symbol_name>" in "<file>"

    Examples:
      | ptn                  | text       | symbol_name | file         |
      | localrules: aaa      | aaaa       | aaaa        | foo.smk      |
      | localrules: bbb      | bbbb       | bbbb        | foo.smk      |
      | localrules: ccc      | cccc       | cccc        | foo.smk      |
      | localrules: aaaa, bb | aaaa, bbbb | bbbb        | foo.smk      |

  Scenario Outline: Resolve in localrules section above all rule declarations
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    localrules: <text>

    rule aaaa:
      input: "input.txt"

    rule bbbb:
      output: touch("output.txt")

    checkpoint cccc:
      output: touch("_output.txt")
    """
    When I put the caret after <ptn>
    Then reference should resolve to "<symbol_name>" in "<file>"

    Examples:
      | ptn                  | text       | symbol_name | file         |
      | localrules: aaa      | aaaa       | aaaa        | foo.smk      |
      | localrules: bbb      | bbbb       | bbbb        | foo.smk      |
      | localrules: ccc      | cccc       | cccc        | foo.smk      |
      | localrules: aaaa, bb | aaaa, bbbb | bbbb        | foo.smk      |

  Scenario Outline: Resolve in ruleorder section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule aaaa:
      input: "input.txt"

    rule bbbb:
      output: touch("output.txt")

    checkpoint cccc:
      output: touch("_output.txt")

    ruleorder: aaaa > <text>
    """
    When I put the caret after <ptn>
    Then reference should resolve to "<symbol_name>" in "<file>"

    Examples:
      | ptn             | text      | symbol_name | file         |
      | > bbb           | bbbb      | bbbb        | foo.smk      |
      | > ccc           | cccc      | cccc        | foo.smk      |


  Scenario Outline: Resolve in ruleorder section above all rule declarations
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    ruleorder: aaaa > <text>

    rule aaaa:
      input: "input.txt"

    rule bbbb:
      output: touch("output.txt")

    checkpoint cccc:
      output: touch("_output.txt")
    """
    When I put the caret after <ptn>
    Then reference should resolve to "<symbol_name>" in "<file>"

    Examples:
      | ptn             | text      | symbol_name | file         |
      | > bbb           | bbbb      | bbbb        | foo.smk      |
      | > ccc           | cccc      | cccc        | foo.smk      |
      | ruleorder: aa   | aaaa      | aaaa        | foo.smk      |

  Scenario Outline: Multiresolve in ruleorder section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    ruleorder: aaaa

    rule aaaa: #1
      input: "input.txt"

    rule aaaa: #2
      output: touch("output.txt")

    checkpoint aaaa: #3
      output: touch("_output.txt")
    """
    When I put the caret after <ptn>
    Then reference should multi resolve to name, file, times
      | aaaa (SMKRule)       | foo.smk | 2 |
      | aaaa (SMKCheckPoint) | foo.smk | 1 |

    Examples:
      | ptn             |
      | ruleorder: aa   |
      | ruleorder: aa   |
      | ruleorder: aa   |



