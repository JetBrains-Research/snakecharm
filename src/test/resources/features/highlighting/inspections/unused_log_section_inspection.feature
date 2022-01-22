Feature: Rule SmkUnusedLogFileInspection inspection
  Scenario Outline: Unused in non-run section 'log' section
    Given a snakemake project
    And a file "boo.smk" with text
    """
    <rule_like> A:
        shell: "{log}"

    <rule_like> B:
        threads: 4

    <rule_like> C:
        threads: 4
    """
    Given I open a file "foo.smk" with text
    """
    module MODULE:
        snakefile: "boo.smk"

    <rule_like> NAME:
        input: "input.txt"
        output: "output.txt"
        log: "my_log.log"
        threads: 4
        name: "name_{log}"
        shell: "command touch {wildcards.log}"

    use rule A,B,C from MODULE as other_* with:
        log: "other_log.log"

    use rule other_B as new_other_B with:
        log: "new_other_log.log"

    rule C:
        log: "C_log.log"
        shell:
          \"\"\"
          touch {input}
          \"\"\"
    """
    And SmkUnusedLogFileInspection inspection is enabled
    And I expect inspection weak warning on <log: "my_log.log"> with message
    """
    Looks like a log file won't be created, because it is not referenced from 'shell' or 'run' sections
    """
    And I expect inspection weak warning on <log: "other_log.log"> with message
    """
    Looks like a log file won't be created in rule 'B', because it is not referenced from 'shell' or 'run' sections
    """
    And I expect inspection weak warning on <log: "other_log.log"> with message
    """
    Looks like a log file won't be created in rule 'C', because it is not referenced from 'shell' or 'run' sections
    """
    And I expect inspection weak warning on <log: "new_other_log.log"> with message
    """
    Looks like a log file won't be created in rule 'B', because it is not referenced from 'shell' or 'run' sections
    """
    And I expect inspection weak warning on <log: "C_log.log"> with message
    """
    Looks like a log file won't be created, because it is not referenced from 'shell' or 'run' sections
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

    module MODULE:
        snakefile: "https://something/file.smk"

    use rule A,B from MODULE as other_* with:
        log: "other_log.log"

    use rule other_B as new_other_B with:
        log: "new_other_log.log"
    """
    And SmkUnusedLogFileInspection inspection is enabled
    And I expect no inspection weak warnings
    When I check highlighting weak warnings
    Examples:
      | rule_like  | log_variation | mention                       |
      | checkpoint | "my_log.log"  | shell: "command {log}"        |
      | rule       | l1="foo.log"  | shell: "--log {log.l1}"       |
      | checkpoint | l1="foo.log"  | run: shell("foo ff {log[0]}") |
      | rule       | "my_log.log"  | shell: """command {log}"""    |

  Scenario Outline: Warnings in 'log' section, if it is not used in rule which was overridden more than one time
    Given a snakemake project
    And a file "boo1.smk" with text
    """
    <rule_like> NAME:
        shell: "command here"

    use rule NAME as NAME2 with:
        log: "other_log.log"
    """
    Given I open a file "foo.smk" with text
    """
    module M:
        snakefile: "boo.smk"

    use rule NAME2 from M as other_* with:
        log: "new_other_log.log"
    """
    And SmkUnusedLogFileInspection inspection is enabled
    And I expect inspection weak warning on <log: "new_other_log.log"> with message
    """
    Looks like a log file won't be created in rule 'NAME', because it is not referenced from 'shell' or 'run' sections
    """
    When I check highlighting weak warnings
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: No warnings in 'log' section if used in wrapper
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        log: <log_variation>
        wrapper: "0.60.1/bio/samtools/merge"
    """
    And SmkUnusedLogFileInspection inspection is enabled
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
    And SmkUnusedLogFileInspection inspection is enabled
    And I expect inspection weak warning on <log: "my_log.log"> with message
    """
    Looks like a log file won't be created, because it is not referenced from 'shell' or 'run' sections
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
    And SmkUnusedLogFileInspection inspection is enabled
    And I expect inspection weak warning on <log: "data/log.log"> with message
    """
    Looks like a log file won't be created, because it is not referenced from 'shell' or 'run' sections
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
    And SmkUnusedLogFileInspection inspection is enabled
    And I expect inspection weak warning on <log: "my_log.log"> with message
    """
    Looks like a log file won't be created, because it is not referenced from 'shell' or 'run' sections
    """
    When I check highlighting weak warnings
    And I invoke quick fix Append '>{log} 2>&1' to shell section command into 'NAME' and see text:
    """
    <rule_like> NAME:
        input: "input.txt"
        output: "output.txt"
        log: "my_log.log"
        threads: 4
        shell: "(command touch) >{log} 2>&1"
    """
    Examples:
      | rule_like  |
      | checkpoint |
      | rule       |
    # Impossible to check inspection for 'shell' without arguments
    # because of 'checkHighlighting' implementation

  Scenario Outline: Quick fix for unused in 'shell' section 'log' reference. Triple quoted string case
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input: "input.txt"
        output: "output.txt"
        log: "my_log.log"
        threads: 4
        shell:
          \"\"\"
          command touch
          \"\"\"
    """
    And SmkUnusedLogFileInspection inspection is enabled
    And I expect inspection weak warning on <log: "my_log.log"> with message
    """
    Looks like a log file won't be created, because it is not referenced from 'shell' or 'run' sections
    """
    When I check highlighting weak warnings
    And I invoke quick fix Append '>{log} 2>&1' to shell section command into 'NAME' and see text:
    """
    <rule_like> NAME:
        input: "input.txt"
        output: "output.txt"
        log: "my_log.log"
        threads: 4
        shell:
          \"\"\"(
          command touch
          ) >{log} 2>&1\"\"\"
    """
    Examples:
      | rule_like  |
      | checkpoint |
      | rule       |

  Scenario Outline: Quick fix for unused in 'shell' section 'log' reference. F-string case
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input: "input.txt"
        output: "output.txt"
        log: "my_log.log"
        threads: 4
        shell:
          <first_line> foo boo"
          <second_line> -foo -boo"
    """
    And SmkUnusedLogFileInspection inspection is enabled
    And I expect inspection weak warning on <log: "my_log.log"> with message
    """
    Looks like a log file won't be created, because it is not referenced from 'shell' or 'run' sections
    """
    When I check highlighting weak warnings
    And I invoke quick fix Append '>{log} 2>&1' to shell section command into 'NAME' and see text:
    """
    <rule_like> NAME:
        input: "input.txt"
        output: "output.txt"
        log: "my_log.log"
        threads: 4
        shell:
          <first_line>( foo boo"
          <second_line> -foo -boo) ><brackets> 2>&1"
    """
    Examples:
      | rule_like  | first_line | second_line | brackets |
      | checkpoint | "          | f"          | {{log}}  |
      | checkpoint | f"         | "           | {log}    |
      | rule       | "          | f"          | {{log}}  |
      | rule       | f"         | "           | {log}    |

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
    And SmkUnusedLogFileInspection inspection is enabled
    And I expect inspection weak warning on <log: "my_log.log"> with message
    """
    Looks like a log file won't be created, because it is not referenced from 'shell' or 'run' sections
    """
    When I check highlighting weak warnings
    And I invoke quick fix Add 'shell(..)' call that creates crates a log file in 'run' section into 'NAME' and see text:
    """
    <rule_like> NAME:
        input: "input.txt"
        output: "output.txt"
        log: "my_log.log"
        threads: 4
        run:
            shell("something")
            shell("(echo TODO) >{log} 2>&1")
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
    And SmkUnusedLogFileInspection inspection is enabled
    And I expect inspection weak warning on <log: "my_log.log"> with message
    """
    Looks like a log file won't be created, because it is not referenced from 'shell' or 'run' sections
    """
    When I check highlighting weak warnings
    And I invoke quick fix Add 'shell' section that creates a log file into 'NAME' and see text:
    """
    <rule_like> NAME:
        input: "input.txt"
        output: "output.txt"
        log: "my_log.log"
        threads: 4
        shell: "(echo TODO) >{log} 2>&1"
    """
    Examples:
      | rule_like  |
      | checkpoint |
      | rule       |

  Scenario Outline: Quick fix for overridden 'rule', 'checkpoint' and 'use rule'
    Given a snakemake project
    And a file "boo.smk" with text
    """
    <rule_like> NAME1:

    rule NAME2:
        shell: "command touch"

    rule NAME3:
        run:
            shell("something")
    """
    Given I open a file "foo.smk" with text
    """
    module M:
        snakefile: "boo.smk"

    use rule NAME1, NAME2, NAME3 from M as other_* with:
        input: "input.txt"
        output: "output.txt"
        log: "my_log.log"
        threads: 4
    """
    And SmkUnusedLogFileInspection inspection is enabled
    And I expect inspection weak warning on <log: "my_log.log"> with message
    """
    Looks like a log file won't be created in rule 'NAME1', because it is not referenced from 'shell' or 'run' sections
    """
    And I expect inspection weak warning on <log: "my_log.log"> with message
    """
    Looks like a log file won't be created in rule 'NAME2', because it is not referenced from 'shell' or 'run' sections
    """
    And I expect inspection weak warning on <log: "my_log.log"> with message
    """
    Looks like a log file won't be created in rule 'NAME3', because it is not referenced from 'shell' or 'run' sections
    """
    When I check highlighting weak warnings
    And I invoke quick fix Add 'shell' section that creates a log file into 'NAME1' and see text:
    """
    module M:
        snakefile: "boo.smk"

    use rule NAME1, NAME2, NAME3 from M as other_* with:
        input: "input.txt"
        output: "output.txt"
        log: "my_log.log"
        threads: 4
    """
    And I invoke quick fix Append '>{log} 2>&1' to shell section command into 'NAME2' and see text:
    """
    module M:
        snakefile: "boo.smk"

    use rule NAME1, NAME2, NAME3 from M as other_* with:
        input: "input.txt"
        output: "output.txt"
        log: "my_log.log"
        threads: 4
    """
    And I invoke quick fix Add 'shell(..)' call that creates crates a log file in 'run' section into 'NAME3' and see text:
    """
    module M:
        snakefile: "boo.smk"

    use rule NAME1, NAME2, NAME3 from M as other_* with:
        input: "input.txt"
        output: "output.txt"
        log: "my_log.log"
        threads: 4
    """
    Then the file "boo.smk" should have text
    """
    <rule_like> NAME1:
        shell: "(echo TODO) >{log} 2>&1"

    rule NAME2:
        shell: "(command touch) >{log} 2>&1"

    rule NAME3:
        run:
            shell("something")
            shell("(echo TODO) >{log} 2>&1")
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

    rule NAME2:
        <scenario>

    use rule NAME2 as NAME3 with:
        log: "new_log.log"
    """
    And SmkUnusedLogFileInspection inspection is enabled
    And I expect inspection weak warning on <log: "my_log.log"> with message
    """
    Looks like a log file won't be created, because it is not referenced from 'shell' or 'run' sections
    """
    And I expect inspection weak warning on <log: "new_log.log"> with message
    """
    Looks like a log file won't be created in rule 'NAME2', because it is not referenced from 'shell' or 'run' sections
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