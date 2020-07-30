Feature: This feature checks reference highlighting in wildcard_constraints sections

  Scenario Outline: Highlight in 'wildcard_constraints' sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_name> NAME:
      wildcard_constraints:
          <arg>="<pyredexp>"
    """
    When I put the caret after <pyredexp>
    Then I expect language injection on "<pyredexp>"
    Examples:
      | rule_name | arg     | pyredexp |
      | rule      | dataset | \d+      |
      | rule      | seqfile | \d+      |