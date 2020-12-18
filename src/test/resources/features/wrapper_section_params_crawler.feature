Feature: Wrappers params parsing for wrapper.py, wrapper.R

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
    <field>:['bar', 'foo']
    <array>:[]
    <get>:['foo']
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
      t4 = snakemake@<field3>
      """
    Then the file "out" should have text
      """
      <field1>:['bar', 'foo']
      <field2>:['jar']
      <field3>:[]
      """
    Examples:
      | import | field1 | field2 | field3    |
      | shell  | input  | output | params    |
      | script | log    | params | resources |