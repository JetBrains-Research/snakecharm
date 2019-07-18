Feature: Inspection quick fixes

  Scenario: Name an unnamed (positional) resources section argument
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      rule NAME:
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
    rule NAME:
      resources: =4
    """

  Scenario: Rule redeclaration rename fix
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule name1:
      input: "input1"

    rule name1: # duplicate
      input: "input2"
    """
    And Rule Redeclaration inspection is enabled
    Then I expect inspection error on <name1> in <rule name1: # duplicate> with message
    """
    This rule name is already used by another rule.
    """
    When I check highlighting errors
    And I invoke quick fix Rename element and see text:
    """
    rule name1:
      input: "input1"

    rule NAME1: # duplicate
      input: "input2"
    """

  Scenario: Section redeclaration element removal fix test
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule name1:
      input: "input1"
      input: "input2"
      output: "output.txt"
    """
    And Section Redeclaration inspection is enabled
    Then I check highlighting errors
    And I invoke quick fix Remove section and see text:
    """
    rule name1:
      input: "input1"
      output: "output.txt"
    """

    # multiple arguments for 'shell' section are not allowed at runtime,
    # this section will be modified when Issue 16 is fixed
  Scenario: Swap execution section and rule section below fix test
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule name1:
      input: "input1"
      shell: "command",
             "multiline",
             "string"
      resources: a=4

    rule name2:
      input: "input2"
    """
    And Rule Section After Execution Section inspection is enabled
    Then I expect inspection error on <resources: a=4> with message
    """
    Rule section 'resources' isn't allowed after 'shell' section.
    """
    When I check highlighting errors
    And I invoke quick fix Move rule section upwards and see text:
    """
    rule name1:
      input: "input1"
      resources: a=4
      shell: "command",
             "multiline",
             "string"

    rule name2:
      input: "input2"
    """
