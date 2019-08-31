Feature: Rule redeclaration inspection
  Scenario: A single rule redeclaration
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
        input: "input.txt"
        output: "output.txt"
        resources: threads=4, mem_mb=100
        shell: "command"

    rule ANOTHER_NAME:
        output: touch("file.txt")

    rule NAME: #overrides
        output: touch("output.txt")
    """
    And Rule Redeclaration inspection is enabled
    Then I expect inspection error on <NAME> in <rule NAME: #overrides> with message
    """
    This rule name is already used by another rule declaration.
    """
    When I check highlighting errors


  Scenario: Multiple rule redeclarations
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
        input: "input.txt"
        output: "output.txt"
        resources: threads=4, mem_mb=100
        shell: "command"

    rule ANOTHER_NAME:
        output: touch("file.txt")

    rule NAME: #overrides1
        output: touch("output.txt")

    rule ANOTHER_NAME: #overrides
        output: touch("file.txt")

    rule NAME: #overrides2
        output: touch("output.txt")
    """
    And Rule Redeclaration inspection is enabled
    Then I expect inspection error on <NAME> in <rule NAME: #overrides1> with message
    """
    This rule name is already used by another rule declaration.
    """
    And I expect inspection error on <ANOTHER_NAME> in <rule ANOTHER_NAME: #overrides> with message
    """
    This rule name is already used by another rule declaration.
    """
    And I expect inspection error on <NAME> in <rule NAME: #overrides2> with message
    """
    This rule name is already used by another rule declaration.
    """
    When I check highlighting errors

  Scenario: Checkpoint redeclaration
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    checkpoint NAME:
        input: "input.txt"
        output: "output.txt"
        resources: threads=4, mem_mb=100
        shell: "command"

    rule ANOTHER_NAME:
        output: touch("file.txt")

    rule NAME: #overrides
        output: touch("output.txt")
    """
    And Rule Redeclaration inspection is enabled
    Then I expect inspection error on <NAME> in <rule NAME: #overrides> with message
    """
    This rule name is already used by another rule declaration.
    """
    When I check highlighting errors

  Scenario Outline: Rule redeclaration rename fix
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> name1:
      input: "input1"

    <rule_like> name1: # duplicate
      input: "input2"
    """
    And Rule Redeclaration inspection is enabled
    Then I expect inspection error on <name1> in <<rule_like> name1: # duplicate> with message
    """
    This rule name is already used by another rule declaration.
    """
    When I check highlighting errors
    And I invoke quick fix Rename element and see text:
    """
    <rule_like> name1:
      input: "input1"

    <rule_like> SNAKEMAKE_IDENTIFIER: # duplicate
      input: "input2"
    """
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: rule redeclaration rename does not rename rule usages
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    ruleorder: boo > foo1
    localrules: boo

    <rule_like> boo:
      output: "aa"

    rule foo1:
      input: rules.boo

    <rule_like> boo: # duplicate
      input: "aa"

    rule foo2:
      input: rules.boo
    """
    And Rule Redeclaration inspection is enabled
    Then I expect inspection error on <boo> in <boo: # duplicate> with message
    """
    This rule name is already used by another rule declaration.
    """
    When I check highlighting errors
    And I invoke quick fix Rename element and see text:
    """
    ruleorder: boo > foo1
    localrules: boo

    <rule_like> boo:
      output: "aa"

    rule foo1:
      input: rules.boo

    <rule_like> SNAKEMAKE_IDENTIFIER: # duplicate
      input: "aa"

    rule foo2:
      input: rules.boo
    """
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario: Rule redeclaration for rule in if..else..
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    if cond:
        rule NAME:
           input: "input.txt"
    else:
        rule NAME: #overrides
            output: touch("output.txt")
    """
    And Rule Redeclaration inspection is enabled
    Then I expect inspection weak warning on <NAME> in <rule NAME: #overrides> with message
    """
    This rule name is probably used by another rule declaration. Not sure because this rule isn't at file top level.
    """
    When I check highlighting weak warnings

  Scenario: Rule redeclaration for rule in while
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    while cond:
        rule NAME:
           input: "input.txt"
    else:
        rule NAME: #overrides
            output: touch("output.txt")
    """
    And Rule Redeclaration inspection is enabled
    Then I expect inspection weak warning on <NAME> in <rule NAME: #overrides> with message
    """
    This rule name is probably used by another rule declaration. Not sure because this rule isn't at file top level.
    """
    When I check highlighting weak warnings

  Scenario: Rule redeclaration for rule in function def
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
       input: "input.txt"

    def foo():
        rule NAME: #overrides
            output: touch("output.txt")
    """
    And Rule Redeclaration inspection is enabled
    Then I expect inspection weak warning on <NAME> in <rule NAME: #overrides> with message
    """
    This rule name is probably used by another rule declaration. Not sure because this rule isn't at file top level.
    """
    When I check highlighting weak warnings
