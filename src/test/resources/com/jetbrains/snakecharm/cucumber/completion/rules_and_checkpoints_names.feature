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
    Then I invoke autocompletion popup and see a text:
     """
     <rule_like> aaaa:
       input: "path/to/input"
       output: "path/to/output"
       shell: "shell command"

     <rule_like> bbbb:
       input: <rule_like>s.aaaa
     """
    Examples:
      | rule_like  |
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
      | aaaa |
      | bbbb |
    And completion list shouldn't contain:
      | cccc |
    Examples:
      | rule_like  |
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
      | aaaa |
      | bbbb |
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |


  Scenario Outline: Complete rule/checkpoint names from different files (multiple declarations)
    Given a snakemake project
    And a file "boo1.smk" with text
     """
     <rule_like> boo11:
       input: "path/to/input"

     <rule_like> boo12:
       input: "path/to/input"
     """
    And a file "boo2.smk" with text
     """
     <rule_like> boo2:
       input: "path/to/input"

     rule rule2:
       input: "path/to/input"

     checkpoint checkpoint2:
       input: "path/to/input"

     subworkflow subworkflow2:
       snakefile: "s"
     """

    And I open a file "foo.smk" with text
     """
     <rule_like> aaaa:
       input: "path/to/input"

     <rule_like> bbbb:
       input: "path/to/input"

     <rule_like> cccc:
       input: <rule_like>s.
     """
    When I put the caret after input: <rule_like>s.
    And I invoke autocompletion popup
    Then completion list should contain:
      | aaaa          |
      | bbbb          |
      | boo11         |
      | boo12         |
      | boo2          |
      | <ok_example>  |
    And completion list shouldn't contain:
      | cccc |
      | subworkflow |
      | <neg_example> |

    Examples:
      | rule_like  | ok_example  | neg_example |
      | rule       | rule2       | checkpoint2 |
      | checkpoint | checkpoint2 | rule2       |


  Scenario Outline: No completion for long reference with rules/checkpoints last part
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <rule_like> boo:
        input: ""

      <rule_like> foo:
        input: roo.too.<rule_like>s.
      """
    When I put the caret after too.<rule_like>s.
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
    | boo |
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Display file path in type text: simple file name
    Given a snakemake project
    Given a file "boo.smk" with text
    """
    <rule_like> boo1:
      input: "file.txt"
      shell: "echo boo"
    <rule_like> boo2:
      input: "file2.txt"
    """
    Given I open a file "foo.smk" with text
    """
      <rule_like> foo0:
        input: "foo0.txt"

      <rule_like> foo:
        input: <rule_like>s.
    """
    When I put the caret after <rule_like>s.
    And I invoke autocompletion popup
    Then completion list should contain these items with type text:
      | boo1 | boo.smk |
      | boo2 | boo.smk |
      | foo0 | foo.smk |
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: display file path in type text: relative paths
    Given a snakemake project
    Given a file "dir1/dir2/boo.smk" with text
    """
    <rule_like> boo:
      input: "file.txt"
      shell: "echo boo"
    """
    Given a file "dir1/dir3/dir4/doo.smk" with text
    """
    <rule_like> doo:
      input: "file.txt"
    """
    Given a file "goo.smk" with text
    """
    <rule_like> goo:
      input: "file1.txt"
    """
    Given a file "dir1/soo.smk" with text
    """
    <rule_like> soo:
      input: "soo.txt"
    """
    Given I open a file "dir1/dir2/foo.smk" with text
    """
    <rule_like> NAME:
      input: "soo.smk"

    <rule_like> foo:
      input: <rule_like>s.
    """
    When I put the caret after <rule_like>s.
    And I invoke autocompletion popup
    Then completion list should contain these items with type text:
      | boo  | dir1/dir2/boo.smk      |
      | doo  | dir1/dir3/dir4/doo.smk |
      | goo  | goo.smk                |
      | soo  | dir1/soo.smk           |
      | NAME | dir1/dir2/foo.smk      |
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |
