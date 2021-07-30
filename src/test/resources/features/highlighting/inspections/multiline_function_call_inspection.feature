Feature: Inspection for multiline function calls in sections, which were declared in a single line style

  Scenario Outline: No inspection in multiline declared section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input:
            foo("abc","abcde"),
            foo2("text1",
              "text2","text3"),
            foo3("text4","text2",
                      "text3")
    <rule_like> rule_148_c:
        input: #ffff
            foo1("text1",
                 "text2", "text3")
    """
    And SmkMultilineFunctionCallInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Inspection in single line declared section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input: foo("abc","abcde"), foo2("text1",
                    "text2","text3"), foo3("text4","text2",
                            "text3")
    """
    And SmkMultilineFunctionCallInspection inspection is enabled
    Then I expect inspection error on pattern <\n                > with message
    """
    Invalid function call. Rewrite section as multiline or rewrite function using a single line
    """
    Then I expect inspection error on pattern <\n                        > with message
    """
    Invalid function call. Rewrite section as multiline or rewrite function using a single line
    """
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Quickfix for inspection in single line declared section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input:foo("abc","abcde"),foo2("text1", #comments here
                    "text2","text3"),foo3("text4","text2",
                            "text3")
    """
    And SmkMultilineFunctionCallInspection inspection is enabled
    Then I expect inspection error on pattern <\n                > with message
    """
    Invalid function call. Rewrite section as multiline or rewrite function using a single line
    """
    Then I expect inspection error on pattern <\n                        > with message
    """
    Invalid function call. Rewrite section as multiline or rewrite function using a single line
    """
    When I check highlighting errors
    Then I invoke quick fix Rewrite section as multiline and see text:
    """
    <rule_like> NAME:
        input:
            foo("abc","abcde"),
            foo2("text1", #comments here
                "text2","text3"),
            foo3("text4","text2",
                "text3")
    """
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |