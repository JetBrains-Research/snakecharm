Feature: Check indent after newline symbol typing
  Scenario: New line in section in the middle of file
  Given a snakemake project
  Given I open a file "foo.smk" with text
  """
  rule foo:
      input: "foo/{sample}"
      # formatter hack
  """
  And I put the caret after "foo/{sample}"
  When I type multiline text '\noutput: "boo/{sample}"' at the caret position
  Then I expect text in current file:
  """
  rule foo:
      input: "foo/{sample}"
      output: "boo/{sample}"
      # formatter hack
  """

  @ignore
  Scenario: New line in section in the end of file
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule foo:
        input: "foo/{sample}"
    """
    And I put the caret after "foo/{sample}"
    When I type multiline text '\noutput: "boo/{sample}"' at the caret position
    Then I expect text in current file:
    """
    rule foo:
        input: "foo/{sample}"
        output: "boo/{sample}"
    """