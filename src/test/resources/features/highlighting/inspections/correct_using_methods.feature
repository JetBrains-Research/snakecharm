Feature: Inspection for methods from snakemake library

  Scenario Outline: Unnecessary methods in rule/checkpoint section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      <section>: <method>
    """
    And SmkCorrectUsingMethodsInspection inspection is enabled
    Then I expect inspection warning on <<method>> in <<section>: <method>> with message
    """
    '<method>' doesn''t make sense in '<section>'
    """
    When I check highlighting warnings
    Examples:
      | rule_like   | section     | method          |
      | rule        | input       | protected(\"\") |
      | rule        | input       | directory(\"\") |
      | rule        | output      | ancient(\"\")   |
      | rule        | log         | ancient(\"\")   |
      | rule        | log         | directory(\"\") |
      | rule        | benchmark   | ancient(\"\")   |
      | rule        | benchmark   | directory(\"\") |