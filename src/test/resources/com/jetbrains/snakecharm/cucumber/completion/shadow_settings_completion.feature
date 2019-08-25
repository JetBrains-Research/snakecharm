Feature: Completion in 'shadow' section

  Scenario Outline: Complete shadow settings in string literal after 'shadow' section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule a:
      shadow: <quote><quote>
    """
    When I put the caret after shadow: <quote>
    And I invoke autocompletion popup
    Then completion list should contain:
    |full   |
    |minimal|
    |shallow|
    Examples:
      | quote |
      | '     |
      | "     |

  Scenario: Check insertion after autocompletion
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule a:
      shadow: ""
    """
    When I put the caret after shadow: "
    Then I invoke autocompletion popup, select "full" lookup item and see a text:
    """
    rule a:
      shadow: "full"
    """

  Scenario: Check completion outside string literal after 'shadow' section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule a:
      shadow: ""
    """
    When I put the caret after shadow:
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      |full   |
      |minimal|
      |shallow|