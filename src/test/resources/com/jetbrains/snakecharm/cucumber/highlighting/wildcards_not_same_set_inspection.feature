Feature: Inspection - Not same wildcards set

  Scenario Outline: Missing wildcards when output section is generator
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <section> NAME:
        log:
          log1 = "{a}.log1",
          log2 = "{a}/{b}.log2",
          log3 = "{b}.log3"
        output: "{a}.{b}"
        benchmark: "{b}.{c}"
      """
    And Not same wildcards set inspection is enabled
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
      | section    |
      | rule       |
      | checkpoint |

  Scenario Outline: Missing wildcards when log section is generator
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <section> NAME:
        benchmark: "{b}.{c}"
        log: "{a}.{b}.{c}.{d}"
      """
    And Not same wildcards set inspection is enabled
    And I expect inspection error on <"{b}.{c}"> with message
      """
      Missing wildcards: 'a', 'd'.
      """
    When I check highlighting errors
    Examples:
      | section    |
      | rule       |
      | checkpoint |

  Scenario Outline: No wildcards missing
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section> NAME:
      output: "{a}.{b}"
      benchmark: "{a}.{b}"
      log: "{a}.{b}"
    """
    And Not same wildcards set inspection is enabled
    Then I expect no inspection error
    When I check highlighting errors
    Examples:
      | section    |
      | rule       |
      | checkpoint |

  Scenario Outline: Empty sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <section> NAME:
        output: "{a}"
        log:
          log2 = "{a}",
          log1 = ""
        benchmark: "" #here
      """
    And Not same wildcards set inspection is enabled
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
      | section    |
      | rule       |
      | checkpoint |

  Scenario Outline: Two rules not interfere
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <section> NAME1:
        output: "{a}.{b}"
        log: "{a}.{d}"

      <section> NAME2:
        output: "{x}.{y}"
        log: "{x}.{z}"
      """
    And Not same wildcards set inspection is enabled
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
      | section    |
      | rule       |
      | checkpoint |

  Scenario Outline: Do not be confused by wildcards from non-defining sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section> NAME1:
      input: "{z}"
      output: "{a}.{b}"
      log: "{a}.{z}"
    """
    And Not same wildcards set inspection is enabled
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
      | section    |
      | rule       |
      | checkpoint |

  Scenario Outline: Cannot parse section wildcards
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <section> NAME:
          <def1>: "{foo}"
          <def2>: <def2_body>
      """
    And Not same wildcards set inspection is enabled
    Then I expect no inspection error
    When I check highlighting errors
    Examples:
      | section    | def1   | def2      | def2_body   |
      | rule       | output | log       | **foo()     |
      | rule       | output | benchmark | foo()       |
      | rule       | log    | benchmark | unpack(foo) |
      | checkpoint | output | log       | **foo()     |

  Scenario Outline: Cannot parse wildcard defining section but 2 other different set
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <section> NAME:
          output: **foo()
          log: "{foo}"
          benchmark: "{sample}.txt"
      """
    And Not same wildcards set inspection is enabled
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
      | section    |
      | rule       |
      | checkpoint |

  Scenario Outline: Empty defining section but 2 other different set
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <section> NAME:
         output: ""
         log: "{foo}"
         benchmark: "{sample}.txt"
      """
    And Not same wildcards set inspection is enabled
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
      | section    |
      | rule       |
      | checkpoint |