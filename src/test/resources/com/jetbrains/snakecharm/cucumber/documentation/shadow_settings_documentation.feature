Feature: Documentation for 'shadow' settings

  Scenario Outline: Documentation for correct settings
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      shadow: "<setting>"
    """
    When I put the caret after shadow: "
    Then I invoke quick documentation popup
    Then Documentation text should contain <documentation>
    Examples:
    | setting | documentation |
    | full    |  The setting shadow: "full" fully shadows the entire subdirectory structure of the current workdir. |
    | shallow |  By setting shadow: "shallow", the top level files and directories are symlinked                    |
    | minimal |  The setting shadow: "minimal" only symlinks the inputs to the rule.                                |
