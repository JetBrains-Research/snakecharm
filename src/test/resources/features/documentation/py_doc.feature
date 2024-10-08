Feature: Check documentation popup for python file
  An example how to write tests on documentation

  Scenario: Documentation for some python method
    Given a snakemake project
    Given I open a file "foo.py" with text
    """
    def foo():
      \"\"\"my documentation\"\"\"
      pass

    foo() #here
    """
    When I put the caret at foo() #here
    Then I invoke quick documentation popup
    Then Documentation text should contain a substring: def
    Then Documentation text should contain a substring: foo
    #    And Documentation text should contain
    #    """
    #    my documentation
    #    """

  Scenario: Navigation info for some python method
    Given a snakemake project
    Given I open a file "foo.py" with text
      """
      def foo():
        \"\"\"my documentation\"\"\"
        pass

      foo() #here
      """
    When I put the caret at foo() #here
    Then I invoke quick navigation info
    Then Documentation text should contain a substring: foo
    #    And Documentation text should contain
    #    """
    #    my documentation
    #    """