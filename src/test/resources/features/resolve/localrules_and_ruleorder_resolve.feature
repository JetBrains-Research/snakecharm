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

    use rule cccc as dddd with:
      output: touch("_output_dddd.txt")

    localrules: <text>
    """
    When I put the caret after <ptn>
    Then reference should resolve to "<symbol_name>" in "<file>"

    Examples:
      | ptn                  | text       | symbol_name | file    |
      | localrules: aaa      | aaaa       | aaaa        | foo.smk |
      | localrules: bbb      | bbbb       | bbbb        | foo.smk |
      | localrules: ccc      | cccc       | cccc        | foo.smk |
      | localrules: aaaa, bb | aaaa, bbbb | bbbb        | foo.smk |
      | localrules: ddd      | dddd       | dddd        | foo.smk |

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

    use rule cccc as dddd with:
      output: touch("_output_dddd.txt")
    """
    When I put the caret after <ptn>
    Then reference should resolve to "<symbol_name>" in "<file>"

    Examples:
      | ptn                  | text       | symbol_name | file    |
      | localrules: aaa      | aaaa       | aaaa        | foo.smk |
      | localrules: bbb      | bbbb       | bbbb        | foo.smk |
      | localrules: ccc      | cccc       | cccc        | foo.smk |
      | localrules: aaaa, bb | aaaa, bbbb | bbbb        | foo.smk |
      | localrules: ddd      | dddd       | dddd        | foo.smk |

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

    use rule cccc as dddd with:
      output: touch("_output_dddd.txt")

    ruleorder: aaaa > <text>
    """
    When I put the caret after <ptn>
    Then reference should resolve to "<symbol_name>" in "<file>"

    Examples:
      | ptn   | text | symbol_name | file    |
      | > bbb | bbbb | bbbb        | foo.smk |
      | > ccc | cccc | cccc        | foo.smk |
      | > ddd | dddd | dddd        | foo.smk |


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

    use rule cccc as dddd with:
      output: touch("_output_dddd.txt")
    """
    When I put the caret after <ptn>
    Then reference should resolve to "<symbol_name>" in "<file>"

    Examples:
      | ptn           | text | symbol_name | file    |
      | > bbb         | bbbb | bbbb        | foo.smk |
      | > ccc         | cccc | cccc        | foo.smk |
      | ruleorder: aa | aaaa | aaaa        | foo.smk |
      | > ddd         | dddd | dddd        | foo.smk |

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
    Then reference should multi resolve to name, file, times[, class name]
      | aaaa | foo.smk | 2 | SmkRuleImpl       |
      | aaaa | foo.smk | 1 | SmkCheckPointImpl |

    Examples:
      | ptn             |
      | ruleorder: aa   |
      | ruleorder: aa   |
      | ruleorder: aa   |

  Scenario Outline: Multiresolve in localrules section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    localrules: aaaa, bbbb

    rule aaaa:
      input: "input.txt"

    rule aaaa:
      output: touch("output.txt")

    checkpoint aaaa:
      output: touch("_output.txt")

    rule bbbb:
      output: touch("output1.txt")
    """
    When I put the caret after <ptn>
    Then reference should multi resolve to name, file, times[, class name]
      | aaaa | foo.smk | 2 | SmkRuleImpl       |
      | aaaa | foo.smk | 1 | SmkCheckPointImpl |

    Examples:
      | ptn            |
      | localrules: aa |
      | localrules: aa |
      | localrules: aa |

  Scenario Outline: Resolve even for declarations from files present but not included for localrules/ruleorder section
    Given a snakemake project
    Given a file "boo.smk" with text
    """
    rule dddd:
      input: "path/to/input"

    use rule dddd as eeee with:
      output: touch("_output_dddd.txt")
    """
    Given I open a file "foo.smk" with text
    """
    <section>: aaaa <separator> <text>

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
      | ptn             | text | section    | separator | symbol_name | file    |
      | <separator> ddd | dddd | localrules | ,         | dddd        | boo.smk |
      | <separator> ddd | dddd | ruleorder  | >         | dddd        | boo.smk |
      | <separator> eee | eeee | localrules | ,         | eeee        | boo.smk |
      | <separator> eee | eeee | ruleorder  | >         | eeee        | boo.smk |

  Scenario Outline: Resolve in localrules/ruleorder section for rules from included files
    Given a snakemake project
    Given a file "boo.smk" with text
    """
    rule dddd:
      input: "path/to/input"

    checkpoint eeee:
        input: "path/to/input/2"

    use rule eeee as ffff with:
      output: touch("_output_dddd.txt")
    """
    Given I open a file "foo.smk" with text
    """
    <section>: aaaa <separator> <text>

    include: "boo.smk"

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
      | ptn             | text | symbol_name | file    | section    | separator |
      | <separator> ddd | dddd | dddd        | boo.smk | localrules | ,         |
      | <separator> ddd | dddd | dddd        | boo.smk | ruleorder  | >         |
      | <separator> eee | eeee | eeee        | boo.smk | localrules | ,         |
      | <separator> eee | eeee | eeee        | boo.smk | ruleorder  | >         |
      | <separator> fff | ffff | ffff        | boo.smk | localrules | ,         |
      | <separator> fff | ffff | ffff        | boo.smk | ruleorder  | >         |

  Scenario Outline: Resolve in localrules/ruleorder section for rules from files included in other files
    Given a snakemake project
    Given a file "boo.smk" with text
    """
    rule dddd:
      input: "path/to/input"

    use rule dddd as ffff with:
      output: touch("_output_dddd.txt")
    """
    Given a file "soo.smk" with text
    """
    include: "boo.smk"
    checkpoint eeee:
        input: "path/to/input/2"
    """
    Given a file "goo.smk" with text
    """
    include: "soo.smk"
    """
    Given I open a file "foo.smk" with text
    """
    <section>: aaaa <separator> <text>

    include: "goo.smk"

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
      | ptn             | text | symbol_name | file    | section    | separator |
      | <separator> ddd | dddd | dddd        | boo.smk | localrules | ,         |
      | <separator> ddd | dddd | dddd        | boo.smk | ruleorder  | >         |
      | <separator> eee | eeee | eeee        | soo.smk | localrules | ,         |
      | <separator> eee | eeee | eeee        | soo.smk | ruleorder  | >         |
      | <separator> fff | ffff | ffff        | boo.smk | localrules | ,         |
      | <separator> fff | ffff | ffff        | boo.smk | ruleorder  | >         |



