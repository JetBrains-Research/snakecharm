Feature: Rule and Checkpoints names completion after 'rules.' and 'checkpoints.'
  (e.g. there's a rule named 'aaaa', then 'rules.aa' completes to 'rules.aaaa', similar for checkpoints)

  Scenario Outline: Complete rule/checkpoint names in input section (single declaration)
    Given a snakemake project
    Given I open a file "foo.smk" with text
     """
     <rule_like> aaaa:
       input: "path/to/input"
       output: "path/to/output"
       shell: "shell command"

     <rule_like> bbbb:
       input: <rule_like>s.aaa
     """
    When I put the caret after input: <rule_like>s.aaa
    Then I invoke autocompletion popup, select "aaaa" lookup item and see a text:
     """
     <rule_like> aaaa:
       input: "path/to/input"
       output: "path/to/output"
       shell: "shell command"

     <rule_like> bbbb:
       input: <rule_like>s.aaaa
     """
  Examples:
    | rule_like    |
    | rule       |
    | checkpoint |

  Scenario Outline: Complete rule/checkpoint names in input section (multiple declarations)
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> aaaa:
      input: "path/to/input"
      output: "path/to/output"
      shell: "shell command"

    <rule_like> bbbb:
      input: "path/to/input"
      output: "path/to/output"
      script: "script.py"

    <rule_like> cccc:
      input: <rule_like>s.
    """
    When I put the caret after input: <rule_like>s.
    And I invoke autocompletion popup
    Then completion list should contain:
      | aaaa    |
      | bbbb    |
      | cccc    |
    Examples:
      | rule_like    |
      | rule       |
      | checkpoint |

  Scenario Outline: Complete at top level for multiple rule/checkpoint declarations
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> aaaa:
      input: "path/to/input"
      output: "path/to/output"
      shell: "shell command"

    <rule_like> bbbb:
      input: "path/to/input"
      output: "path/to/output"
      script: "script.py"

    <rule_like>s.ccc
    """
    When I put the caret after <rule_like>s.
    And I invoke autocompletion popup
    Then completion list should contain:
      | aaaa    |
      | bbbb    |
    Examples:
      | rule_like    |
      | rule       |
      | checkpoint |





