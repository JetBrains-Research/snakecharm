Feature: Annotate syntax errors

  Scenario: Annotate keyword argument duplication
      Given a snakemake project
      Given I open a file "foo.smk" with text
      """
      rule NAME:
          params: a="value", a1="value1", a="_value"

      checkpoint NAME2:
          params: b="value", b1="value1", b="_value"
      """
      Then I expect inspection error on <a="_value"> with message
      """
      Keyword argument already provided: a=\"value\".
      """
      And I expect inspection error on <b="_value"> with message
      """
      Keyword argument already provided: b=\"value\".
      """
      When I check highlighting errors

  Scenario: Annotate positional argument after keyword argument
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
        input: file1="file1.txt", "file2.txt", file3="file3.txt"
    """
    Then I expect inspection error on <"file2.txt"> with message
    """
    Positional argument after keyword argument.
    """
    When I check highlighting errors

  Scenario: Annotate multiple run/shell/script/wrapper/cwl sections.
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
        input: "file1.txt"
        output: touch("output.txt")
        script: "script.py"
        shell: "command"
        wrapper: "dir/wrapper"
    """
    Then I expect inspection error on <shell: "command"> with message
    """
    Multiple run/shell sections.
    """
    And I expect inspection error on <wrapper: "dir/wrapper"> with message
    """
    Multiple run/shell sections.
    """
    When I check highlighting errors




