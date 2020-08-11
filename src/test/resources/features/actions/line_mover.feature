Feature: Line mover

  Scenario: TODO Usage Example
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    rule foo:
        input: "i"
        output:
            "o"
    """
    When I put the caret at input
    And I invoke MoveStatementDown action
    Then editor content will be
    """
    rule foo:
        output:
            "o"
        input: "i"
    """