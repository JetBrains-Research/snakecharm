Feature: Wildcard usage without 'wildcards.' prefix in shell section

  Scenario Outline: No wildcards in shell section, only rule parameters
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> rule1:
      input: "file.txt"
      output: "output.txt"
      params: a="text"
      shell: "command {input} --key {params.a} > {output}"
    """
    And Wildcards in Shell Section inspection is enabled
    Then I expect no inspection error
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Wildcards with prefix
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> rule1:
      input: "file.txt"
      output: "output.txt"
      shell: "command {input} --key {wildcards.a} > {output}"
    """
    And Wildcards in Shell Section inspection is enabled
    Then I expect no inspection error
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Wildcard without prefix
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> rule1:
      input: "file.txt"
      output: "output.txt"
      shell: "command {input} --key {a} > {output}"
    """
    And Wildcards in Shell Section inspection is enabled
    Then I expect inspection error on <a> in <{a}> with message
    """
    'wildcards' in 'shell' section can be used only with 'wildcards.' prefix
    """
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Toplevel variable in shell section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    a = 1

    <rule_like> rule1:
      input: "file.txt"
      output: "output.txt"
      shell: "command {input} --key {a} > {output}"
    """
    And Wildcards in Shell Section inspection is enabled
    Then I expect no inspection error
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Toplevel variable included from another file
    Given a snakemake project
    Given a file "boo.smk" with text
    """
    b = 2
    """
    Given I open a file "foo.smk" with text
    """
    a = 1

    include: "boo.smk"

    <rule_like> rule1:
      shell: "echo {a} {b}"
    """
    And Wildcards in Shell Section inspection is enabled
    Then I expect no inspection error
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |

  Scenario Outline: Local variable from an included file
    Given a snakemake project
    Given a file "boo.smk" with text
    """
    def foo():
        b = 2
    """
    Given I open a file "foo.smk" with text
    """
    a = 1

    include: "boo.smk"

    <rule_like> rule1:
      shell: "echo {a} {b}"
    """
    And Wildcards in Shell Section inspection is enabled
    Then I expect inspection error on <b> in <{b}> with message
    """
    'wildcards' in 'shell' section can be used only with 'wildcards.' prefix
    """
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |
      | checkpoint |

