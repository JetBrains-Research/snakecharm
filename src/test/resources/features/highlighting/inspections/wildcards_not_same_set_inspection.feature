Feature: Inspection - SmkNotSameWildcardsSetInspection

  Scenario Outline: Missing wildcards when output section is generator
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <rule_like> NAME <addition>:
        log:
          log1 = "{a}.log1",
          log2 = "{a}/{b}.log2",
          log3 = "{b}.log3"
        output: "{a}.{b}"
        benchmark: "{b}.{c}"
      """
    And SmkNotSameWildcardsSetInspection inspection is enabled
    Then I expect inspection error on <log1 = "{a}.log1"> with message
      """
      Missing wildcards: 'b', 'c'.
      """
    And I expect inspection error on <log2 = "{a}/{b}.log2"> with message
      """
      Missing wildcards: 'c'.
      """
    Then I expect inspection error on <log3 = "{b}.log3"> with message
      """
      Missing wildcards: 'a', 'c'.
      """
    And I expect inspection error on <"{a}.{b}"> with message
      """
      Missing wildcards: 'c'.
      """
    And I expect inspection error on <"{b}.{c}"> with message
      """
      Missing wildcards: 'a'.
      """
    When I check highlighting errors
    Examples:
      | rule_like  | addition      |
      | rule       |               |
      | checkpoint |               |
      | use rule   | as NAME2 with |

    Scenario Outline: No missing wildcards, if defining section was overridden
      Given a snakemake project
      Given I open a file "foo.smk" with text
      """
      <rule_like> NAME:
        output: "{sample}"

      use rule NAME as new_NAME with:
        output: "{sample1}"
      """
      And SmkNotSameWildcardsSetInspection inspection is enabled
      Then I expect no inspection errors
      When I check highlighting errors
      Examples:
        | rule_like  |
        | rule       |
        | checkpoint |

  Scenario Outline: Missing wildcards, defining in non overridden section
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <rule_like> NAME:
        output: "{sample}"

      use rule NAME as new_NAME with: log: "{sample1}"
      """
    And SmkNotSameWildcardsSetInspection inspection is enabled
    Then I expect inspection error on <use rule NAME as new_NAME with: log: "{sample1}"> with message
      """
      Missing wildcards: 'sample1' in inherited 'output' section.
      """
    Then I expect inspection error on <"{sample1}"> with message
      """
      Missing wildcards: 'sample'.
      """
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: No error if cannot detect any wildcards defined in non overridden section
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <rule_like> NAME:
        output: foo()

      use rule NAME as new_NAME with: log: "{sample1}"
      """
    And SmkNotSameWildcardsSetInspection inspection is enabled
    Then I expect inspection weak warning on <use rule NAME as new_NAME with: log: "{sample1}"> with message
      """
      Cannot check missing wildcards in inherited 'output' section.
      """
    When I check highlighting weak warnings
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: No missing wildcards, because declaration section was overriden
    Given a snakemake project
    And a file "boo.smk" with text
    """
    <rule_like> NAME1:
        input: "{sample1}.in"
        output: "{sample1}.out"
        shell: "touch {output}"

    <rule_like> NAME2:
        input: "{sample2}.in"
        output: "{sample2}.out"
        shell: "touch {output}"
    """
    Given I open a file "foo.smk" with text
      """
      module M:
        snakefile: "boo.smk"

      use rule NAME1,NAME2 from M as other_* with:
        output:
          "{sample}.out1"
        log: "{sample}.out2"
      """
    And SmkNotSameWildcardsSetInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Missing wildcards in overridden rule or rules
    Given a snakemake project
    And a file "boo.smk" with text
    """
    <rule_like> NAME1:
        input: "{sample1}.in"
        output: "{sample1}.{sample2}.out"
        benchmark: "{sample1}.{sample2}.out"
        shell: "touch {output}"

    <rule_like> NAME2:
        input: "{sample2}.in"
        output: "{sample1}.{sample2}.out"
        benchmark: "{sample1}.{sample2}.out"
        shell: "touch {output}"
    """
    Given I open a file "foo.smk" with text
      """
      module M:
        snakefile: "boo.smk"

      <rule_like> NAME3:
        output: "{sample1}.{sample2}"

      use rule <inheritance_info> as other_* with:
        log: "{sample1}.out2"
      """
    And SmkNotSameWildcardsSetInspection inspection is enabled
    And I expect inspection error on <"{sample1}.out2"> with message
      """
      Missing wildcards: 'sample2'.
      """
    When I check highlighting errors
    Examples:
      | rule_like  | inheritance_info   |
      | rule       | NAME3              |
      | rule       | NAME1,NAME2 from M |
      | checkpoint | NAME3              |
      | checkpoint | NAME1,NAME2 from M |

  Scenario Outline: Missing wildcards when log section is generator
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <rule_like> NAME:
        benchmark: "{b}.{c}"
        log: "{a}.{b}.{c}.{d}"
      """
    And SmkNotSameWildcardsSetInspection inspection is enabled
    And I expect inspection error on <"{b}.{c}"> with message
      """
      Missing wildcards: 'a', 'd'.
      """
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: No wildcards missing
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      output: "{a}.{b}"
      benchmark: "{a}.{b}"
      log: "{a}.{b}"
    """
    And SmkNotSameWildcardsSetInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Empty sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <rule_like> NAME:
        output: "{a}"
        log:
          log2 = "{a}",
          log1 = ""
        benchmark: "" #here
      """
    And SmkNotSameWildcardsSetInspection inspection is enabled
    Then I expect inspection error on <""> in <"" #here> with message
      """
      Missing wildcards: 'a'.
      """
    And I expect inspection error on <log1 = ""> with message
      """
      Missing wildcards: 'a'.
      """
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Two rules not interfere
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <rule_like> NAME1:
        output: "{a}.{b}"
        log: "{a}.{d}"

      <rule_like> NAME2:
        output: "{x}.{y}"
        log: "{x}.{z}"
      """
    And SmkNotSameWildcardsSetInspection inspection is enabled
    Then I expect inspection error on <"{a}.{b}"> with message
      """
      Missing wildcards: 'd'.
      """
    Then I expect inspection error on <"{a}.{d}"> with message
      """
      Missing wildcards: 'b'.
      """
    Then I expect inspection error on <"{x}.{y}"> with message
      """
      Missing wildcards: 'z'.
      """
    Then I expect inspection error on <"{x}.{z}"> with message
      """
      Missing wildcards: 'y'.
      """
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Do not be confused by wildcards from non-defining sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME1:
      input: "{z}"
      output: "{a}.{b}"
      log: "{a}.{z}"
    """
    And SmkNotSameWildcardsSetInspection inspection is enabled
    Then I expect inspection error on <"{a}.{b}"> with message
    """
    Missing wildcards: 'z'.
    """
    Then I expect inspection error on <"{a}.{z}"> with message
    """
    Missing wildcards: 'b'.
    """
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Cannot parse section wildcards
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <rule_like> NAME:
          <def1>: "{foo}"
          <def2>: <def2_body>
      """
    And SmkNotSameWildcardsSetInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | rule_like  | def1   | def2      | def2_body   |
      | rule       | output | log       | **foo()     |
      | rule       | output | benchmark | foo()       |
      | rule       | log    | benchmark | unpack(foo) |
      | checkpoint | output | log       | **foo()     |

  Scenario Outline: Cannot parse wildcard defining section but 2 other different set
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <rule_like> NAME:
          output: **foo()
          log: "{foo}"
          benchmark: "{sample}.txt"
      """
    And SmkNotSameWildcardsSetInspection inspection is enabled
    Then I expect inspection error on <"{foo}"> with message
      """
      Missing wildcards: 'sample'.
      """
    Then I expect inspection error on <"{sample}.txt"> with message
      """
      Missing wildcards: 'foo'.
      """
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Empty defining section but 2 other different set
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <rule_like> NAME:
         output: ""
         log: "{foo}"
         benchmark: "{sample}.txt"
      """
    And SmkNotSameWildcardsSetInspection inspection is enabled
    Then I expect inspection error on <""> with message
      """
      Missing wildcards: 'foo', 'sample'.
      """
    Then I expect inspection error on <"{foo}"> with message
      """
      Missing wildcards: 'sample'.
      """
    Then I expect inspection error on <"{sample}.txt"> with message
      """
      Missing wildcards: 'foo'.
      """
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |


  Scenario Outline: Do not look for wildcards in `run:` section:
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <rule_like> NAME:
         output: "{sample}"
         run:
            shell("curl {config} {output} | gunzip > {output[0]}")
      """
    And SmkNotSameWildcardsSetInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Issue 266: No error if cannot detect any wildcards in argument
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <rule_like> a:
          output:
              **foo(""),
              o1 = foo("{wd} "),
              o2 = "{wd}"
      """
    And SmkNotSameWildcardsSetInspection inspection is enabled
    Then I expect inspection weak warning on <**foo("")> with message
         """
         Cannot check missing wildcards here.
         """
    Then I expect inspection weak warning on <o1 = foo("{wd} ")> with message
         """
         Cannot check missing wildcards here.
         """
    When I check highlighting weak warnings
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

#  Scenario Outline: Issue 266 Weak warning
#    Given a snakemake project
#    Given I open a file "foo.smk" with text
#      """
#      <rule_like> a:
#          output:
#              **foo(""),
#              o1 = foo("{wd} "),
#              o2 = "{wd}"
#      """
#    And SmkNotSameWildcardsSetInspection inspection is enabled
#    Then I expect no inspection errors
#    When I check highlighting errors
#      Examples:
#        | rule_like    |
#        | rule       |
#        | checkpoint |

  Scenario Outline: Issue 307 - No warning if not wildcards in rule
    Given a snakemake project
    Given I open a file "foo.smk" with text
       """
       <rule_like> foo:
           input: "dd"
           output:
               "",
               temp("ddd")
       """
    And SmkNotSameWildcardsSetInspection inspection is enabled
    Then I expect no inspection weak warnings
    When I check highlighting weak warnings
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |