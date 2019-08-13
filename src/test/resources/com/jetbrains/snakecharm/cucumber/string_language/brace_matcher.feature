Feature: SmkSL brace matcher test

  Scenario: Forward matching
    Given a snakemake project
    Given I open a file "foo.smkStringLanguage" with text
    """
    {foo}
    """
    When I put the caret at {
    Then I expect forward brace matching after "foo}"

  Scenario: Backward matching
    Given a snakemake project
    Given I open a file "foo.smkStringLanguage" with text
    """
    {foo}
    """
    When I put the caret after }
    Then I expect backward brace matching before "{foo"

  Scenario: Matching with regexp
    Given a snakemake project
    Given I open a file "foo.smkStringLanguage" with text
    """
    {foo,a{3,5}}
    """
    When I put the caret at {foo
    Then I expect forward brace matching after "5}}"

  Scenario: Matching with multiple braces
    Given a snakemake project
    Given I open a file "foo.smkStringLanguage" with text
    """
    {foo} text {boo}
    """
    When I put the caret at {foo
    Then I expect forward brace matching after "foo}"
    When I put the caret at {boo
    Then I expect forward brace matching after "boo}"