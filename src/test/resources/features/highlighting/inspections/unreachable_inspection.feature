Feature: PyUnreachableCodeInspection inspection

  # TODO: uncomment:
  #    [HACK] See https://github.com/JetBrains-Research/snakecharm/issues/14
#  Scenario: PyUnreachableCodeInspection inspection works for python code
#    Given a snakemake project
#    Given I open a file "foo.smk" with text
#     """
#     return 0
#
#     print()
#     """
#    And PyUnreachableCodeInspection inspection is enabled
#    Then I expect inspection error on <return 0> with message
#    """
#    'return' outside of function
#    """
#    And I expect inspection warning on <print()> with message
#    """
#    This code is unreachable
#    """
#    When I check highlighting warnings

  Scenario: Reachable code after return out of snakemake run section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule foo:
        run:
            return 1
    print()
    """
    And PyUnreachableCodeInspection inspection is enabled
    Then I expect no inspection warnings
    When I check highlighting warnings

  Scenario Outline: Reachable code after return out of snakemake blocks
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <block>:
        return 1
    print()
    """
    And PyUnreachableCodeInspection inspection is enabled
    Then I expect no inspection warnings
    When I check highlighting warnings
    Examples:
      | block     |
      | onstart   |
      | onerror   |
      | onsuccess |