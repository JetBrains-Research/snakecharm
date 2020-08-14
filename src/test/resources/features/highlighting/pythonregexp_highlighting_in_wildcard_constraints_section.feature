Feature: This feature checks reference highlighting in wildcard_constraints sections

  Scenario Outline: Highlight in 'wildcard_constraints' sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_block>
      wildcard_constraints:
          <arg>="<pyredexp>"
    """
    When I put the caret after <pyredexp>
    Then I expect language "PythonRegExp" injection on "<pyredexp>"
    Examples:
      | rule_block | arg      | pyredexp |
      | rule NAME: | dataset  | \d+      |
      | rule NAME: | seqfile  | \d+      |
      | rule NAME: | wildcard | \d+      |
      |            | dataset  | \d+      |
      |            | seqfile  | \d+      |
      |            | wildcard | \d+      |