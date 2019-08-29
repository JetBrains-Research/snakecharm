Feature: completion in wrappers

  Scenario Outline: complete tool without tag
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      wrapper: "sam"
    """
    And I prepare wrapper storage
    When I put the caret after sam
    And I invoke autocompletion popup
    Then completion list should contain:
      | 0.36.0/bio/samtools/sort |
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |


  Scenario Outline: complete tool with tag
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      wrapper: "0.33.0/"
    """
    And I prepare wrapper storage
    When I put the caret after 0.33.0/
    And I invoke autocompletion popup
    Then completion list should contain:
      | bio/samtools/sort |
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |
