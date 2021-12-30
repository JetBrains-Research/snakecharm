Feature: Inspection: SmkWildcardNotDefinedInspection
  
  Scenario Outline: No wildcards defining section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section> NAME:
      <usage>: "{sample}.txt"
    """
    And SmkWildcardNotDefinedInspection inspection is enabled
    Then I expect inspection error on <sample> with message
    """
    Wildcard 'sample' isn't properly defined.
    """
    When I check highlighting errors
    Examples:
      | section    | usage      |
      | rule       | input      |
      | rule       | group      |
      | checkpoint | input      |

  Scenario Outline: Cannot parse wildcard defining section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section> NAME:
       <def_section>: <def_section_body>
       <usage>: "{sample}.txt"
    """
    And SmkWildcardNotDefinedInspection inspection is enabled
    Then I expect inspection weak warning on <sample> with message
    """
    Cannot check whether wildcard 'sample' is defined or not.
    """
    When I check highlighting weak warnings
    Examples:
      | section    | usage | def_section | def_section_body |
      | rule       | input | output      | foo()            |
      | rule       | input | log         | foo()            |
      | rule       | input | benchmark   | *foo()           |
      | rule       | input | benchmark   | **foo()          |
      | rule       | input | benchmark   | unpack(foo)      |
      | rule       | group | output      | foo()            |
      | checkpoint | input | output      | foo()            |

  Scenario Outline: Cannot parse wildcard defining section but other exists
    Given a snakemake project
    Given I open a file "foo.smk" with text
     """
     <section> NAME:
       <def_section1>: foo()
       <def_section2>: "{sample}"
       <usage>: "{sample} {typo}.txt"
     """
    And SmkWildcardNotDefinedInspection inspection is enabled
    Then I expect inspection error on <typo> with message
    """
    Wildcard 'typo' isn't defined in '<def_section1>' section.
    """
    When I check highlighting errors
    Examples:
      | section    | def_section1 | def_section2 | usage |
      | rule       | output       | log          | input |
      | rule       | output       | benchmark    | input |
      | rule       | log          | benchmark    | input |
      | checkpoint | output       | log          | input |

  Scenario Outline: Defined wildcard name
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section> NAME:
      <usage>: "{sample}.txt"
      <def>: "{sample}"
    """
    And SmkWildcardNotDefinedInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | section    | def       | usage |
      | rule       | output    | input |
      | rule       | benchmark | input |
      | rule       | log       | input |
      | checkpoint | output    | input |
      | checkpoint | benchmark | input |
      | checkpoint | log       | input |
      | rule       | output    | group |

  Scenario Outline: Defined wildcard in overridden rule or checkpoint
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section> NAME:
      <def>: "{sample}"

    use rule NAME as NAME2 with:
      input: "{sample}"

    use rule NAME2 as NAME3 with:
      input: "{sample}"
    """
    And SmkWildcardNotDefinedInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | section    | def    |
      | rule       | output |
      | checkpoint | output |

  Scenario Outline: Defined wildcard in overridden rules or checkpoints from another module
    Given a snakemake project
    And a file "boo.smk" with text
    """
    <section> NAME:
      <def>: "{sample}"

    <section> NAME1:
      <def>: "{sample}"
    """
    Given I open a file "foo.smk" with text
    """
    module M:
      snakefile: "boo.smk"

    use rule * from M as other_*

    use rule other_NAME as NAME3 with:
      input: "{sample}"

    use rule NAME,NAME1 from M as new_* with:
      input: "{sample}"

    use rule NAME3 as NAME4 with:
      input: "{sample}"
    """
    And SmkWildcardNotDefinedInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | section    | def    |
      | rule       | output |
      | checkpoint | output |

  Scenario Outline: Undefined wildcard in one of the overridden rules
    Given a snakemake project
    And a file "boo.smk" with text
    """
    <section> NAME:
      output: "{sample}"

    <section> NAME1:
      output: "{file}"
    """
    Given I open a file "foo.smk" with text
    """
    module M:
      snakefile: "boo.smk"

    use rule NAME,NAME1 from M as new_* with:

    use rule new_NAME1 as name_one with:
      input: "{sample}"
    """
    And SmkWildcardNotDefinedInspection inspection is enabled
    Then I expect inspection error on <sample> with message
    """
    Wildcard 'sample' isn't defined in any appropriate section of overridden rules
    """
    When I check highlighting errors
    Examples:
      | section    |
      | rule       |
      | rule       |
      | checkpoint |
      | checkpoint |

  Scenario Outline: Undefined wildcard if it is was defined in sections, which is overridden
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section> A:
      <definition>: "{sample}_{sample2}"

    use rule A as B with:
      <definition>: "{log_wildcard}"
      input: "{sample2}"
    """
    And SmkWildcardNotDefinedInspection inspection is enabled
    Then I expect inspection error on <sample2> in <input: "{sample2}"> with message
    """
    Wildcard 'sample2' isn't defined in any appropriate section of overridden rules
    """
    When I check highlighting errors
    Examples:
      | section    | definition |
      | rule       | log        |
      | rule       | output     |
      | checkpoint | log        |
      | checkpoint | output     |

  Scenario Outline: Undefined wildcard if not all parent rules contains wildcard which is checked
    Given a snakemake project
    And a file "boo.smk" with text
    """
    <rule_like> NAME1:
        output: "{sample1}.out"

    <rule_like> NAME2:
        output: "{sample2}.out"
    """
    Given I open a file "foo.smk" with text
      """
      module M:
        snakefile: "boo.smk"

      use rule NAME1,NAME2 from M as other_* with:
        input: "{sample1}.out2"
      """
    And SmkWildcardNotDefinedInspection inspection is enabled
    Then I expect inspection error on <sample1> with message
    """
    Wildcard 'sample1' isn't defined in any appropriate section of overridden rules
    """
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Cannot parse wildcard defining section in overridden rule or checkpoint
    Given a snakemake project
    And a file "boo.smk" with text
    """
    <section> NAME:
      output: <text>
    """
    Given I open a file "foo.smk" with text
    """
    module M:
      snakefile: "boo.smk"

    use rule NAME from M as NAME2 with:
      input: "{sample}"
    """
    And SmkWildcardNotDefinedInspection inspection is enabled
    Then I expect inspection weak warning on <sample> with message
    """
    Cannot check whether wildcard 'sample' is defined or not.
    """
    When I check highlighting weak warnings
    Examples:
      | section    | text       |
      | rule       | foo()      |
      | rule       | "file.txt" |
      | checkpoint | foo()      |
      | checkpoint | "file.txt" |

  Scenario Outline: Undefined wildcard in wildcards using section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section> NAME:
      <usage>: "{sample}.txt"
      <def>: "{sample1}{sample2}"
    """
    And SmkWildcardNotDefinedInspection inspection is enabled
    Then I expect inspection error on <sample> with message
    """
    Wildcard 'sample' isn't defined in '<def>' section.
    """
    When I check highlighting errors
    Examples:
      | section    | def       | usage     |
      | rule       | output    | input     |
      | rule       | benchmark | input     |
      | rule       | log       | input     |
      | checkpoint | output    | input     |
      | checkpoint | benchmark | input     |
      | checkpoint | log       | input     |
      | rule       | output    | conda     |
      | rule       | output    | group     |
      | rule       | output    | resources |
      | rule       | output    | params    |
      | rule       | output    | conda     |

  Scenario Outline:  Undefined wildcard in wildcards defining section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section> NAME:
      <def>: "{sample1}.txt"
      <other_def>: "{sample2}"
    """
    And SmkWildcardNotDefinedInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | section    | def    | other_def |
      | rule       | output | benchmark |
      | rule       | output | log       |
      | checkpoint | output | benchmark |
      | checkpoint | output | log       |

  Scenario Outline: Two rules not interfere
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <section> NAME1:
        output: "{a}.{b}"
        input: "{a}.{z}"

      <section> NAME2:
        output: "{x}.{y}"
        input: "{x}.{a}"
      """
    And SmkWildcardNotDefinedInspection inspection is enabled
    Then I expect inspection error on <a> in <input: "{x}.{a}"> with message
      """
      Wildcard 'a' isn't defined in 'output' section.
      """
    Then I expect inspection error on <z> in <input: "{a}.{z}"> with message
      """
      Wildcard 'z' isn't defined in 'output' section.
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
        group: "{a}.{z}"
      """
    And SmkWildcardNotDefinedInspection inspection is enabled
    Then I expect inspection error on <z> in <"{z}"> with message
      """
      Wildcard 'z' isn't defined in 'output' section.
      """
    Then I expect inspection error on <z> in <"{a}.{z}"> with message
      """
      Wildcard 'z' isn't defined in 'output' section.
      """
    When I check highlighting errors
    Examples:
      | section    |
      | rule       |
      | checkpoint |

  Scenario Outline: Undefined in main defining section but is in other defining sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
       """
       <section> NAME:
           <def_section1>: "{a}"
           <def_section2>: "{a}{b}"
           <usage>: "{b}.txt"
       """
    And SmkWildcardNotDefinedInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | section    | usage | def_section1 | def_section2 |
      | rule       | input | output       | log          |
      | rule       | input | output       | benchmark    |
      | rule       | input | log          | benchmark    |
      | rule       | group | output       | log          |
      | checkpoint | input | output       | log          |

  Scenario Outline: Expand injections isn't undefined wildcard
    Given a snakemake project
    Given I open a file "foo.smk" with text
     """
     <rule_like> NAME:
         output: "{x}"
         input:
             expand("{prefix}", prefix="p")
     """
    And SmkWildcardNotDefinedInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Wildcard references which looks like qualified cannot be resolved as wildcards
    Given a snakemake project
    Given I open a file "foo.smk" with text
       """
       <rule_like> NAME:
           output: "{sample}"
           input: "{wildcards.sample}"
       """
    And SmkWildcardNotDefinedInspection inspection is enabled
    Then I expect inspection error on <wildcards> with message
      """
      Wildcard 'wildcards' isn't defined in 'output' section.
      """
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |