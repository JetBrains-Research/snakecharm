Feature: Rule names completion for 'rules' object
  (e.g. there's a rule named 'aaaa', then 'rules.aa' completes to 'rules.aaaa')

  Scenario Outline: Complete in input section for a single other section present
    Given a snakemake project
    Given I open a file "foo.smk" with text
     """
     <section> aaaa:
       input: "path/to/input"
       output: "path/to/output"
       shell: "shell command"

     <section> bbbb:
       input: <section>s.aaa
     """
    When I put the caret after input: <section>s.aaa
    Then I invoke autocompletion popup, select "aaaa" lookup item and see a text:
     """
     <section> aaaa:
       input: "path/to/input"
       output: "path/to/output"
       shell: "shell command"

     <section> bbbb:
       input: <section>s.aaaa
     """
  Examples:
    | section    |
    | rule       |
    | checkpoint |

  Scenario Outline: Complete in input section for multiple section definitions
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section> aaaa:
      input: "path/to/input"
      output: "path/to/output"
      shell: "shell command"

    <section> bbbb:
      input: "path/to/input"
      output: "path/to/output"
      script: "script.py"

    <section> cccc:
      input: <section>s.
    """
    When I put the caret after input: <section>s.
    And I invoke autocompletion popup
    Then completion list should contain:
      | aaaa    |
      | bbbb    |
      | cccc    |
    Examples:
      | section    |
      | rule       |
      | checkpoint |

  Scenario Outline: Complete at top level for multiple sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section> aaaa:
      input: "path/to/input"
      output: "path/to/output"
      shell: "shell command"

    <section> bbbb:
      input: "path/to/input"
      output: "path/to/output"
      script: "script.py"

    <section>s.ccc
    """
    When I put the caret after <section>s.
    And I invoke autocompletion popup
    Then completion list should contain:
      | aaaa    |
      | bbbb    |
    Examples:
      | section    |
      | rule       |
      | checkpoint |





