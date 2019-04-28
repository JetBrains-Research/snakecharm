Feature: Check Resolve
  Resolve runtime magic from snakemake

  Scenario: Resolve 'snakemake.io.expand' in *.smk file
    Given a snakemake project
    Given a file "foo.smk" with text
    """
    expand("foo")
    """
    When I put the caret at exp
    Then reference should resolve to "expand" in "io.py"