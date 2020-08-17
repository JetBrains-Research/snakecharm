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
    When I invoke EditorCodeBlockStart action and see text with markers:
    """
    rule shell_name:
      <start>input: repeat("",<caret> 5)<end>
      wildcard_constraints: wildcard="/d+1"
    """

  Scenario: Go to end of block
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    rule shell_name:
      input: repeat("", 5)
      wildcard_constraints: wildcard="/d+1"
    """
    When I put the caret after wildcard_constraints:
    When I invoke EditorCodeBlockEnd action and see text with markers:
    """
    rule shell_name:
      input: repeat("", 5)
      <start>wildcard_constraints:<caret> wildcard="/d+1"<end>
    """