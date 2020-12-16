Feature: Fixes for PyTypeCheckerInspection related false positives
  Issue #317

  Scenario: PyTypeCheckerInspection works in snakemake files
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule rule_317_2:
        input: expand(
            [s for s in 1],
            key="val"
        )
    """
    And PyTypeCheckerInspection inspection is enabled
    Then I expect inspection warning on <1> in <[s for s in 1]> with message
    """
    Expected type 'collections.Iterable', got 'int' instead
    """
    When I check highlighting warnings

  Scenario Outline: Args Section type is iterable
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule rule_317_1:
        output: "d"

    <rule_like> rule_317_2:
        input: expand(
            [s for s in rules.rule_317_1.output],
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
    Then I expect no inspection warnings
    When I check highlighting warnings
    Examples:
      | rule_like   |
      | rule        |
      | checkpoint  |