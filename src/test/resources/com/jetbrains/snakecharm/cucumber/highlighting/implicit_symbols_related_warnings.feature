Feature: This feature is tests for errors/warnings related to implicit symbols

  Scenario Outline: Do not show arg warning for 'shell'
    Given a snakemake project
    And I open a file "foo.smk" with text
    """
    def test_fun(arg):
        pass

    test_fun() #here
    <rulelike> foo:
        run:
            shell("echo {input[1]}")
    """
    And PyArgumentListInspection inspection is enabled
    # Ensure Inspection works
    Then I expect inspection warning on <)> in <) #here> with message
    """
    Parameter 'arg' unfilled
    """
    # And no inspection warnings for shell!
    When I check highlighting warnings
    Examples:
      | rulelike   |
      | rule       |
      | checkpoint |