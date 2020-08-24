Feature: Code block provider test
  # See moveCaretToCodeBlockStart/moveCaretToCodeBlockEnd

  Scenario: Go to start of block
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    rule shell_name:
      input: repeat("", 5)
      wildcard_constraints: wildcard="/d+1"
    """
    When I put the caret after input: repeat("",
    When I invoke EditorCodeBlockStart action
    When I expect caret at input: repeat("", 5)


  Scenario: Go to end of block
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    rule shell_name:
      input: repeat("", 5)
      wildcard_constraints: wildcard="/d+1"
    """
    When I put the caret after wildcard_constraints:
    When I invoke EditorCodeBlockEnd action
    When I expect caret after wildcard="/d+1"