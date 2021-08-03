Feature: Rule SmkUnusedLogSectionInspection inspection
  Scenario Outline: Unused in non-run section 'log' section
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
    Section 'log' is never used
    """
    When I check highlighting weak warnings
    Examples:
      | rule_like  |
      | checkpoint |
      | rule       |

  Scenario Outline: No warnings in 'log' section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input: "input.txt"
        output: "output.txt"
        log: <log_variation>
        threads: 4
        <mention>
    """
    And SmkUnusedLogSectionInspection inspection is enabled
    And I expect no inspection weak warnings
    When I check highlighting weak warnings
    Examples:
      | rule_like  | log_variation | mention                            |
      | checkpoint | "my_log.log"  | shell: "command {log}"             |
      | rule       | l1="foo.log"  | shell: "--log {log.l1}"            |
      | checkpoint | l1="foo.log"  | run: shell("foo ff {log[0]}")      |
      | rule       | "log.log"     | name: "{log}"                      |

  Scenario Outline: No warnings in 'log' section if used in wrapper
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        log: <log_variation>
        wrapper 0.60.1/bio/samtools/merge"
    """
    And SmkUnusedLogSectionInspection inspection is enabled
    And I expect no inspection weak warnings
    When I check highlighting weak warnings
    Examples:
      | rule_like  | log_variation |
      | rule       | "log.log"     |
      | checkpoint | "my_log.log"  |
      | rule       | l1="foo.log"  |

  Scenario Outline: Unused in 'run' section 'log' section
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
    Section 'log' is never used
    """
    When I check highlighting weak warnings
    Examples:
      | rule_like  |
      | checkpoint |
      | rule       |

  Scenario Outline: No warnings in 'log' section. 'log' is referenced from another rule
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input: "input.txt"
        output: "output.txt"
        log: <log_variation>
        threads: 4

    <rule_like> NAME2:
        run:
          print("Hello world!")
          shell("<shell_variation>")
          foo = 5
    """
    And SmkUnusedLogSectionInspection inspection is enabled
    And I expect no inspection weak warnings
    When I check highlighting weak warnings
    Examples:
      | rule_like  | log_variation | shell_variation           |
      | checkpoint | "my_log.log"  | command {checkpoints.NAME.log}  |
      | checkpoint | l1="foo.log"  | -=log {checkpoints.NAME.log.l1} |
      | checkpoint | "foo", "boo"  | {checkpoints.NAME.log[1]}       |
      | rule       | "my_log.log"  | command {rules.NAME.log}  |
      | rule       | l1="foo.log"  | -=log {rules.NAME.log.l1} |
      | rule       | "foo", "boo"  | {rules.NAME.log[1]}       |

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
    Section 'log' is never used
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

  Scenario Outline: Confusing scenarios of unused 'log' section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input: "input.txt"
        output: "output.txt"
        log: "my_log.log"
        threads: 4
        <scenario>
    """
    And SmkUnusedLogSectionInspection inspection is enabled
    And I expect inspection weak warning on <log: "my_log.log"> with message
    """
    Section 'log' is never used
    """
    When I check highlighting weak warnings
    Examples:
      | rule_like  | scenario                  |
      | checkpoint | shell: "echo {boo.log}"   |
      | checkpoint | shell: "echo config[log]" |
      | checkpoint | shell: "log.log"          |
      | checkpoint | input: "/foo/{log}.txt"   |
      | rule       | shell: "echo {boo.log}"   |
      | rule       | shell: "echo config[log]" |
      | rule       | shell: "log.log"          |
      | rule       | input: "/foo/{log}.txt"   |