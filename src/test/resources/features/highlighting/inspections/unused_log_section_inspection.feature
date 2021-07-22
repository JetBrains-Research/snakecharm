Feature: Rule SmkUnusedLogSectionInspection inspection
  Scenario Outline: Unused in 'shell' section 'log' reference
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input: "input.txt"
        output: "output.txt"
        log: "my_log.log"
        threads: 4
        shell: "command touch"
    """
    And SmkUnusedLogSectionInspection inspection is enabled
    And I expect inspection weak warning on <log: "my_log.log"> with message
    """
    Section 'log' is never used in 'shell' section
    """
    When I check highlighting weak warnings
    Examples:
      | rule_like  |
      | checkpoint |
      | rule       |

  Scenario Outline: No warnings in 'shell' section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input: "input.txt"
        output: "output.txt"
        log: <log_variation>
        threads: 4
        shell: "<shell_variation>"
    """
    And SmkUnusedLogSectionInspection inspection is enabled
    And I expect no inspection weak warnings
    When I check highlighting weak warnings
    Examples:
      | rule_like  |  | log_variation | shell_variation |
      | checkpoint |  | "my_log.log"  | command {log}   |
      | rule       |  | l1="foo.log"  | -=log {log.l1}  |

  Scenario Outline: Unused in 'run' section 'log' reference
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input: "input.txt"
        output: "output.txt"
        log: "my_log.log"
        threads: 4
        run:
          print("Hello world!")
          shell("something")
          foo = 5
    """
    And SmkUnusedLogSectionInspection inspection is enabled
    And I expect inspection weak warning on <log: "my_log.log"> with message
    """
    Section 'log' is never used in 'shell' section
    """
    When I check highlighting weak warnings
    Examples:
      | rule_like  |
      | checkpoint |
      | rule       |

  Scenario Outline: No warnings in 'shell' section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input: "input.txt"
        output: "output.txt"
        log: <log_variation>
        threads: 4
        run:
          print("Hello world!")
          shell("<shell_variation>")
          foo = 5
    """
    And SmkUnusedLogSectionInspection inspection is enabled
    And I expect no inspection weak warnings
    When I check highlighting weak warnings
    Examples:
      | rule_like  | log_variation | shell_variation |
      | checkpoint | "my_log.log"  | command {log}   |
      | checkpoint | l1="foo.log"  | -=log {log.l1}  |
      | rule       | "my_log.log"  | command {log}   |
      | rule       | l1="foo.log"  | -=log {log.l1}  |

  Scenario Outline: Quick fix for unused in 'shell' section 'log' reference
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input: "input.txt"
        output: "output.txt"
        log: "my_log.log"
        threads: 4
        shell: "command touch"
    """
    And SmkUnusedLogSectionInspection inspection is enabled
    And I expect inspection weak warning on <log: "my_log.log"> with message
    """
    Section 'log' is never used in 'shell' section
    """
    When I check highlighting weak warnings
    And I invoke quick fix Remove unused 'log' section and see text:
    """
    <rule_like> NAME:
        input: "input.txt"
        output: "output.txt"
        threads: 4
        shell: "command touch"
    """
    Examples:
      | rule_like  |
      | checkpoint |
      | rule       |