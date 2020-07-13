Feature: Inspection for multiline arguments in same section
  Scenario Outline: Multiline string argument in execution section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        <section>: <args>, "c"
    """
    And SmkSectionMultilineStringArgsInspection inspection is enabled
    Then I expect inspection weak warning on <<args>> with message
    """
    Multiline string argument in '<section>' will be considered as concatenation. Maybe comma is missing.
    """
    When I check highlighting weak warnings
    Examples:
      | section | rule_like  | args                                  |
      | output  | rule       | "a" \n        "b"                     |
      | log     | rule       | "a" \n        'b'                     |
      | input   | rule       | "a" \n        """b"""                 |
      | input   | rule       | "foo" + "boo" + "doo" \n        "roo" |
      | input   | rule       | "a" + ("b" \n        "c" + "d")       |
      | input   | rule       | f"foo{1}oo""boo" \n        "roo"      |
      | input   | rule       | "boo"f"foo{1}oo" \n        "roo"      |
      | input   | rule       | "boo""foo{sample}oo" \n        "roo"  |
      | input   | checkpoint | f"a" \n        "b"                    |
      | output  | checkpoint | "{input[0]}" \n        "{input[1]}"   |
      | log     | checkpoint | 'a' \n        "b"                     |

  Scenario Outline: Multiple strings (one line) argument in execution section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        <section>: <args>, "c"
    """
    And SmkSectionMultilineStringArgsInspection inspection is enabled
    Then I expect no inspection weak warnings
    When I check highlighting weak warnings
    Examples:
      | section | rule_like  | args                           |
      | output  | rule       | "a" "b"                        |
      | log     | rule       | "a""b"                         |
      | input   | rule       | f"a""b"                        |
      | input   | checkpoint | """a \n        "b" \n """  'c' |
      | output  | checkpoint | "a"        "b"                 |
      | log     | checkpoint | "{input}"        "{output}"    |
