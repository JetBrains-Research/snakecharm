Feature: Check highlighting of inspections on wildcards
  Scenario Outline: No generator section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section> NAME:
      input: "{sample}.txt"
    """
    And Wildcard not defined inspection is enabled
    Then I expect inspection error on <sample> with message
    """
    Wildcard 'sample' isn't defined
    """
    When I check highlighting errors
    Examples:
      | section    |
      | rule       |
      | checkpoint |

  Scenario Outline: Defined wildcard name
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section> NAME:
      input: "{sample}.txt"
      <generator>: "{sample}"
    """
    And Wildcard not defined inspection is enabled
    Then I expect no inspection error
    When I check highlighting errors
    Examples:
      | section    | generator |
      | rule       | output    |
      | rule       | benchmark |
      | rule       | log       |
      | checkpoint | output    |
      | checkpoint | benchmark |
      | checkpoint | log       |

  Scenario Outline: Undefined wildcard name
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section> NAME:
      input: "{sample}.txt"
      <generator>: "{sample1}{sample2}"
    """
    And Wildcard not defined inspection is enabled
    Then I expect inspection error on <sample> with message
    """
    Wildcard 'sample' isn't defined in '<generator>' section
    """
    When I check highlighting errors
    Examples:
      | section    | generator |
      | rule       | output    |
      | rule       | benchmark |
      | rule       | log       |
      | checkpoint | output    |
      | checkpoint | benchmark |
      | checkpoint | log       |

  Scenario Outline:  Undefined wildcard
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section> NAME:
      input: "{sample}.txt"
      <generator>: "{sample1}{sample2}"
    """
    And Wildcard not defined inspection is enabled
    Then I expect inspection error on <sample> with message
    """
    Wildcard 'sample' isn't defined in '<generator>' section
    """
    When I check highlighting errors
    Examples:
      | section    | generator |
      | rule       | output    |
      | rule       | benchmark |
      | rule       | log       |
      | checkpoint | output    |
      | checkpoint | benchmark |
      | checkpoint | log       |

  Scenario Outline:  Undefined wildcard in generator section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section> NAME:
      output: "{sample1}.txt"
      <generator>: "{sample2}"
    """
    And Wildcard not defined inspection is enabled
    Then I expect inspection error on <sample2> with message
    """
    Wildcard 'sample2' isn't defined in 'output' section
    """
    When I check highlighting errors
    Examples:
      | section    | generator |
      | rule       | benchmark |
      | rule       | log       |
      | checkpoint | benchmark |
      | checkpoint | log       |

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
    Missing wildcards: 'b'.
    """
    Then I expect inspection error on <log3 = "{b}.log3"> with message
    """
    Missing wildcards: 'a'.
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

  Scenario Outline: Not same wildcards set when log section is generator
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
    Missing wildcards: 'a, d'.
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

  Scenario Outline: Wildcard not defined: Two rules not interfere
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section> NAME1:
      output: "{a}.{b}.{z}"
      log: "{a}.{d}"
      
    <section> NAME2:
      output: "{x}.{y}"
      log: "{x}.{z}"
    """
    And Wildcard not defined inspection is enabled
    Then I expect inspection error on <d> with message
    """
    Wildcard 'd' isn't defined in 'output' section
    """
    Then I expect inspection error on <z> with message
    """
    Wildcard 'z' isn't defined in 'output' section
    """
    When I check highlighting errors
    Examples:
      | section    |
      | rule       |
      | checkpoint |

  Scenario Outline: Not same wildcards set: Two rules not interfere
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
    Then I expect inspection error on <"{a}.{d}"> with message
    """
    Missing wildcards: 'b'.
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
