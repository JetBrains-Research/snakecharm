Feature: Rule names completion for 'rules' object
  (e.g. there's a rule named 'aaaa', then 'rules.aa' completes to 'rules.aaaa')

  Scenario: Complete in input section for a single other rule present
    Given a snakemake project
    Given I open a file "foo.smk" with text
     """
     rule aaaa:
       input: "path/to/input"
       output: "path/to/output"
       shell: "shell command"

     rule bbbb:
       input: rules.aaa
     """
    When I put the caret after input: rules.aaa
    Then I invoke autocompletion popup, select "aaaa" lookup item and see a text:
     """
     rule aaaa:
       input: "path/to/input"
       output: "path/to/output"
       shell: "shell command"

     rule bbbb:
       input: rules.aaaa
     """

  Scenario: Complete in input section for multiple rules
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule aaaa:
      input: "path/to/input"
      output: "path/to/output"
      shell: "shell command"

    rule bbbb:
      input: "path/to/input"
      output: "path/to/output"
      script: "script.py"

    rule cccc:
      input: rules.
    """
    When I put the caret after input: rules.
    And I invoke autocompletion popup
    Then completion list should contain:
      | aaaa    |
      | bbbb    |
      | cccc    |

  Scenario: Complete at top level for multiple rules
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule aaaa:
      input: "path/to/input"
      output: "path/to/output"
      shell: "shell command"

    rule bbbb:
      input: "path/to/input"
      output: "path/to/output"
      script: "script.py"

    rules.ccc
    """
    When I put the caret after rules.
    And I invoke autocompletion popup
    Then completion list should contain:
      | aaaa    |
      | bbbb    |





