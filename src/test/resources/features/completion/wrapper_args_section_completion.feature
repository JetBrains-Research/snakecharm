Feature: Completion for arguments used in wrapper

  Scenario Outline: Parsing arguments from wrapper.py
    Given a snakemake project
    When I check wrapper args parsing for "Python" resulting in "out" with text
    """
    <import>

    t1 = snakemake.<get>.get("foo", None)
    t2, t3 = snakemake.<array>[0], snakemake.<array>[1]
    t4, t5 = snakemake.<field>.foo, snakemake.<field>.bar
    """
    Then the file "out" should have text
    """
    <field>:bar, foo
    <array>:
    <get>:foo
    """
    Examples:
      | import                              | get       | array   | field |
      | from snakemake.shell import shell   | params    | output  | input |
      | import snakemake.shell              | params    | output  | input |
      | from snakemake.script import script | resources | message | log   |

  Scenario Outline: Consider only supported section names from wrapper.py, ignore other api, see #311
     Given a snakemake project
     When I check wrapper args parsing for "Python" resulting in "out.txt" with text
     """
     log_append = snakemake.<call>(stdout=True, stderr=True, append=True)
     log_append = snakemake.<call>.foo(stdout=True, stderr=True, append=True)
     """
     Then the file "out.txt" should have text
     """
     """
     Examples:
       | call          |
       | log_fmt_shell |
       | smth_else_foo |

  Scenario Outline: Consider only supported section names from wrapper.R ignore other api, see #311
    Given a snakemake project
    When I check wrapper args parsing for "R" resulting in "out.txt" with text
    """
    log_append = snakemake@<call>(1)
    log_append = snakemake@<call>[["jar"]]
    """
    Then the file "out.txt" should have text
    """
    """
    Examples:
      | call          |
      | log_fmt_shell |
      | smth_else_foo |

  Scenario Outline: Parsing arguments from wrapper.R
    Given a snakemake project
    When I check wrapper args parsing for "R" resulting in "out" with text
    """
    from snakemake@<import> import <import>

    t1 = snakemake@<field2>[["jar"]]
    t2, t3 = snakemake@<field1>[["foo"]], snakemake@<field1>[["bar"]]
    """
    Then the file "out" should have text
    """
    <field1>:bar, foo
    <field2>:jar
    """
    Examples:
      | import | field2    | field1 |
      | shell  | params    | input  |
      | script | resources | log    |

  Scenario Outline: Simple completion for bundled wrappers
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      <section>:
      wrapper: "0.64.0/<wrapper>"
    """
    When I put the caret after <section>:
    And I invoke autocompletion popup
    Then completion list should contain:
      | <completion> |
    Examples:
      | rule_like   | section | wrapper               | completion |
      | rule       | input   | bio/bcftools/reheader | vcf        |
      | rule       | input   | bio/bwa/mem           | reads      |
      | rule       | params  | bio/gatk/applybqsr    | java_opts  |
      | checkpoint | params  | bio/bcftools/call     | mpileup    |
      | checkpoint | params  | utils/cairosvg        | extra      |
      | checkpoint | output  | bio/last/lastal       | blasttab   |
