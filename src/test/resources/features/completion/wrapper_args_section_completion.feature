Feature: Completion for arguments used in wrapper

  Scenario Outline: Parsing arguments from wrapper.py
    Given a snakemake project
    When I check wrapper args parsing for "Python" resulting in "out" with text
    """
    from snakemake.<import> import <import>

    t1 = snakemake.<get>.get("foo", None)
    t2, t3 = snakemake.<array>[0], snakemake.<array>[1]
    t4, t5 = snakemake.<field>.foo, snakemake.<field>.bar
    """
    Then the file "out" should have text
    """
    <field>: bar, foo
    <array>:
    <get>: foo
    <import>:
    """
    Examples:
      | import | get       | array   | field |
      | shell  | params    | output  | input |
      | script | resources | message | log   |