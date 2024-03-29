Feature: SmkRuleRedeclarationInspection inspection
  Scenario: A single SmkRuleRedeclarationInspection
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
    And SmkRuleRedeclarationInspection inspection is enabled
    Then I expect inspection error on <NAME> in <rule NAME: #overrides> with message
    """
    This rule name is already used by another rule declaration.
    """
    When I check highlighting errors

  Scenario: A single SmkRuleRedeclarationInspection with rule defined in 'use' section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    use rule RULE from MODULE as NAME with:
        input: "data.csv"

    rule ANOTHER_NAME:
        output: touch("file.txt")

    rule NAME: #overrides
        output: touch("output.txt")
    """
    And SmkRuleRedeclarationInspection inspection is enabled
    Then I expect inspection error on <NAME> in <rule NAME: #overrides> with message
    """
    This rule name is already used by another rule declaration.
    """
    When I check highlighting errors

  Scenario: A single SmkRuleRedeclarationInspection with two 'use' sections, which defines same rules
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    use rule RULE from MODULE as NAME with:
        input: "data.csv"

    use rule ANOTHER_RULE from MODULE as NAME with: #overrides
        input: "data_v2.csv"
    """
    And SmkRuleRedeclarationInspection inspection is enabled
    Then I expect inspection error on <NAME> in <NAME with: #overrides> with message
    """
    This rule name is already used by another rule declaration.
    """
    When I check highlighting errors

  Scenario: Multiple SmkRuleRedeclarationInspections
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
    And SmkRuleRedeclarationInspection inspection is enabled
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
    And SmkRuleRedeclarationInspection inspection is enabled
    Then I expect inspection error on <NAME> in <rule NAME: #overrides> with message
    """
    This rule name is already used by another rule declaration.
    """
    When I check highlighting errors

  Scenario Outline: SmkRuleRedeclarationInspection rename fix
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> name1:
      input: "input1"

    <rule_like> name1: # duplicate
      input: "input2"
    """
    And SmkRuleRedeclarationInspection inspection is enabled
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

  Scenario Outline: SmkRuleRedeclarationInspection rename does not rename rule usages
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
    And SmkRuleRedeclarationInspection inspection is enabled
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

  Scenario: SmkRuleRedeclarationInspection for rule in if..else..
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
    And SmkRuleRedeclarationInspection inspection is enabled
    Then I expect inspection weak warning on <NAME> in <rule NAME: #overrides> with message
    """
    This rule name is probably used by another rule declaration. Not sure because this rule isn't at file top level.
    """
    When I check highlighting weak warnings

  Scenario: SmkRuleRedeclarationInspection for rule in while
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
    And SmkRuleRedeclarationInspection inspection is enabled
    Then I expect inspection weak warning on <NAME> in <rule NAME: #overrides> with message
    """
    This rule name is probably used by another rule declaration. Not sure because this rule isn't at file top level.
    """
    When I check highlighting weak warnings

  Scenario: SmkRuleRedeclarationInspection for rule in function def
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
       input: "input.txt"

    def foo():
        rule NAME: #overrides
            output: touch("output.txt")
    """
    And SmkRuleRedeclarationInspection inspection is enabled
    Then I expect inspection weak warning on <NAME> in <rule NAME: #overrides> with message
    """
    This rule name is probably used by another rule declaration. Not sure because this rule isn't at file top level.
    """
    When I check highlighting weak warnings


  Scenario Outline: Warn if rule definition redeclared in other file
    Given a snakemake project
    Given a file "a.smk" with text
    """
    <rule_like1> foo:
      input "aa"
    """
    Given I open a file "b.smk" with text
    """
    <rule_like2> foo: # duplicate1
      output: "bb"

    <rule_like2> foo: # duplicate2
      output: "bb"
    """
    And SmkRuleRedeclarationInspection inspection is enabled
    Then I expect inspection weak warning on <foo> in <foo: # duplicate1> with message
    """
    This rule name is probably used by another rule declaration. Not sure because the usage is in the other file.
    """
    Then I expect inspection error on <foo> in <foo: # duplicate2> with message
    """
    This rule name is already used by another rule declaration.
    """
    When I check highlighting weak warnings
    
    Examples:
      | rule_like1 | rule_like2 |
      | rule       | rule       |
      | rule       | checkpoint |
      | checkpoint | rule       |
