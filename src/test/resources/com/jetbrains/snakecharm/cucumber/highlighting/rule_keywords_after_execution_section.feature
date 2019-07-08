Feature: Rule sections after execution sections inspection.
  Execution sections being: run, script, wrapper, shell, cwl.

  Scenario Outline: Params section after shell section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input: "input.txt"
        output: "output.txt"
        shell: "command"
        params: a="value"
    """
    And Rule Section After Execution Section inspection is enabled
    Then I expect inspection error on <params: a="value"> with message
    """
    Rule section 'params' isn't allowed after 'shell' section.
    """
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Threads section after wrapper section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input: "input.txt"
        output: "output.txt"
        wrapper: "dir/wrapper"
        threads: 8
    """
    And Rule Section After Execution Section inspection is enabled
    Then I expect inspection error on <threads: 8> with message
    """
    Rule section 'threads' isn't allowed after 'wrapper' section.
    """
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |


  Scenario Outline: Log section after script section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input: "input.txt"
        output: "output.txt"
        script: "script_file.py"
        log: "log_path"
    """
    And Rule Section After Execution Section inspection is enabled
    Then I expect inspection error on <log: "log_path"> with message
    """
    Rule section 'log' isn't allowed after 'script' section.
    """
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Resources section after cwl section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input: "input.txt"
        output: "output.txt"
        cwl: "https://github.com/repository/with/file.cwl"
        resources: mem_mb=100
    """
    And Rule Section After Execution Section inspection is enabled
    Then I expect inspection error on <resources: mem_mb=100> with message
    """
    Rule section 'resources' isn't allowed after 'cwl' section.
    """
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |


