Feature: Several rules were overridden in use section as one rule

  Scenario: Imported rules mentioned in list
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    use rule A, B from M as NAME
    """
    And SmkSeveralRulesAreOverriddenAsOneInspection inspection is enabled
    Then I expect inspection error on <NAME> with message
    """
    Multiple rules in 'use rule' statement but name modification ('as' statement) does not contain a wildcard '*'.
    """
    When I check highlighting errors
    And I invoke quick fix Append '_*' pattern in the end of name definition. and see text:
    """
    use rule A, B from M as NAME_*
    """

  Scenario: Rules are imported with '*' pattern
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    use rule * from M as NAME
    """
    And SmkSeveralRulesAreOverriddenAsOneInspection inspection is enabled
    Then I expect inspection weak warning on <NAME> with message
    """
    Name pattern is missed. Only the last rule will be overridden.
    """
    When I check highlighting weak warnings
    And I invoke quick fix Append '_*' pattern in the end of name definition. and see text:
    """
    use rule * from M as NAME_*
    """

  Scenario Outline: Imported rules mentioned explicitly
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    use rule <import> from M <overriding>
    """
    And SmkSeveralRulesAreOverriddenAsOneInspection inspection is enabled
    Then I expect no inspection weak warnings
    When I check highlighting weak warnings
    Examples:
      | import | overriding |
      | A      | as NAME    |
      | A      | as NAME_*  |
      | A, B   | as NAME_*  |
      | A, B   |            |

  Scenario: Rules are imported with '*' pattern, no errors because of pattern using
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    use rule * from M as NAME_*
    """
    And SmkSeveralRulesAreOverriddenAsOneInspection inspection is enabled
    Then I expect no inspection weak warnings
    When I check highlighting weak warnings