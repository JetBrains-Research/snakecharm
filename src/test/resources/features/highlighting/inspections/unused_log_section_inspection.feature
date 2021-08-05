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
        name: "name_{log}"
        shell: "command touch {wildcards.log}"
    """
    And SmkUnusedLogSectionInspection inspection is enabled
    And I expect inspection weak warning on <log: "my_log.log"> with message
    """
    Looks like 'log' file won't be created, because it is not referenced from 'shell' or 'run' sections
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
      | rule_like  | log_variation | mention                       |
      | checkpoint | "my_log.log"  | shell: "command {log}"        |
      | rule       | l1="foo.log"  | shell: "--log {log.l1}"       |
      | checkpoint | l1="foo.log"  | run: shell("foo ff {log[0]}") |

  Scenario Outline: No warnings in 'log' section if used in wrapper
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        log: <log_variation>
        wrapper: "0.60.1/bio/samtools/merge"
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
          log = "Bye"
          print("Hello world!")
          shell("something")
          print(log)
          foo = 5
    """
    And SmkUnusedLogSectionInspection inspection is enabled
    And I expect inspection weak warning on <log: "my_log.log"> with message
    """
    Looks like 'log' file won't be created, because it is not referenced from 'shell' or 'run' sections
    """
    When I check highlighting weak warnings
    Examples:
      | rule_like  |
      | checkpoint |
      | rule       |

  Scenario Outline: There are warnings in 'log' section if 'log' is referenced from another rule
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input: "input.txt"
        output: "output.txt"
        log: "data/log.log"
        threads: 4

    <rule_like> NAME2:
        run:
          print("Hello world!")
          shell("{<rules_like>.NAME.log}")
          foo = 5
    """
    And SmkUnusedLogSectionInspection inspection is enabled
    And I expect inspection weak warning on <log: "data/log.log"> with message
    """
    Looks like 'log' file won't be created, because it is not referenced from 'shell' or 'run' sections
    """
    When I check highlighting weak warnings
    Examples:
      | rule_like  | rules_like  |
      | rule       | rules       |
      | checkpoint | checkpoints |

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
    Looks like 'log' file won't be created, because it is not referenced from 'shell' or 'run' sections
    """
    When I check highlighting weak warnings
    And I invoke quick fix Create 'log' file in 'shell' section and see text:
    """
    <rule_like> NAME:
        input: "input.txt"
        output: "output.txt"
        log: "my_log.log"
        threads: 4
        shell: "command touch >{log} 2>&1"
    """
    Examples:
      | rule_like  |
      | checkpoint |
      | rule       |
    # Impossible to check inspection for 'shell' without arguments
    # because of 'checkHighlighting' implementation

  Scenario Outline: Quick fix for unused in 'run' section 'log' reference
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input: "input.txt"
        output: "output.txt"
        log: "my_log.log"
        threads: 4
        run:
            shell("something")
    """
    And SmkUnusedLogSectionInspection inspection is enabled
    And I expect inspection weak warning on <log: "my_log.log"> with message
    """
    Looks like 'log' file won't be created, because it is not referenced from 'shell' or 'run' sections
    """
    When I check highlighting weak warnings
    And I invoke quick fix Create 'log' file in 'run' section and see text:
    """
    <rule_like> NAME:
        input: "input.txt"
        output: "output.txt"
        log: "my_log.log"
        threads: 4
        run:
            shell("something")
            shell("echo TODO >{log} 2>&1")
    """
    Examples:
      | rule_like  |
      | checkpoint |
      | rule       |

  Scenario Outline: Quick fix for unused in rule/checkpoint 'log' section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input: "input.txt"
        output: "output.txt"
        log: "my_log.log"
        threads: 4
    """
    And SmkUnusedLogSectionInspection inspection is enabled
    And I expect inspection weak warning on <log: "my_log.log"> with message
    """
    Looks like 'log' file won't be created, because it is not referenced from 'shell' or 'run' sections
    """
    When I check highlighting weak warnings
    And I invoke quick fix Create 'shell' section which creates 'log' file and see text:
    """
    <rule_like> NAME:
        input: "input.txt"
        output: "output.txt"
        log: "my_log.log"
        threads: 4
        shell: "echo TODO >{log} 2>&1"
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
    Looks like 'log' file won't be created, because it is not referenced from 'shell' or 'run' sections
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