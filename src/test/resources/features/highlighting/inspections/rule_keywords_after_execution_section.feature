Feature: Rule sections after execution sections inspection.
  Execution sections being: run, script, wrapper, shell, cwl, notebook

  Scenario Outline: One execution section not allowed after another execution section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input: "input.txt"
        output: "output.txt"
        <sect1>: <sect1_text>
        <sect2>: <sect2_text>
    """
    And Rule Section After Execution Section inspection is enabled
    Then I expect inspection error on <<sect2>: <sect2_text>> with message
    """
    Rule section '<sect2>' isn't allowed after '<sect1>' section.
    """
    When I check highlighting errors
    Examples:
      | rule_like  | sect1    | sect1_text           | sect2     | sect2_text |
      | rule       | script   | "s.py"               | log       | "l.log"    |
      | checkpoint | script   | "s.py"               | log       | "l.log"    |
      | rule       | cwl      | "https://f.cwl" | resources | mem_mb=100 |
      | rule       | wrapper  | "dir/wrapper"        | threads   | 8          |
      | rule       | shell    | "cmd"                | params    | a="value"  |
      | rule       | notebook | "n.r"                | params    | a="value"  |

  Scenario Outline: Move execution section to the end of the rule fix test
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> name1:
      input: "input1"
      # comment
      shell: "command"
             "multiline"
             "string"
      resources: a=4
      version: 2.0

    <rule_like> name2:
      input: "input2"
    """
    And Rule Section After Execution Section inspection is enabled
    Then I expect inspection error on <resources: a=4> with message
    """
    Rule section 'resources' isn't allowed after 'shell' section.
    """
    And I expect inspection error on <version: 2.0> with message
    """
    Rule section 'version' isn't allowed after 'shell' section.
    """
    When I check highlighting errors
    And I invoke quick fix Move execution section to the end of the rule and see text:
    """
    <rule_like> name1:
      input: "input1"
      # comment
      resources: a=4
      version: 2.0
      shell: "command"
             "multiline"
             "string"

    <rule_like> name2:
      input: "input2"
    """
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

