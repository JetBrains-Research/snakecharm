Feature: Inspection quick fixes

  Scenario Outline: Name an unnamed (positional) resources section argument
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <rule_like> NAME:
        resources: 4
      """
    And Resources Keyword Arguments inspection is enabled
    Then I expect inspection error on <4> with message
      """
      Resources have to be named (e.g. 'threads=4').
      """
    When I check highlighting errors
    And I invoke quick fix Name resource and see text:
    """
    <rule_like> NAME:
      resources: =4
    """
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

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
    This rule name is already used by another rule.
    """
    When I check highlighting errors
    And I invoke quick fix Rename rule and see text:
    """
    <rule_like> name1:
      input: "input1"

    <rule_like> DEFAULT_SNAKEMAKE_RULE_NAME: # duplicate
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
    This rule name is already used by another rule.
    """
    When I check highlighting errors
    And I invoke quick fix Rename rule and see text:
    """
    ruleorder: boo > foo1
    localrules: boo

    <rule_like> boo:
      output: "aa"

    rule foo1:
      input: rules.boo

    <rule_like> DEFAULT_SNAKEMAKE_RULE_NAME: # duplicate
      input: "aa"

    rule foo2:
      input: rules.boo
    """
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Section redeclaration element removal fix test
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> name1:
      input: "input1"
      input: "input2"
      output: "output.txt"
    """
    And Section Redeclaration inspection is enabled
    Then I check highlighting errors
    And I invoke quick fix Remove section and see text:
    """
    <rule_like> name1:
      input: "input1"
      output: "output.txt"
    """
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Swap execution section and rule section below fix test
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> name1:
      input: "input1"
      # comment
      shell: "command"
             "multiline"
             "string"
      resources: a=4
      version: 2.0

    <rule_like> name2:
      input: "input2"
    """
    And Rule Section After Execution Section inspection is enabled
    Then I expect inspection error on <resources: a=4> with message
    """
    Rule section 'resources' isn't allowed after 'shell' section.
    """
    And I expect inspection error on <version: 2.0> with message
    """
    Rule section 'version' isn't allowed after 'shell' section.
    """
    When I check highlighting errors
    And I invoke quick fix Move execution section to the end of the rule and see text:
    """
    <rule_like> name1:
      input: "input1"
      # comment
      resources: a=4
      version: 2.0
      shell: "command"
             "multiline"
             "string"

    <rule_like> name2:
      input: "input2"
    """
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |
