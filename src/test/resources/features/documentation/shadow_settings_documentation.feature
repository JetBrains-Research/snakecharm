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
    | setting | documentation                                                                                       |
    | full    |  The setting shadow: "full" fully shadows the entire subdirectory structure of the current workdir. |
    | shallow |  By setting shadow: "shallow", the top level files and directories are symlinked                    |
    | minimal |  The setting shadow: "minimal" only symlinks the inputs to the rule.                                |

  Scenario Outline: Documentation for setting in different quotes
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      shadow: <quote>minimal<quote>
    """
    When I put the caret after shadow: <quote>
    Then I invoke quick documentation popup
    Then Documentation text should be equal to The setting shadow: "minimal" only symlinks the inputs to the rule.
    Examples:
    | quote |
    | "     |
    | '     |
    | """   |

  Scenario Outline: Documentation for fstring setting
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      shadow: f<quote>minimal<quote>
    """
    When I put the caret after shadow: f<quote>
    Then I invoke quick documentation popup
    Then Documentation text should be equal to The setting shadow: "minimal" only symlinks the inputs to the rule.
    Examples:
      | quote |
      | "     |
      | '     |
      | """   |

  Scenario: Shadow documentation doesn't break python documentation
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    def f(number):
      "Documentation"
      return number
    f(1)
    """
    When I put the caret at f(1)
    Then I invoke quick navigation info
    Then Documentation text should contain Documentation