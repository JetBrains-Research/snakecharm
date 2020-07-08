Feature: Annotate if return out of function or run/onstart/onerror/onsuccess blocks

  Scenario: Return out of function
      Given a snakemake project
      Given I open a file "foo.smk" with text
      """
      for i in range(0):
          return 0

      return 1
      """
      Then I expect inspection error on <return 0> with message
      """
      'return' outside of function
      """
      Then I expect inspection error on <return 1> with message
      """
      'return' outside of function
      """
      When I check highlighting errors

  Scenario: Return out of snakemake run section
        Given a snakemake project
        Given I open a file "foo.smk" with text
        """
        rule foo:
            run:
                return 1
        """
        Then I expect no inspection errors
        When I check highlighting errors

  Scenario Outline: Return out of snakemake blocks
        Given a snakemake project
        Given I open a file "foo.smk" with text
        """
        <block>:
            return 1
        """
        Then I expect no inspection errors
        When I check highlighting errors
    Examples:
      | block     |
      | onstart   |
      | onerror   |
      | onsuccess |