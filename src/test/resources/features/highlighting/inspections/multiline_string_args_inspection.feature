Feature: Inspection for multiline arguments in same section

  Scenario Outline: Multiline string argument in execution section (warning on whole argument)
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
      | rule_like  | section | args                                 |
      | rule       | output  | "a" \n        "b"                    |
      | rule       | log     | "a" \n        'b'                    |
      | rule       | input   | "a" \n        """b"""                |
      | rule       | input   | f"foo{1}oo""boo" \n        "roo"     |
      | rule       | input   | "boo"f"foo{1}oo" \n        "roo"     |
      | rule       | input   | "boo""foo{sample}oo" \n        "roo" |
      | checkpoint | input   | f"a" \n        "b"                   |
      | checkpoint | output  | "{input[0]}" \n        "{input[1]}"  |
      | checkpoint | log     | 'a' \n        "b"                    |

  Scenario Outline: Multiline string argument in execution section (warning on subexpression)
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        <section>: <args>, "c"
    """
    And SmkSectionMultilineStringArgsInspection inspection is enabled
    Then I expect inspection weak warning on <<warning>> with message
    """
    Multiline string argument in '<section>' will be considered as concatenation. Maybe comma is missing.
    """
    When I check highlighting weak warnings
    Examples:
      | rule_like | section | args                                  | warning               |
      | rule      | input   | "foo" + "boo" + "doo" \n        "roo" | "doo" \n        "roo" |
      | rule      | input   | "a" + ("b" \n        "c" + "d")       | "b" \n        "c"     |

  Scenario Outline: Single line strings consisted of several fragments
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
      | rule_like  | section | args                        |
      | rule       | output  | "a" "b"                     |
      | rule       | log     | "a""b"                      |
      | rule       | input   | f"a""b"                     |
      | checkpoint | output  | "a"        "b"              |
      | checkpoint | log     | "{input}"        "{output}" |


  Scenario Outline: Permitted multiline string args
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        <section>: <args>
    """
    And SmkSectionMultilineStringArgsInspection inspection is enabled
    Then I expect no inspection weak warnings
    When I check highlighting weak warnings
    Examples:
      | rule_like  | section | args                                                 |
      | rule       | input   | foo("bar" \n "abc" \n "123"), "c"                    |
      | rule       | shell   | "echo foo;"\n        "ls \ "\n        "&& echo done" |
      | rule       | wrapper   | "echo foo;"\n        "ls \ "\n        "&& echo done" |
      | rule       | message | "line1"\n        "line2"\n        "line3"            |
      | checkpoint | input   | """a \n        "b" \n """  'c', "c"                  |