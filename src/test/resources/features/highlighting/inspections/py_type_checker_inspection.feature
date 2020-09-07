Feature: Fixes for PyTypeCheckerInspection related false positives
  Scenario Outline: Args Section type is iterable
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule rule_317_1:
        output: "d"

    <rule_like> rule_317_2:
        input: expand(
            [s for s in rules.rule_317_1.output],
            [s for s in 1],
            key="val"
        )
        output: "out.txt"
        run:
            with open(output[0], 'w') as out:
                for p in input:
                    print(p, file=out)
            for i,s in enumerate(output):
                        print(i, s)
    """
    And PyTypeCheckerInspection inspection is enabled
    # warning only for `1`, not for `rules.rule_317_1.output` and not for `for p in input`
    Then I expect inspection warning on <1> in <[s for s in 1]> with message
    """
    Expected 'collections.Iterable', got 'int' instead
    """
    When I check highlighting warnings
    Examples:
      | rule_like   |
      | rule        |
      | checkpoint  |